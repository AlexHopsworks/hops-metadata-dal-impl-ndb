/*
 * Hops Database abstraction layer for storing the hops metadata in MySQL Cluster
 * Copyright (C) 2015  hops.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.hops.metadata.ndb;

import com.mysql.clusterj.ClusterJException;
import com.mysql.clusterj.ClusterJHelper;
import com.mysql.clusterj.Constants;
import com.mysql.clusterj.LockMode;
import io.hops.exception.StorageException;
import io.hops.metadata.ndb.wrapper.ClusterJCaching;
import io.hops.metadata.ndb.wrapper.HopsExceptionHelper;
import io.hops.metadata.ndb.wrapper.HopsSession;
import io.hops.metadata.ndb.wrapper.HopsSessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DBSessionProvider implements Runnable {

  static final Log LOG = LogFactory.getLog(DBSessionProvider.class);
  static HopsSessionFactory sessionFactory;
  private ConcurrentLinkedQueue<DBSession> sessionPool =
      new ConcurrentLinkedQueue<>();
  private ConcurrentLinkedQueue<DBSession> toGC =
      new ConcurrentLinkedQueue<>();
  private final int MAX_REUSE_COUNT;
  private Properties conf;
  private final Random rand;
  private AtomicInteger sessionsCreated = new AtomicInteger(0);
  private long rollingAvg[];
  private AtomicInteger rollingAvgIndex = new AtomicInteger(-1);
  private boolean automaticRefresh = false;
  private Thread thread;
  private final ClusterJCaching clusterJCaching;

  public DBSessionProvider(Properties conf)
      throws StorageException {
    this.conf = conf;

    boolean useClusterjDtoCache = Boolean.parseBoolean(
            (String) conf.get("io.hops.enable.clusterj.dto.cache"));
    boolean useClusterjSessionCache = Boolean.parseBoolean(
            (String) conf.get("io.hops.enable.clusterj.session.cache"));
    clusterJCaching = new ClusterJCaching(useClusterjDtoCache, useClusterjSessionCache);

    int initialPoolSize = Integer.parseInt(
            (String) conf.get("io.hops.session.pool.size"));
    int reuseCount = Integer.parseInt(
            (String) conf.get("io.hops.session.reuse.count"));

    if (reuseCount <= 0) {
      System.err.println("Invalid value for session reuse count");
      System.exit(-1);
    }

    this.MAX_REUSE_COUNT = reuseCount;
    rand = new Random(System.currentTimeMillis());
    rollingAvg = new long[initialPoolSize];
    start(initialPoolSize);
  }

  private void start(int initialPoolSize) throws StorageException {
    LOG.info("Database connect string: " +
        conf.get(Constants.PROPERTY_CLUSTER_CONNECTSTRING));
    LOG.info(
        "Database name: " + conf.get(Constants.PROPERTY_CLUSTER_DATABASE));
    LOG.info("Max Transactions: " +
        conf.get(Constants.PROPERTY_CLUSTER_MAX_TRANSACTIONS));
    LOG.info("Using ClusterJ Session Cache: "+clusterJCaching.useClusterjSessionCache());
    LOG.info("Using ClusterJ DTO Cache: "+clusterJCaching.useClusterjDtoCache());
    try {
      sessionFactory =
          new HopsSessionFactory(ClusterJHelper.getSessionFactory(conf), clusterJCaching);
    } catch (ClusterJException ex) {
      throw HopsExceptionHelper.wrap(ex);
    }

    for (int i = 0; i < initialPoolSize; i++) {
      sessionPool.add(initSession());
    }

    thread = new Thread(this, "Session Pool Refresh Daemon");
    thread.setDaemon(true);
    automaticRefresh = true;
    thread.start();
  }

  private DBSession initSession() throws StorageException {
    Long startTime = System.currentTimeMillis();
    HopsSession session = sessionFactory.getSession();
    Long sessionCreationTime = (System.currentTimeMillis() - startTime);
    rollingAvg[rollingAvgIndex.incrementAndGet() % rollingAvg.length] =
        sessionCreationTime;

    int reuseCount = rand.nextInt(MAX_REUSE_COUNT) + 1;
    DBSession dbSession = new DBSession(session, reuseCount);
    sessionsCreated.incrementAndGet();
    return dbSession;
  }

  private void closeSession(DBSession dbSession) throws StorageException {
    Long startTime = System.currentTimeMillis();
    dbSession.getSession().close();
    Long sessionCreationTime = (System.currentTimeMillis() - startTime);
    rollingAvg[rollingAvgIndex.incrementAndGet() % rollingAvg.length] =
        sessionCreationTime;
  }

  public void stop() throws StorageException {
    automaticRefresh = false;
    while (!sessionPool.isEmpty()) {
      DBSession dbsession = sessionPool.remove();
      closeSession(dbsession);
    }
  }

  public DBSession getSession() throws StorageException {
    try {
      DBSession session = sessionPool.remove();
      return session;
    } catch (NoSuchElementException e) {
      LOG.warn(
          "DB Sessino provider cant keep up with the demand for new sessions");
      return initSession();
    }
  }

  public void returnSession(DBSession returnedSession, boolean forceClose) throws StorageException {
    //session has been used, increment the use counter
    returnedSession
        .setSessionUseCount(returnedSession.getSessionUseCount() + 1);

    if ((returnedSession.getSessionUseCount() >=
        returnedSession.getMaxReuseCount()) ||
        forceClose) { // session can be closed even before the reuse count has expired. Close the session incase of database errors.
      toGC.add(returnedSession);
    } else { // increment the count and return it to the pool
      returnedSession.getSession().setLockMode(LockMode.READ_COMMITTED);
      sessionPool.add(returnedSession);
    }
  }

  public double getSessionCreationRollingAvg() {
    double avg = 0;
    for (long aRollingAvg : rollingAvg) {
      avg += aRollingAvg;
    }
    avg = avg / rollingAvg.length;
    return avg;
  }

  public int getTotalSessionsCreated() {
    return sessionsCreated.get();
  }

  public int getAvailableSessions() {
    return sessionPool.size();
  }

  @Override
  public void run() {
    while (automaticRefresh) {
      try {
        int toGCSize = toGC.size();

        if (toGCSize > 0) {
          LOG.debug("Renewing a session(s) " + toGCSize);
          for (int i = 0; i < toGCSize; i++) {
            DBSession session = toGC.remove();
            session.getSession().close();
          }

          for (int i = 0; i < toGCSize; i++) {
            sessionPool.add(initSession());
          }
        }
        Thread.sleep(5);
      } catch (NoSuchElementException e) {
        for (int i = 0; i < 100; i++) {
          try {
            sessionPool.add(initSession());
          } catch (StorageException e1) {
            LOG.error(e1);
          }
        }
      } catch (InterruptedException ex) {
        LOG.warn(ex);
        Thread.currentThread().interrupt();
      } catch (StorageException e) {
        LOG.error(e);
      }
    }
  }

  public void clearCache() throws StorageException {
    Iterator<DBSession> itr = sessionPool.iterator();
    while(itr.hasNext()){
      DBSession session = itr.next();
      session.getSession().dropInstanceCache();
    }
  }
}

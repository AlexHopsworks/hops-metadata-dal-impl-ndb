/*
 * Hops Database abstraction layer for storing the hops metadata in MySQL Cluster
 * Copyright (C) 2015  hops.io
 *
 * This program is free software; you can redistribute it and/or
 * append it under the terms of the GNU General Public License
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
package io.hops.metadata.ndb.dalimpl.hdfs;

import com.mysql.clusterj.annotation.Column;
import com.mysql.clusterj.annotation.PersistenceCapable;
import com.mysql.clusterj.annotation.PrimaryKey;
import io.hops.exception.StorageException;
import io.hops.metadata.hdfs.TablesDef;
import static io.hops.metadata.hdfs.TablesDef.INodeTableDef.PARENT_ID;
import static io.hops.metadata.hdfs.TablesDef.ProvenanceLogTableDef.APP_ID;
import static io.hops.metadata.hdfs.TablesDef.ProvenanceLogTableDef.DATASET_NAME;
import static io.hops.metadata.hdfs.TablesDef.ProvenanceLogTableDef.LOGICAL_TIME;
import static io.hops.metadata.hdfs.TablesDef.ProvenanceLogTableDef.PARENT_ID;
import static io.hops.metadata.hdfs.TablesDef.ProvenanceLogTableDef.TIMESTAMP;
import io.hops.metadata.hdfs.dal.ProvenanceLogDataAccess;
import io.hops.metadata.hdfs.entity.ProvenanceLogEntry;
import io.hops.metadata.ndb.ClusterjConnector;
import io.hops.metadata.ndb.wrapper.HopsSession;
import java.util.ArrayList;
import java.util.Collection;

public class ProvenanceLogClusterj implements TablesDef.ProvenanceLogTableDef,
  ProvenanceLogDataAccess<ProvenanceLogEntry> {

  private ClusterjConnector connector = ClusterjConnector.getInstance();

  @PersistenceCapable(table = TABLE_NAME)
  public interface ProvenanceLogEntryDto {

    @PrimaryKey
    @Column(name = INODE_ID)
    long getInodeId();

    void setInodeId(long inodeId);

    @PrimaryKey
    @Column(name = USER_ID)
    int getUserId();

    void setUserId(int userId);
    
    @PrimaryKey
    @Column(name = APP_ID)
    String getAppId();

    void setAppId(String appId);

    @PrimaryKey
    @Column(name = LOGICAL_TIME)
    int getLogicalTime();

    void setLogicalTime(int logicalTime);
    
    @Column(name = LOGICAL_TIME_BATCH)
    int getLogicalTimeBatch();

    void setLogicalTimeBatch(int logicalTimeBatch);
    
    @Column(name = TIMESTAMP)
    long getTimestamp();

    void setTimestamp(long timestamp);
    
    @Column(name = TIMESTAMP_BATCH)
    long getTimestampBatch();

    void setTimestampBatch(long timestampBatch);
    
    @PrimaryKey
    @Column(name = PARENT_ID)
    long getParentId();

    void setParentId(long parentId);
    
    @PrimaryKey
    @Column(name = PARTITION_ID)
    long getPartitionId();

    void setPartitionId(long partitionId);

    @Column(name = PROJECT_NAME)
    String getProjectName();

    void setProjectName(String projectName);

    @Column(name = DATASET_NAME)
    String getDatasetName();

    void setDatasetName(String datasetName);
    
    @Column(name = INODE_NAME)
    String getInodeName();

    void setInodeName(String inodeName);
    
    @Column(name = USER_NAME)
    String getUserName();

    void setUserName(String userName);
    
    @Column(name = OPERATION)
    short getOperation();

    void setOperation(short operation);
  }

  @Override
  public void addAll(Collection<ProvenanceLogEntry> logEntries)
    throws StorageException {
    HopsSession session = connector.obtainSession();
    ArrayList<ProvenanceLogEntryDto> added = new ArrayList<>(logEntries.size());
    try {
      for (ProvenanceLogEntry logEntry : logEntries) {
        added.add(createPersistable(logEntry));
      }
      session.savePersistentAll(added);
    } finally {
      session.release(added);
    }
  }

  @Override
  public void add(ProvenanceLogEntry logEntry) throws StorageException {
    HopsSession session = connector.obtainSession();
    ProvenanceLogEntryDto dto = null;
    try {
      dto = createPersistable(logEntry);
      session.savePersistent(dto);
    } finally {
      session.release(dto);
    }
  }

  @Override
  public Collection<ProvenanceLogEntry> readExisting(Collection<ProvenanceLogEntry> logEntries)
    throws StorageException {
    HopsSession session = connector.obtainSession();
    final ArrayList<ProvenanceLogEntryDto> dtos = new ArrayList<>();
    try {
      for (ProvenanceLogEntry logEntry : logEntries) {
        Object[] pk = new Object[]{logEntry.getInodeId(), logEntry.getUserId(), 
          logEntry.getAppId(), logEntry.getLogicalTime()};
        ProvenanceLogEntryDto dto = session.newInstance(ProvenanceLogEntryDto.class, pk);
        dto = session.load(dto);
        dtos.add(dto);
      }
      session.flush();
      Collection<ProvenanceLogEntry> plel = createCollection(dtos);
      return plel;
    } finally {
      session.release(dtos);
    }
  }

  private ProvenanceLogEntryDto createPersistable(ProvenanceLogEntry logEntry) throws StorageException {
    HopsSession session = connector.obtainSession();
    ProvenanceLogEntryDto dto = session.newInstance(ProvenanceLogEntryDto.class);
    dto.setInodeId(logEntry.getInodeId());
    dto.setUserId(logEntry.getUserId());
    dto.setAppId(logEntry.getAppId());
    dto.setLogicalTime(logEntry.getLogicalTime());
    dto.setLogicalTimeBatch(logEntry.getLogicalTimeBatch());
    dto.setTimestamp(logEntry.getTimestamp());
    dto.setTimestampBatch(logEntry.getTimestampBatch());
    dto.setParentId(logEntry.getParentId());
    dto.setPartitionId(logEntry.getPartitionId());
    dto.setProjectName(logEntry.getProjectName());
    dto.setDatasetName(logEntry.getDatasetName());
    dto.setInodeName(logEntry.getInodeName());
    dto.setUserName(logEntry.getUserName());
    dto.setOperation(logEntry.getOperationOrdinal());
    return dto;
  }

  private Collection<ProvenanceLogEntry> createCollection(Collection<ProvenanceLogEntryDto> collection) {
    ArrayList<ProvenanceLogEntry> list = new ArrayList<>(collection.size());
    for (ProvenanceLogEntryDto dto : collection) {
      list.add(createProvenanceLogEntry(dto));
    }
    return list;
  }

  private ProvenanceLogEntry createProvenanceLogEntry(ProvenanceLogEntryDto dto) {
    return new ProvenanceLogEntry(
      dto.getInodeId(),
      dto.getUserId(),
      dto.getAppId(),
      dto.getLogicalTime(),
      dto.getLogicalTimeBatch(),
      dto.getTimestamp(),
      dto.getLogicalTimeBatch(),
      dto.getParentId(),
      dto.getPartitionId(),
      dto.getProjectName(),
      dto.getDatasetName(),
      dto.getInodeName(),
      dto.getUserName(),
      ProvenanceLogEntry.Operation.values()[dto.getOperation()]);
  }
}

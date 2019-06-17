/*
 * Copyright (C) 2015 hops.io.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.metadata.ndb.dalimpl.yarn;

import com.mysql.clusterj.annotation.Column;
import com.mysql.clusterj.annotation.PersistenceCapable;
import com.mysql.clusterj.annotation.PrimaryKey;
import io.hops.exception.StorageException;
import io.hops.metadata.ndb.ClusterjConnector;
import io.hops.metadata.ndb.wrapper.HopsSession;
import io.hops.metadata.yarn.TablesDef;
import static io.hops.metadata.yarn.TablesDef.AppProvenanceTableDef.SUBMIT_TIME;
import static io.hops.metadata.yarn.TablesDef.AppProvenanceTableDef.TIMESTAMP;
import io.hops.metadata.yarn.dal.AppProvenanceDataAccess;
import io.hops.metadata.yarn.entity.AppProvenanceEntry;
import java.util.ArrayList;
import java.util.Collection;

public class AppProvenanceClusterJ implements TablesDef.AppProvenanceTableDef,
  AppProvenanceDataAccess<AppProvenanceEntry> {

  private ClusterjConnector connector = ClusterjConnector.getInstance();

  @PersistenceCapable(table = TABLE_NAME)
  public interface AppProvenanceEntryDto {

    @PrimaryKey
    @Column(name = ID)
    String getId();

    void setId(String id);

    @Column(name = NAME)
    String getName();

    void setName(String name);
    
    @PrimaryKey
    @Column(name = STATE)
    String getState();

    void setState(String state);

    @Column(name = USER)
    String getUser();

    void setUser(String user);
    
    @PrimaryKey
    @Column(name = TIMESTAMP)
    long getTimestamp();

    void setTimestamp(long timestamp);
    
    @PrimaryKey
    @Column(name = SUBMIT_TIME)
    long getSubmitTime();

    void setSubmitTime(long submitTime);
    
    @PrimaryKey
    @Column(name = START_TIME)
    long getStartTime();

    void setStartTime(long startTime);
    
    @PrimaryKey
    @Column(name = FINISH_TIME)
    long getFinishTime();

    void setFinishTime(long finishTime);
  }

  @Override
  public void addAll(Collection<AppProvenanceEntry> entries)
    throws StorageException {
    HopsSession session = connector.obtainSession();
    ArrayList<AppProvenanceEntryDto> added = new ArrayList<>(entries.size());
    try {
      for (AppProvenanceEntry entry : entries) {
        added.add(createPersistable(entry));
      }
      session.savePersistentAll(added);
    } finally {
      session.release(added);
    }
  }

  @Override
  public void add(AppProvenanceEntry entry) throws StorageException {
    HopsSession session = connector.obtainSession();
    AppProvenanceEntryDto dto = null;
    try {
      dto = createPersistable(entry);
      session.savePersistent(dto);
    } finally {
      session.release(dto);
    }
  }

  private AppProvenanceEntryDto createPersistable(AppProvenanceEntry entry) throws StorageException {
    HopsSession session = connector.obtainSession();
    AppProvenanceEntryDto dto = session.newInstance(AppProvenanceEntryDto.class);
    dto.setId(entry.getId());
    dto.setName(entry.getName());
    dto.setState(entry.getState());
    dto.setUser(entry.getUser());
    dto.setTimestamp(entry.getTimestamp());
    dto.setSubmitTime(entry.getSubmitTime());
    dto.setStartTime(entry.getStartTime());
    dto.setFinishTime(entry.getFinishTime());
    return dto;
  }

  private Collection<AppProvenanceEntry> createCollection(Collection<AppProvenanceEntryDto> collection) {
    ArrayList<AppProvenanceEntry> list = new ArrayList<>(collection.size());
    for (AppProvenanceEntryDto dto : collection) {
      list.add(createProvenanceLogEntry(dto));
    }
    return list;
  }

  private AppProvenanceEntry createProvenanceLogEntry(AppProvenanceEntryDto dto) {
    return new AppProvenanceEntry(
      dto.getId(),
      dto.getName(),
      dto.getState(),
      dto.getUser(),
      dto.getTimestamp(),
      dto.getSubmitTime(), 
      dto.getStartTime(),
      dto.getFinishTime());
  }
}
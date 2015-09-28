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
package io.hops.metadata.ndb.dalimpl.hdfs;

import com.google.common.collect.Lists;
import com.mysql.clusterj.annotation.Column;
import com.mysql.clusterj.annotation.PersistenceCapable;
import com.mysql.clusterj.annotation.PrimaryKey;
import io.hops.exception.StorageException;
import io.hops.metadata.hdfs.TablesDef;
import io.hops.metadata.hdfs.dal.UserGroupDataAccess;
import io.hops.metadata.hdfs.entity.Group;
import io.hops.metadata.hdfs.entity.User;
import io.hops.metadata.ndb.ClusterjConnector;
import io.hops.metadata.ndb.wrapper.HopsQuery;
import io.hops.metadata.ndb.wrapper.HopsQueryBuilder;
import io.hops.metadata.ndb.wrapper.HopsQueryDomainType;
import io.hops.metadata.ndb.wrapper.HopsSession;

import java.util.List;

public class UserGroupCluterj implements TablesDef.UsersGroupsTableDef,
    UserGroupDataAccess<User, Group>{

  @PersistenceCapable(table = TABLE_NAME)
  public interface UserGroupDTO {

    @PrimaryKey
    @Column(name = USER_ID)
    byte[] getUserId();

    void setUserId(byte[] id);

    @PrimaryKey
    @Column(name = GROUP_ID)
    byte[] getGroupId();

    void setGroupId(byte[] id);
  }

  private ClusterjConnector connector = ClusterjConnector.getInstance();

  @Override
  public void addUserToGroup(User user, Group group) throws StorageException {
    addUserToGroup(user.getId(), group.getId());
  }

  @Override
  public void addUserToGroup(byte[] userId, byte[] groupId)
      throws StorageException {
    HopsSession session = connector.obtainSession();
    UserGroupDTO dto = session.newInstance(UserGroupDTO.class);
    dto.setUserId(userId);
    dto.setGroupId(groupId);
    session.savePersistent(dto);
    session.release(dto);
  }

  @Override
  public List<Group> getGroupsForUser(User user) throws StorageException {
    return getGroupsForUser(user.getId());
  }

  @Override
  public List<Group> getGroupsForUser(byte[] userId)
      throws StorageException {
    HopsSession session = connector.obtainSession();

    HopsQueryBuilder qb = session.getQueryBuilder();
    HopsQueryDomainType<UserGroupDTO> dobj =  qb.createQueryDefinition
        (UserGroupDTO.class);
    dobj.where(dobj.get("userId").equal(dobj.param("param")));
    HopsQuery<UserGroupDTO> query = session.createQuery(dobj);
    query.setParameter("param", userId);
    List<UserGroupDTO> res =  query.getResultList();

    List<GroupClusterj.GroupDTO> groupDTOs = Lists.newArrayList();
    for(UserGroupDTO ug : res){
      GroupClusterj.GroupDTO groupDTO = session.newInstance(GroupClusterj
          .GroupDTO.class, ug.getGroupId());
      session.load(groupDTO);
      groupDTOs.add(groupDTO);
    }
    session.flush();

   return GroupClusterj.convertAndRelease(session, groupDTOs);
  }

}

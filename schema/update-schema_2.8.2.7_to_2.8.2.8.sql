ALTER TABLE `hdfs_le_descriptors` ADD COLUMN `location_domain_id` tinyint(4) default 0;

ALTER TABLE `yarn_le_descriptors` ADD COLUMN `location_domain_id` tinyint(4) default 0;

ALTER TABLE `hdfs_hash_buckets` DROP COLUMN `hash`;

ALTER TABLE `hdfs_hash_buckets` ADD COLUMN `hash` binary(20) NOT NULL DEFAULT '0';

ALTER TABLE hdfs_replicas ADD KEY `storage_and_bucket_idx` (`storage_id`, `bucket_id`);

ALTER TABLE `hdfs_block_infos` ADD COLUMN `truncate_block_num_bytes` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_block_infos` ADD COLUMN `truncate_block_generation_stamp` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_inode_attributes` RENAME TO `hdfs_directory_with_quota_feature`;

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_quota_disk` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_quota_ssd` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_quota_raid5` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_quota_archive` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_quota_db` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_used_disk` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_used_ssd` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_used_raid5` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_used_archive` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_directory_with_quota_feature` ADD COLUMN `typespace_used_db` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_quota_update` ADD COLUMN `typespace_delta_disk` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_quota_update` ADD COLUMN `typespace_delta_ssd` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_quota_update` ADD COLUMN `typespace_delta_raid5` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_quota_update` ADD COLUMN `typespace_delta_archive` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `hdfs_quota_update` ADD COLUMN `typespace_delta_db` bigint(20) NOT NULL DEFAULT '-1';

ALTER TABLE `yarn_applicationstate` MODIFY COLUMN `appstate` longblob NULL;

ALTER TABLE `yarn_applicationattemptstate` DROP FOREIGN KEY `applicationid`;

ALTER TABLE `yarn_applicationattemptstate` MODIFY COLUMN `applicationattemptstate` longblob NULL;

ALTER TABLE `hdfs_quota_update` CHANGE `diskspace_delta` `storage_space_delta` bigint(20);

ALTER TABLE `hdfs_directory_with_quota_feature` CHANGE `dsquota` `ssquota` bigint(20);

ALTER TABLE `hdfs_directory_with_quota_feature` CHANGE `diskspace` `storage_space` bigint(20);

CREATE TABLE `hdfs_xattrs` (
  `inode_id` bigint(20) NOT NULL,
  `namespace` tinyint(4) NOT NULL,
  `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
  `value` varchar(13500) COLLATE latin1_general_cs DEFAULT '',
  PRIMARY KEY (`inode_id`,`namespace`,`name`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs COMMENT='NDB_TABLE=READ_BACKUP=1'
/*!50100 PARTITION BY KEY (inode_id) */;

ALTER TABLE `hdfs_inodes` ADD COLUMN `num_xattrs` tinyint(4) NOT NULL DEFAULT '0';

ALTER TABLE `hdfs_metadata_log` CHANGE `inode_partition_id` `pk1` bigint(20);
ALTER TABLE `hdfs_metadata_log` CHANGE `inode_parent_id` `pk2` bigint(20);
ALTER TABLE `hdfs_metadata_log` CHANGE `inode_name` `pk3` varchar(255) COLLATE latin1_general_cs NOT NULL DEFAULT '';

ALTER TABLE hdfs_active_block_reports ADD (`nn_address` varchar(128) COLLATE latin1_general_cs NOT NULL);

ALTER TABLE `hdfs_on_going_sub_tree_ops` ADD COLUMN `start_time` bigint(20) NOT NULL;

ALTER TABLE `hdfs_on_going_sub_tree_ops` ADD COLUMN `async_lock_recovery_time` bigint(20) NOT NULL DEFAULT '0';

ALTER TABLE `hdfs_on_going_sub_tree_ops` ADD COLUMN `user` varchar(256) NOT NULL DEFAULT '';

ALTER TABLE `hdfs_on_going_sub_tree_ops` ADD COLUMN `inode_id` bigint(20) NOT NULL DEFAULT '0';

CREATE TABLE `hdfs_file_provenance_log` (
  `inode_id` bigint(20) NOT NULL,
  `inode_operation` varchar(45) NOT NULL,
  `io_logical_time` int(11) NOT NULL,
  `io_timestamp` bigint(20) NOT NULL,
  `io_app_id` varchar(45) NOT NULL,
  `io_user_id` int(11) NOT NULL,
  `i_partition_id` bigint(20) NOT NULL,
  `project_i_id` bigint(20) NOT NULL,
  `dataset_i_id` bigint(20) NOT NULL,
  `parent_i_id` bigint(20) NOT NULL,
  `i_name` varchar(255) NOT NULL,
  `project_name` varchar(255) NOT NULL,
  `dataset_name` varchar(255) NOT NULL,
  `i_p1_name` varchar(255) NOT NULL,
  `i_p2_name` varchar(255) NOT NULL,
  `i_parent_name` varchar(255) NOT NULL,
  `io_user_name` varchar(100) NOT NULL,
  `i_xattr_name` varchar(255) NOT NULL,
  `io_logical_time_batch` int(11) NOT NULL,
  `io_timestamp_batch` bigint(20) NOT NULL,
  `ds_logical_time` int(11) NOT NULL,
  PRIMARY KEY (`inode_id`, `inode_operation`, `io_logical_time`, `io_timestamp`, `io_app_id`, `io_user_id`),
  KEY `io_logical_time` (`io_logical_time` ASC)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `yarn_app_provenance_log` (
  `id` varchar(45) NOT NULL,
  `state` varchar(45) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `user` varchar(100) NOT NULL,
  `submit_time` bigint(20) NOT NULL,
  `start_time` bigint(20) NOT NULL,
  `finish_time` bigint(20) NOT NULL,
  PRIMARY KEY (`id`,`state`,`timestamp`)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

CREATE TABLE `hdfs_file_provenance_xattrs_buffer` (
  `inode_id` bigint(20) NOT NULL,
  `namespace` tinyint(4) NOT NULL,
  `name` varchar(255) COLLATE latin1_general_cs NOT NULL,
  `inode_logical_time` int(11) NOT NULL,
  `value` varchar(13500) COLLATE latin1_general_cs DEFAULT '',
  PRIMARY KEY (`inode_id`,`namespace`,`name`,`inode_logical_time`),
  INDEX `xattr_versions` (`inode_id`, `namespace`, `name` ASC)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs;

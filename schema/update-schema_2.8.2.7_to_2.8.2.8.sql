CREATE TABLE `hdfs_provenance_log` (
  `inode_id` bigint(20) NOT NULL,
  `user_id` int(11) NOT NULL,
  `app_id` varchar(45) NOT NULL,
  `logical_time` int(11) NOT NULL,
  `partition_id` int(11) NOT NULL,
  `parent_id` int(11) NOT NULL,
  `project_name` varchar(255) NOT NULL,
  `dataset_name` varchar(255) NOT NULL,
  `inode_name` varchar(255) NOT NULL,
  `user_name` varchar(100) NOT NULL,
  `logical_time_batch` int(11) NOT NULL,
  `timestamp` bigint(20) NOT NULL,
  `timestamp_batch` bigint(20) NOT NULL,
  `operation` smallint(11) NOT NULL,
  PRIMARY KEY (`inode_id`, `user_id`, `app_id`, `logical_time`),
  KEY `logical_time` (`logical_time` ASC)
) ENGINE=ndbcluster DEFAULT CHARSET=latin1 COLLATE=latin1_general_cs$$
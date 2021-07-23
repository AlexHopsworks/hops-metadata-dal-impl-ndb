ALTER TABLE `hdfs_file_provenance_log`
ADD COLUMN `i_p1_id` bigint(20) NOT NULL,
ADD COLUMN `i_p2_id` bigint(20) NOT NULL,
ADD COLUMN `i_p3_name` varchar(255) NOT NULL,
ADD COLUMN `i_p3_id` bigint(20) NOT NULL,
ADD COLUMN `i_p4_name` varchar(255) NOT NULL,
ADD COLUMN `i_p4_id` bigint(20) NOT NULL,
COMMENT = 'NDB_TABLE=READ_BACKUP=1';
-- MySQL dump 10.13  Distrib 5.7.29, for Linux (x86_64)
--
-- Host: 10.0.0.102    Database: sagan_opt
-- ------------------------------------------------------
-- Server version	5.7.25

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `member_profile`
--

DROP TABLE IF EXISTS `member_profile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `member_profile` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `avatar_url` varchar(255) DEFAULT NULL,
  `bio` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `github_id` bigint(20) DEFAULT NULL,
  `github_username` varchar(255) DEFAULT NULL,
  `gravatar_email` varchar(255) DEFAULT NULL,
  `hidden` tinyint(1) DEFAULT NULL,
  `lanyrd_username` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `speakerdeck_username` varchar(255) DEFAULT NULL,
  `twitter_username` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  `video_embeds` varchar(255) DEFAULT NULL,
  `job_title` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `member_profile_username` (`username`),
  KEY `member_profile_github_id` (`github_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post`
--

DROP TABLE IF EXISTS `post`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `post` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `broadcast` tinyint(1) NOT NULL,
  `category` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `draft` tinyint(1) NOT NULL,
  `format` varchar(255) DEFAULT NULL,
  `public_slug` varchar(255) DEFAULT NULL,
  `publish_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `raw_content` varchar(255) NOT NULL,
  `rendered_content` varchar(255) NOT NULL,
  `rendered_summary` varchar(255) NOT NULL,
  `title` varchar(255) NOT NULL,
  `author_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `public_slug` (`public_slug`),
  KEY `idx_category` (`category`),
  KEY `idx_draft` (`draft`),
  KEY `idx_publish_at` (`publish_at`),
  KEY `post_author_id` (`author_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `post_public_slug_aliases`
--

DROP TABLE IF EXISTS `post_public_slug_aliases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `post_public_slug_aliases` (
  `post_id` int(11) NOT NULL,
  `public_slug_aliases` varchar(255) NOT NULL,
  PRIMARY KEY (`post_id`,`public_slug_aliases`),
  UNIQUE KEY `public_slug_aliases` (`public_slug_aliases`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `repo_url` varchar(255) DEFAULT NULL,
  `category` varchar(255) DEFAULT NULL,
  `site_url` varchar(255) DEFAULT NULL,
  `stack_overflow_tags` varchar(255) DEFAULT NULL,
  `raw_boot_config` varchar(255) DEFAULT NULL,
  `rendered_boot_config` varchar(255) DEFAULT NULL,
  `raw_overview` varchar(255) DEFAULT '',
  `rendered_overview` varchar(255) DEFAULT '',
  `parent_project_id` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL DEFAULT '255',
  PRIMARY KEY (`id`),
  KEY `project_display_order` (`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_release_list`
--

DROP TABLE IF EXISTS `project_release_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_release_list` (
  `project_id` varchar(255) NOT NULL,
  `repository_id` varchar(255) DEFAULT NULL,
  `api_doc_url` varchar(255) DEFAULT NULL,
  `artifact_id` varchar(255) DEFAULT NULL,
  `group_id` varchar(255) DEFAULT NULL,
  `is_current` tinyint(1) DEFAULT NULL,
  `ref_doc_url` varchar(255) DEFAULT NULL,
  `release_status` int(11) DEFAULT NULL,
  `version_name` varchar(255) NOT NULL,
  PRIMARY KEY (`project_id`,`version_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_repository`
--

DROP TABLE IF EXISTS `project_repository`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_repository` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `snapshots_enabled` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_sample_list`
--

DROP TABLE IF EXISTS `project_sample_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_sample_list` (
  `title` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `display_order` int(11) NOT NULL,
  `project_id` varchar(255) NOT NULL,
  PRIMARY KEY (`project_id`,`display_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_version`
--

DROP TABLE IF EXISTS `schema_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schema_version` (
  `version_rank` int(11) NOT NULL,
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) NOT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`version`),
  KEY `schema_version_vr_idx` (`version_rank`),
  KEY `schema_version_ir_idx` (`installed_rank`),
  KEY `schema_version_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spring_tools_platform`
--

DROP TABLE IF EXISTS `spring_tools_platform`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spring_tools_platform` (
  `id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `spring_tools_platform_downloads`
--

DROP TABLE IF EXISTS `spring_tools_platform_downloads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `spring_tools_platform_downloads` (
  `spring_tools_platform_id` varchar(255) NOT NULL,
  `download_url` varchar(255) NOT NULL,
  `variant` varchar(255) NOT NULL,
  `label` varchar(255) NOT NULL,
  PRIMARY KEY (`spring_tools_platform_id`,`variant`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-04-01 15:19:24

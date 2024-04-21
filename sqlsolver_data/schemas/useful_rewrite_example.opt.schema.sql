-- MySQL dump 10.13  Distrib 5.7.29, for osx10.15 (x86_64)
--
-- Host: localhost    Database: lobsters_test
-- ------------------------------------------------------
-- Server version	5.7.29

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
-- Table structure for table `ar_internal_metadata`
--

DROP TABLE IF EXISTS `ar_internal_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ar_internal_metadata` (
  `key` varchar(255) CHARACTER SET utf8 NOT NULL,
  `value` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime DEFAULT NULL,
  `short_id` varchar(10) NOT NULL DEFAULT '',
  `story_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `parent_comment_id` bigint(20) unsigned DEFAULT NULL,
  `thread_id` bigint(20) unsigned DEFAULT NULL,
  `comment` mediumtext NOT NULL,
  `upvotes` int(11) NOT NULL DEFAULT '0',
  `downvotes` int(11) NOT NULL DEFAULT '0',
  `confidence` decimal(20,19) NOT NULL DEFAULT '0.0000000000000000000',
  `markeddown_comment` mediumtext,
  `is_deleted` tinyint(1) DEFAULT '0',
  `is_moderated` tinyint(1) DEFAULT '0',
  `is_from_email` tinyint(1) DEFAULT '0',
  `hat_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `short_id` (`short_id`),
  KEY `confidence_idx` (`confidence`),
  KEY `comments_hat_id_fk` (`hat_id`),
  KEY `comments_parent_comment_id_fk` (`parent_comment_id`),
  KEY `story_id_short_id` (`story_id`,`short_id`),
  KEY `thread_id` (`thread_id`),
  KEY `downvote_index` (`user_id`,`story_id`,`downvotes`,`created_at`),
  KEY `index_comments_on_user_id` (`user_id`),
  FULLTEXT KEY `index_comments_on_comment` (`comment`),
  CONSTRAINT `comments_hat_id_fk` FOREIGN KEY (`hat_id`) REFERENCES `hats` (`id`),
  CONSTRAINT `comments_parent_comment_id_fk` FOREIGN KEY (`parent_comment_id`) REFERENCES `comments` (`id`),
  CONSTRAINT `comments_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `comments_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `domains`
--

DROP TABLE IF EXISTS `domains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `domains` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `domain` varchar(255) DEFAULT NULL,
  `is_tracker` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `banned_at` datetime DEFAULT NULL,
  `banned_by_user_id` int(11) DEFAULT NULL,
  `banned_reason` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hat_requests`
--

DROP TABLE IF EXISTS `hat_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hat_requests` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `hat` varchar(255) NOT NULL,
  `link` varchar(255) NOT NULL,
  `comment` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `hat_requests_user_id_fk` (`user_id`),
  CONSTRAINT `hat_requests_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hats`
--

DROP TABLE IF EXISTS `hats`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hats` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `granted_by_user_id` bigint(20) unsigned NOT NULL,
  `hat` varchar(255) NOT NULL,
  `link` varchar(255) CHARACTER SET utf8mb4 DEFAULT NULL,
  `modlog_use` tinyint(1) DEFAULT '0',
  `doffed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `hats_granted_by_user_id_fk` (`granted_by_user_id`),
  KEY `hats_user_id_fk` (`user_id`),
  CONSTRAINT `hats_granted_by_user_id_fk` FOREIGN KEY (`granted_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `hats_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `hidden_stories`
--

DROP TABLE IF EXISTS `hidden_stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `hidden_stories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `story_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_hidden_stories_on_user_id_and_story_id` (`user_id`,`story_id`),
  KEY `hidden_stories_story_id_fk` (`story_id`),
  CONSTRAINT `hidden_stories_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `hidden_stories_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invitation_requests`
--

DROP TABLE IF EXISTS `invitation_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `invitation_requests` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(255) DEFAULT NULL,
  `is_verified` tinyint(1) DEFAULT '0',
  `email` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `memo` text,
  `ip_address` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invitations`
--

DROP TABLE IF EXISTS `invitations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `invitations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `code` varchar(255) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `memo` mediumtext,
  `used_at` datetime DEFAULT NULL,
  `new_user_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `invitations_new_user_id_fk` (`new_user_id`),
  KEY `invitations_user_id_fk` (`user_id`),
  CONSTRAINT `invitations_new_user_id_fk` FOREIGN KEY (`new_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `invitations_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `keystores`
--

DROP TABLE IF EXISTS `keystores`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `keystores` (
  `key` varchar(50) NOT NULL DEFAULT '',
  `value` bigint(20) DEFAULT NULL,
  UNIQUE KEY `key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `messages` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `author_user_id` bigint(20) unsigned NOT NULL,
  `recipient_user_id` bigint(20) unsigned NOT NULL,
  `has_been_read` tinyint(1) DEFAULT '0',
  `subject` varchar(100) DEFAULT NULL,
  `body` mediumtext,
  `short_id` varchar(30) DEFAULT NULL,
  `deleted_by_author` tinyint(1) DEFAULT '0',
  `deleted_by_recipient` tinyint(1) DEFAULT '0',
  `hat_id` bigint(20) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `random_hash` (`short_id`),
  KEY `author_user_id` (`author_user_id`),
  KEY `index_messages_on_hat_id` (`hat_id`),
  KEY `messages_recipient_user_id_fk` (`recipient_user_id`),
  CONSTRAINT `messages_hat_id_fk` FOREIGN KEY (`hat_id`) REFERENCES `hats` (`id`),
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `messages_recipient_user_id_fk` FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mod_notes`
--

DROP TABLE IF EXISTS `mod_notes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mod_notes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `moderator_user_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `note` text NOT NULL,
  `markeddown_note` text NOT NULL,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_mod_notes_on_id_and_user_id` (`id`,`user_id`),
  KEY `mod_notes_moderator_user_id_fk` (`moderator_user_id`),
  KEY `mod_notes_user_id_fk` (`user_id`),
  CONSTRAINT `mod_notes_moderator_user_id_fk` FOREIGN KEY (`moderator_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `mod_notes_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `moderations`
--

DROP TABLE IF EXISTS `moderations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `moderations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `moderator_user_id` bigint(20) unsigned DEFAULT NULL,
  `story_id` bigint(20) unsigned DEFAULT NULL,
  `comment_id` bigint(20) unsigned DEFAULT NULL,
  `user_id` bigint(20) unsigned DEFAULT NULL,
  `action` mediumtext,
  `reason` mediumtext,
  `is_from_suggestions` tinyint(1) DEFAULT '0',
  `tag_id` bigint(20) unsigned DEFAULT NULL,
  `domain_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `moderations_comment_id_fk` (`comment_id`),
  KEY `index_moderations_on_created_at` (`created_at`),
  KEY `index_moderations_on_domain_id` (`domain_id`),
  KEY `moderations_moderator_user_id_fk` (`moderator_user_id`),
  KEY `moderations_story_id_fk` (`story_id`),
  KEY `moderations_tag_id_fk` (`tag_id`),
  KEY `index_moderations_on_user_id` (`user_id`),
  CONSTRAINT `moderations_comment_id_fk` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`),
  CONSTRAINT `moderations_moderator_user_id_fk` FOREIGN KEY (`moderator_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `moderations_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `moderations_tag_id_fk` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `read_ribbons`
--

DROP TABLE IF EXISTS `read_ribbons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `read_ribbons` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `is_following` tinyint(1) DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `story_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_read_ribbons_on_story_id` (`story_id`),
  KEY `index_read_ribbons_on_user_id` (`user_id`),
  CONSTRAINT `read_ribbons_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `read_ribbons_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `saved_stories`
--

DROP TABLE IF EXISTS `saved_stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `saved_stories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `story_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_saved_stories_on_user_id_and_story_id` (`user_id`,`story_id`),
  KEY `saved_stories_story_id_fk` (`story_id`),
  CONSTRAINT `saved_stories_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `saved_stories_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_migrations`
--

DROP TABLE IF EXISTS `schema_migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schema_migrations` (
  `version` varchar(255) CHARACTER SET utf8 NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `stories`
--

DROP TABLE IF EXISTS `stories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `url` varchar(250) DEFAULT '',
  `title` varchar(150) NOT NULL DEFAULT '',
  `description` mediumtext,
  `short_id` varchar(6) NOT NULL DEFAULT '',
  `is_expired` tinyint(1) NOT NULL DEFAULT '0',
  `upvotes` int(10) unsigned NOT NULL DEFAULT '0',
  `downvotes` int(10) unsigned NOT NULL DEFAULT '0',
  `is_moderated` tinyint(1) NOT NULL DEFAULT '0',
  `hotness` decimal(20,10) NOT NULL DEFAULT '0.0000000000',
  `markeddown_description` mediumtext,
  `story_cache` mediumtext,
  `comments_count` int(11) NOT NULL DEFAULT '0',
  `merged_story_id` bigint(20) unsigned DEFAULT NULL,
  `unavailable_at` datetime DEFAULT NULL,
  `twitter_id` varchar(20) DEFAULT NULL,
  `user_is_author` tinyint(1) DEFAULT '0',
  `user_is_following` tinyint(1) NOT NULL DEFAULT '0',
  `domain_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_short_id` (`short_id`),
  KEY `index_stories_on_created_at` (`created_at`),
  KEY `index_stories_on_domain_id` (`domain_id`),
  KEY `hotness_idx` (`hotness`),
  KEY `is_idxes` (`is_expired`,`is_moderated`),
  KEY `index_stories_on_is_expired` (`is_expired`),
  KEY `index_stories_on_is_moderated` (`is_moderated`),
  KEY `index_stories_on_merged_story_id` (`merged_story_id`),
  KEY `index_stories_on_twitter_id` (`twitter_id`),
  KEY `url` (`url`(191)),
  KEY `index_stories_on_user_id` (`user_id`),
  FULLTEXT KEY `index_stories_on_description` (`description`),
  FULLTEXT KEY `index_stories_on_story_cache` (`story_cache`),
  FULLTEXT KEY `stories_story_cache` (`story_cache`),
  FULLTEXT KEY `index_stories_on_title` (`title`),
  CONSTRAINT `fk_rails_a04bca56b0` FOREIGN KEY (`domain_id`) REFERENCES `domains` (`id`),
  CONSTRAINT `stories_merged_story_id_fk` FOREIGN KEY (`merged_story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `stories_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=145 DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `suggested_taggings`
--

DROP TABLE IF EXISTS `suggested_taggings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `suggested_taggings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `story_id` bigint(20) unsigned NOT NULL,
  `tag_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `suggested_taggings_story_id_fk` (`story_id`),
  KEY `suggested_taggings_tag_id_fk` (`tag_id`),
  KEY `suggested_taggings_user_id_fk` (`user_id`),
  CONSTRAINT `suggested_taggings_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `suggested_taggings_tag_id_fk` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`),
  CONSTRAINT `suggested_taggings_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `suggested_titles`
--

DROP TABLE IF EXISTS `suggested_titles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `suggested_titles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `story_id` bigint(20) unsigned NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `title` varchar(150) CHARACTER SET utf8mb4 NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `suggested_titles_story_id_fk` (`story_id`),
  KEY `suggested_titles_user_id_fk` (`user_id`),
  CONSTRAINT `suggested_titles_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `suggested_titles_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag_filters`
--

DROP TABLE IF EXISTS `tag_filters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tag_filters` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `user_id` bigint(20) unsigned NOT NULL,
  `tag_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tag_filters_tag_id_fk` (`tag_id`),
  KEY `user_tag_idx` (`user_id`,`tag_id`),
  CONSTRAINT `tag_filters_tag_id_fk` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`),
  CONSTRAINT `tag_filters_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taggings`
--

DROP TABLE IF EXISTS `taggings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `taggings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `story_id` bigint(20) unsigned NOT NULL,
  `tag_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `story_id_tag_id` (`story_id`,`tag_id`),
  KEY `taggings_tag_id_fk` (`tag_id`),
  CONSTRAINT `taggings_story_id_fk` FOREIGN KEY (`story_id`) REFERENCES `stories` (`id`),
  CONSTRAINT `taggings_tag_id_fk` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=273 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `tag` varchar(25) NOT NULL,
  `description` varchar(100) DEFAULT NULL,
  `privileged` tinyint(1) DEFAULT '0',
  `is_media` tinyint(1) DEFAULT '0',
  `inactive` tinyint(1) DEFAULT '0',
  `hotness_mod` float DEFAULT '0',
  `permit_by_new_users` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag` (`tag`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 DEFAULT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 DEFAULT NULL,
  `password_digest` varchar(75) CHARACTER SET utf8mb4 DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `is_admin` tinyint(1) DEFAULT '0',
  `password_reset_token` varchar(75) CHARACTER SET utf8mb4 DEFAULT NULL,
  `session_token` varchar(75) CHARACTER SET utf8mb4 NOT NULL DEFAULT '',
  `about` mediumtext CHARACTER SET utf8mb4,
  `invited_by_user_id` bigint(20) unsigned DEFAULT NULL,
  `is_moderator` tinyint(1) DEFAULT '0',
  `pushover_mentions` tinyint(1) DEFAULT '0',
  `rss_token` varchar(75) CHARACTER SET utf8mb4 DEFAULT NULL,
  `mailing_list_token` varchar(75) CHARACTER SET utf8mb4 DEFAULT NULL,
  `mailing_list_mode` int(11) DEFAULT '0',
  `karma` int(11) NOT NULL DEFAULT '0',
  `banned_at` datetime DEFAULT NULL,
  `banned_by_user_id` bigint(20) unsigned DEFAULT NULL,
  `banned_reason` varchar(200) CHARACTER SET utf8mb4 DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `disabled_invite_at` datetime DEFAULT NULL,
  `disabled_invite_by_user_id` bigint(20) unsigned DEFAULT NULL,
  `disabled_invite_reason` varchar(200) DEFAULT NULL,
  `settings` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_hash` (`session_token`),
  UNIQUE KEY `mailing_list_token` (`mailing_list_token`),
  UNIQUE KEY `password_reset_token` (`password_reset_token`),
  UNIQUE KEY `rss_token` (`rss_token`),
  UNIQUE KEY `username` (`username`),
  KEY `users_banned_by_user_id_fk` (`banned_by_user_id`),
  KEY `users_disabled_invite_by_user_id_fk` (`disabled_invite_by_user_id`),
  KEY `users_invited_by_user_id_fk` (`invited_by_user_id`),
  KEY `mailing_list_enabled` (`mailing_list_mode`),
  CONSTRAINT `users_banned_by_user_id_fk` FOREIGN KEY (`banned_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `users_disabled_invite_by_user_id_fk` FOREIGN KEY (`disabled_invite_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `users_invited_by_user_id_fk` FOREIGN KEY (`invited_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=310 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `votes`
--

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-05-16 23:39:52

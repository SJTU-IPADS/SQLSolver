-- MySQL dump 10.13  Distrib 8.0.19, for osx10.15 (x86_64)
--
-- Host: localhost    Database: diaspora_test
-- ------------------------------------------------------
-- Server version	8.0.19

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `account_deletions`
--

DROP TABLE IF EXISTS `account_deletions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_deletions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `person_id` int DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_account_deletions_on_person_id` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `account_migrations`
--

DROP TABLE IF EXISTS `account_migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `account_migrations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `old_person_id` int NOT NULL,
  `new_person_id` int NOT NULL,
  `completed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_account_migrations_on_old_person_id_and_new_person_id` (`old_person_id`,`new_person_id`),
  UNIQUE KEY `index_account_migrations_on_old_person_id` (`old_person_id`),
  KEY `fk_rails_610fe19943` (`new_person_id`),
  CONSTRAINT `fk_rails_610fe19943` FOREIGN KEY (`new_person_id`) REFERENCES `people` (`id`),
  CONSTRAINT `fk_rails_ddbe553eee` FOREIGN KEY (`old_person_id`) REFERENCES `people` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ar_internal_metadata`
--

DROP TABLE IF EXISTS `ar_internal_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ar_internal_metadata` (
  `key` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `value` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `aspect_memberships`
--

DROP TABLE IF EXISTS `aspect_memberships`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspect_memberships` (
  `id` int NOT NULL AUTO_INCREMENT,
  `aspect_id` int NOT NULL,
  `contact_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_aspect_memberships_on_aspect_id_and_contact_id` (`aspect_id`,`contact_id`),
  KEY `index_aspect_memberships_on_aspect_id` (`aspect_id`),
  KEY `index_aspect_memberships_on_contact_id` (`contact_id`),
  CONSTRAINT `aspect_memberships_aspect_id_fk` FOREIGN KEY (`aspect_id`) REFERENCES `aspects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `aspect_memberships_contact_id_fk` FOREIGN KEY (`contact_id`) REFERENCES `contacts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `aspect_visibilities`
--

DROP TABLE IF EXISTS `aspect_visibilities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspect_visibilities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `shareable_id` int NOT NULL,
  `aspect_id` int NOT NULL,
  `shareable_type` varchar(255) COLLATE utf8mb4_bin NOT NULL DEFAULT 'Post',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_aspect_visibilities_on_shareable_and_aspect_id` (`shareable_id`,`shareable_type`(189),`aspect_id`),
  KEY `index_aspect_visibilities_on_aspect_id` (`aspect_id`),
  KEY `index_aspect_visibilities_on_shareable_id_and_shareable_type` (`shareable_id`,`shareable_type`(190)),
  CONSTRAINT `aspect_visibilities_aspect_id_fk` FOREIGN KEY (`aspect_id`) REFERENCES `aspects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `aspects`
--

DROP TABLE IF EXISTS `aspects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `aspects` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `order_id` int DEFAULT NULL,
  `post_default` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_aspects_on_user_id_and_name` (`user_id`,`name`(190)),
  KEY `index_aspects_on_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authorizations`
--

DROP TABLE IF EXISTS `authorizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `authorizations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `o_auth_application_id` int DEFAULT NULL,
  `refresh_token` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `code` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `redirect_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `nonce` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `scopes` text COLLATE utf8mb4_bin,
  `code_used` tinyint(1) DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_authorizations_on_o_auth_application_id` (`o_auth_application_id`),
  KEY `index_authorizations_on_user_id` (`user_id`),
  CONSTRAINT `fk_rails_4ecef5b8c5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rails_e166644de5` FOREIGN KEY (`o_auth_application_id`) REFERENCES `o_auth_applications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blocks`
--

DROP TABLE IF EXISTS `blocks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blocks` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `person_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_blocks_on_user_id_and_person_id` (`user_id`,`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comment_signatures`
--

DROP TABLE IF EXISTS `comment_signatures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_signatures` (
  `comment_id` int NOT NULL,
  `author_signature` text COLLATE utf8mb4_bin NOT NULL,
  `signature_order_id` int NOT NULL,
  `additional_data` text COLLATE utf8mb4_bin,
  UNIQUE KEY `index_comment_signatures_on_comment_id` (`comment_id`),
  KEY `comment_signatures_signature_orders_id_fk` (`signature_order_id`),
  CONSTRAINT `comment_signatures_comment_id_fk` FOREIGN KEY (`comment_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comment_signatures_signature_orders_id_fk` FOREIGN KEY (`signature_order_id`) REFERENCES `signature_orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `text` text COLLATE utf8mb4_bin NOT NULL,
  `commentable_id` int NOT NULL,
  `author_id` int NOT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `likes_count` int NOT NULL DEFAULT '0',
  `commentable_type` varchar(60) COLLATE utf8mb4_bin NOT NULL DEFAULT 'Post',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_comments_on_guid` (`guid`(191)),
  KEY `index_comments_on_person_id` (`author_id`),
  KEY `index_comments_on_commentable_id_and_commentable_type` (`commentable_id`,`commentable_type`),
  CONSTRAINT `comments_author_id_fk` FOREIGN KEY (`author_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contacts`
--

DROP TABLE IF EXISTS `contacts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contacts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `person_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `sharing` tinyint(1) NOT NULL DEFAULT '0',
  `receiving` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_contacts_on_user_id_and_person_id` (`user_id`,`person_id`),
  KEY `index_contacts_on_person_id` (`person_id`),
  CONSTRAINT `contacts_person_id_fk` FOREIGN KEY (`person_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation_visibilities`
--

DROP TABLE IF EXISTS `conversation_visibilities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_visibilities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `conversation_id` int NOT NULL,
  `person_id` int NOT NULL,
  `unread` int NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_conversation_visibilities_usefully` (`conversation_id`,`person_id`),
  KEY `index_conversation_visibilities_on_conversation_id` (`conversation_id`),
  KEY `index_conversation_visibilities_on_person_id` (`person_id`),
  CONSTRAINT `conversation_visibilities_conversation_id_fk` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `conversation_visibilities_person_id_fk` FOREIGN KEY (`person_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversations`
--

DROP TABLE IF EXISTS `conversations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `subject` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `author_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_conversations_on_guid` (`guid`(191)),
  KEY `conversations_author_id_fk` (`author_id`),
  CONSTRAINT `conversations_author_id_fk` FOREIGN KEY (`author_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `invitation_codes`
--

DROP TABLE IF EXISTS `invitation_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invitation_codes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `token` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `count` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `like_signatures`
--

DROP TABLE IF EXISTS `like_signatures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `like_signatures` (
  `like_id` int NOT NULL,
  `author_signature` text COLLATE utf8mb4_bin NOT NULL,
  `signature_order_id` int NOT NULL,
  `additional_data` text COLLATE utf8mb4_bin,
  UNIQUE KEY `index_like_signatures_on_like_id` (`like_id`),
  KEY `like_signatures_signature_orders_id_fk` (`signature_order_id`),
  CONSTRAINT `like_signatures_like_id_fk` FOREIGN KEY (`like_id`) REFERENCES `likes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `like_signatures_signature_orders_id_fk` FOREIGN KEY (`signature_order_id`) REFERENCES `signature_orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `likes`
--

DROP TABLE IF EXISTS `likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `likes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `positive` tinyint(1) DEFAULT '1',
  `target_id` int DEFAULT NULL,
  `author_id` int DEFAULT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `target_type` varchar(60) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_likes_on_target_id_and_author_id_and_target_type` (`target_id`,`author_id`,`target_type`),
  UNIQUE KEY `index_likes_on_guid` (`guid`(191)),
  KEY `likes_author_id_fk` (`author_id`),
  KEY `index_likes_on_post_id` (`target_id`),
  CONSTRAINT `likes_author_id_fk` FOREIGN KEY (`author_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `locations`
--

DROP TABLE IF EXISTS `locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `locations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `lat` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `lng` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `status_message_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_locations_on_status_message_id` (`status_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `mentions`
--

DROP TABLE IF EXISTS `mentions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `mentions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `mentions_container_id` int NOT NULL,
  `person_id` int NOT NULL,
  `mentions_container_type` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_mentions_on_person_and_mc_id_and_mc_type` (`person_id`,`mentions_container_id`,`mentions_container_type`(191)),
  KEY `index_mentions_on_mc_id_and_mc_type` (`mentions_container_id`,`mentions_container_type`(191)),
  KEY `index_mentions_on_person_id` (`person_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` int NOT NULL AUTO_INCREMENT,
  `conversation_id` int NOT NULL,
  `author_id` int NOT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `text` text COLLATE utf8mb4_bin NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_messages_on_guid` (`guid`(191)),
  KEY `index_messages_on_author_id` (`author_id`),
  KEY `messages_conversation_id_fk` (`conversation_id`),
  CONSTRAINT `messages_author_id_fk` FOREIGN KEY (`author_id`) REFERENCES `people` (`id`) ON DELETE CASCADE,
  CONSTRAINT `messages_conversation_id_fk` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notification_actors`
--

DROP TABLE IF EXISTS `notification_actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_actors` (
  `id` int NOT NULL AUTO_INCREMENT,
  `notification_id` int DEFAULT NULL,
  `person_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_notification_actors_on_notification_id_and_person_id` (`notification_id`,`person_id`),
  KEY `index_notification_actors_on_notification_id` (`notification_id`),
  KEY `index_notification_actors_on_person_id` (`person_id`),
  CONSTRAINT `notification_actors_notification_id_fk` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `target_type` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `target_id` int DEFAULT NULL,
  `recipient_id` int NOT NULL,
  `unread` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `type` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_notifications_on_guid` (`guid`(191)),
  KEY `index_notifications_on_recipient_id` (`recipient_id`),
  KEY `index_notifications_on_target_id` (`target_id`),
  KEY `index_notifications_on_target_type_and_target_id` (`target_type`(190),`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `o_auth_access_tokens`
--

DROP TABLE IF EXISTS `o_auth_access_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `o_auth_access_tokens` (
  `id` int NOT NULL AUTO_INCREMENT,
  `authorization_id` int DEFAULT NULL,
  `token` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_o_auth_access_tokens_on_token` (`token`(191)),
  KEY `index_o_auth_access_tokens_on_authorization_id` (`authorization_id`),
  CONSTRAINT `fk_rails_5debabcff3` FOREIGN KEY (`authorization_id`) REFERENCES `authorizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `o_auth_applications`
--

DROP TABLE IF EXISTS `o_auth_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `o_auth_applications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `client_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `client_secret` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `client_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `redirect_uris` text COLLATE utf8mb4_bin,
  `response_types` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `grant_types` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `application_type` varchar(255) COLLATE utf8mb4_bin DEFAULT 'web',
  `contacts` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `logo_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `client_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `policy_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `tos_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `sector_identifier_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `token_endpoint_auth_method` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `jwks` text COLLATE utf8mb4_bin,
  `jwks_uri` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `ppid` tinyint(1) DEFAULT '0',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_o_auth_applications_on_client_id` (`client_id`(191)),
  KEY `index_o_auth_applications_on_user_id` (`user_id`),
  CONSTRAINT `fk_rails_ad75323da2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `o_embed_caches`
--

DROP TABLE IF EXISTS `o_embed_caches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `o_embed_caches` (
  `id` int NOT NULL AUTO_INCREMENT,
  `url` varchar(1024) COLLATE utf8mb4_bin NOT NULL,
  `data` text COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_o_embed_caches_on_url` (`url`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `open_graph_caches`
--

DROP TABLE IF EXISTS `open_graph_caches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `open_graph_caches` (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `ob_type` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `image` text COLLATE utf8mb4_bin,
  `url` text COLLATE utf8mb4_bin,
  `description` text COLLATE utf8mb4_bin,
  `video_url` text COLLATE utf8mb4_bin,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `participations`
--

DROP TABLE IF EXISTS `participations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `participations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `target_id` int DEFAULT NULL,
  `target_type` varchar(60) COLLATE utf8mb4_bin NOT NULL,
  `author_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `count` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_participations_on_target_id_and_target_type_and_author_id` (`target_id`,`target_type`,`author_id`),
  KEY `index_participations_on_author_id` (`author_id`),
  KEY `index_participations_on_guid` (`guid`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `people`
--

DROP TABLE IF EXISTS `people`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `people` (
  `id` int NOT NULL AUTO_INCREMENT,
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `diaspora_handle` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `serialized_public_key` text COLLATE utf8mb4_bin NOT NULL,
  `owner_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `closed_account` tinyint(1) DEFAULT '0',
  `fetch_status` int DEFAULT '0',
  `pod_id` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_people_on_diaspora_handle` (`diaspora_handle`(191)),
  UNIQUE KEY `index_people_on_guid` (`guid`(191)),
  UNIQUE KEY `index_people_on_owner_id` (`owner_id`),
  KEY `people_pod_id_fk` (`pod_id`),
  CONSTRAINT `people_pod_id_fk` FOREIGN KEY (`pod_id`) REFERENCES `pods` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `photos`
--

DROP TABLE IF EXISTS `photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `photos` (
  `id` int NOT NULL AUTO_INCREMENT,
  `author_id` int NOT NULL,
  `public` tinyint(1) NOT NULL DEFAULT '0',
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `pending` tinyint(1) NOT NULL DEFAULT '0',
  `text` text COLLATE utf8mb4_bin,
  `remote_photo_path` text COLLATE utf8mb4_bin,
  `remote_photo_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `random_string` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `processed_image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `unprocessed_image` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `status_message_guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `height` int DEFAULT NULL,
  `width` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_photos_on_guid` (`guid`(191)),
  KEY `index_photos_on_author_id` (`author_id`),
  KEY `index_photos_on_status_message_guid` (`status_message_guid`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pods`
--

DROP TABLE IF EXISTS `pods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pods` (
  `id` int NOT NULL AUTO_INCREMENT,
  `host` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `ssl` tinyint(1) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `status` int DEFAULT '0',
  `checked_at` datetime DEFAULT '1970-01-01 00:00:00',
  `offline_since` datetime DEFAULT NULL,
  `response_time` int DEFAULT '-1',
  `software` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `error` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `port` int DEFAULT NULL,
  `blocked` tinyint(1) DEFAULT '0',
  `scheduled_check` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_pods_on_host_and_port` (`host`(190),`port`),
  KEY `index_pods_on_checked_at` (`checked_at`),
  KEY `index_pods_on_offline_since` (`offline_since`),
  KEY `index_pods_on_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `poll_answers`
--

DROP TABLE IF EXISTS `poll_answers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `poll_answers` (
  `id` int NOT NULL AUTO_INCREMENT,
  `answer` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `poll_id` int NOT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `vote_count` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_poll_answers_on_guid` (`guid`(191)),
  KEY `index_poll_answers_on_poll_id` (`poll_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `poll_participation_signatures`
--

DROP TABLE IF EXISTS `poll_participation_signatures`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `poll_participation_signatures` (
  `poll_participation_id` int NOT NULL,
  `author_signature` text COLLATE utf8mb4_bin NOT NULL,
  `signature_order_id` int NOT NULL,
  `additional_data` text COLLATE utf8mb4_bin,
  UNIQUE KEY `index_poll_participation_signatures_on_poll_participation_id` (`poll_participation_id`),
  KEY `poll_participation_signatures_signature_orders_id_fk` (`signature_order_id`),
  CONSTRAINT `poll_participation_signatures_poll_participation_id_fk` FOREIGN KEY (`poll_participation_id`) REFERENCES `poll_participations` (`id`) ON DELETE CASCADE,
  CONSTRAINT `poll_participation_signatures_signature_orders_id_fk` FOREIGN KEY (`signature_order_id`) REFERENCES `signature_orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `poll_participations`
--

DROP TABLE IF EXISTS `poll_participations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `poll_participations` (
  `id` int NOT NULL AUTO_INCREMENT,
  `poll_answer_id` int NOT NULL,
  `author_id` int NOT NULL,
  `poll_id` int NOT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_poll_participations_on_poll_id_and_author_id` (`poll_id`,`author_id`),
  UNIQUE KEY `index_poll_participations_on_guid` (`guid`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `polls`
--

DROP TABLE IF EXISTS `polls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `polls` (
  `id` int NOT NULL AUTO_INCREMENT,
  `question` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `status_message_id` int NOT NULL,
  `status` tinyint(1) DEFAULT NULL,
  `guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_polls_on_guid` (`guid`(191)),
  KEY `index_polls_on_status_message_id` (`status_message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `author_id` int NOT NULL,
  `public` tinyint(1) NOT NULL DEFAULT '0',
  `guid` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `type` varchar(40) COLLATE utf8mb4_bin NOT NULL,
  `text` text COLLATE utf8mb4_bin,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `provider_display_name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `root_guid` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `likes_count` int DEFAULT '0',
  `comments_count` int DEFAULT '0',
  `o_embed_cache_id` int DEFAULT NULL,
  `reshares_count` int DEFAULT '0',
  `interacted_at` datetime DEFAULT NULL,
  `tweet_id` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `open_graph_cache_id` int DEFAULT NULL,
  `tumblr_ids` text COLLATE utf8mb4_bin,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_posts_on_guid` (`guid`(191)),
  UNIQUE KEY `index_posts_on_author_id_and_root_guid` (`author_id`,`root_guid`(190)),
  KEY `index_posts_on_person_id` (`author_id`),
  KEY `index_posts_on_created_at_and_id` (`created_at`,`id`),
  KEY `index_posts_on_id_and_type` (`id`,`type`),
  KEY `index_posts_on_root_guid` (`root_guid`(191)),
  CONSTRAINT `posts_author_id_fk` FOREIGN KEY (`author_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ppid`
--

DROP TABLE IF EXISTS `ppid`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ppid` (
  `id` int NOT NULL AUTO_INCREMENT,
  `o_auth_application_id` int DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `guid` varchar(32) COLLATE utf8mb4_bin DEFAULT NULL,
  `identifier` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `index_ppid_on_o_auth_application_id` (`o_auth_application_id`),
  KEY `index_ppid_on_user_id` (`user_id`),
  CONSTRAINT `fk_rails_150457f962` FOREIGN KEY (`o_auth_application_id`) REFERENCES `o_auth_applications` (`id`),
  CONSTRAINT `fk_rails_e6b8e5264f` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `profiles`
--

DROP TABLE IF EXISTS `profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `profiles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `diaspora_handle` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `first_name` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `last_name` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `image_url` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `image_url_small` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `image_url_medium` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `birthday` date DEFAULT NULL,
  `gender` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `bio` text COLLATE utf8mb4_bin,
  `searchable` tinyint(1) NOT NULL DEFAULT '1',
  `person_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `location` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `full_name` varchar(70) COLLATE utf8mb4_bin DEFAULT NULL,
  `nsfw` tinyint(1) DEFAULT '0',
  `public_details` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `index_profiles_on_full_name_and_searchable` (`full_name`,`searchable`),
  KEY `index_profiles_on_full_name` (`full_name`),
  KEY `index_profiles_on_person_id` (`person_id`),
  CONSTRAINT `profiles_person_id_fk` FOREIGN KEY (`person_id`) REFERENCES `people` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `references`
--

DROP TABLE IF EXISTS `references`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `references` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` int NOT NULL,
  `source_type` varchar(60) COLLATE utf8mb4_bin NOT NULL,
  `target_id` int NOT NULL,
  `target_type` varchar(60) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_references_on_source_and_target` (`source_id`,`source_type`,`target_id`,`target_type`),
  KEY `index_references_on_source_id_and_source_type` (`source_id`,`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` int NOT NULL AUTO_INCREMENT,
  `item_id` int NOT NULL,
  `item_type` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `reviewed` tinyint(1) DEFAULT '0',
  `text` text COLLATE utf8mb4_bin,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_reports_on_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `person_id` int DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_roles_on_person_id_and_name` (`person_id`,`name`(190))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_migrations`
--

DROP TABLE IF EXISTS `schema_migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schema_migrations` (
  `version` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `services`
--

DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `services` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(127) COLLATE utf8mb4_bin NOT NULL,
  `user_id` int NOT NULL,
  `uid` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `access_token` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `access_secret` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `nickname` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_services_on_type_and_uid` (`type`(64),`uid`),
  KEY `index_services_on_user_id` (`user_id`),
  CONSTRAINT `services_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `share_visibilities`
--

DROP TABLE IF EXISTS `share_visibilities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `share_visibilities` (
  `id` int NOT NULL AUTO_INCREMENT,
  `shareable_id` int NOT NULL,
  `hidden` tinyint(1) NOT NULL DEFAULT '0',
  `shareable_type` varchar(60) COLLATE utf8mb4_bin NOT NULL DEFAULT 'Post',
  `user_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `shareable_and_user_id` (`shareable_id`,`shareable_type`,`user_id`),
  KEY `shareable_and_hidden_and_user_id` (`shareable_id`,`shareable_type`,`hidden`,`user_id`),
  KEY `index_post_visibilities_on_post_id` (`shareable_id`),
  KEY `index_share_visibilities_on_user_id` (`user_id`),
  CONSTRAINT `share_visibilities_user_id_fk` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `signature_orders`
--

DROP TABLE IF EXISTS `signature_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `signature_orders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `order` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_signature_orders_on_order` (`order`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `simple_captcha_data`
--

DROP TABLE IF EXISTS `simple_captcha_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `simple_captcha_data` (
  `id` int NOT NULL AUTO_INCREMENT,
  `key` varchar(40) COLLATE utf8mb4_bin DEFAULT NULL,
  `value` varchar(12) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_key` (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag_followings`
--

DROP TABLE IF EXISTS `tag_followings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag_followings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tag_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_tag_followings_on_tag_id_and_user_id` (`tag_id`,`user_id`),
  KEY `index_tag_followings_on_tag_id` (`tag_id`),
  KEY `index_tag_followings_on_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taggings`
--

DROP TABLE IF EXISTS `taggings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `taggings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tag_id` int DEFAULT NULL,
  `taggable_id` int DEFAULT NULL,
  `taggable_type` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `tagger_id` int DEFAULT NULL,
  `tagger_type` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `context` varchar(127) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_taggings_uniquely` (`taggable_id`,`taggable_type`,`tag_id`),
  KEY `index_taggings_on_created_at` (`created_at`),
  KEY `index_taggings_on_tag_id` (`tag_id`),
  KEY `index_taggings_on_taggable_id_and_taggable_type_and_context` (`taggable_id`,`taggable_type`(95),`context`(95))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tags` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `taggings_count` int DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_tags_on_name` (`name`(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_preferences`
--

DROP TABLE IF EXISTS `user_preferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preferences` (
  `id` int NOT NULL AUTO_INCREMENT,
  `email_type` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_user_preferences_on_user_id_and_email_type` (`user_id`,`email_type`(190))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `serialized_private_key` text COLLATE utf8mb4_bin,
  `getting_started` tinyint(1) NOT NULL DEFAULT '1',
  `disable_mail` tinyint(1) NOT NULL DEFAULT '0',
  `language` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `email` varchar(255) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `encrypted_password` varchar(255) COLLATE utf8mb4_bin NOT NULL DEFAULT '',
  `reset_password_token` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `remember_created_at` datetime DEFAULT NULL,
  `sign_in_count` int DEFAULT '0',
  `current_sign_in_at` datetime DEFAULT NULL,
  `last_sign_in_at` datetime DEFAULT NULL,
  `current_sign_in_ip` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `last_sign_in_ip` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  `invited_by_id` int DEFAULT NULL,
  `authentication_token` varchar(30) COLLATE utf8mb4_bin DEFAULT NULL,
  `unconfirmed_email` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `confirm_email_token` varchar(30) COLLATE utf8mb4_bin DEFAULT NULL,
  `locked_at` datetime DEFAULT NULL,
  `show_community_spotlight_in_stream` tinyint(1) NOT NULL DEFAULT '1',
  `auto_follow_back` tinyint(1) DEFAULT '0',
  `auto_follow_back_aspect_id` int DEFAULT NULL,
  `hidden_shareables` text COLLATE utf8mb4_bin,
  `reset_password_sent_at` datetime DEFAULT NULL,
  `last_seen` datetime DEFAULT NULL,
  `remove_after` datetime DEFAULT NULL,
  `export` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `exported_at` datetime DEFAULT NULL,
  `exporting` tinyint(1) DEFAULT '0',
  `strip_exif` tinyint(1) DEFAULT '1',
  `exported_photos_file` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `exported_photos_at` datetime DEFAULT NULL,
  `exporting_photos` tinyint(1) DEFAULT '0',
  `color_theme` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `post_default_public` tinyint(1) DEFAULT '0',
  `consumed_timestep` int DEFAULT NULL,
  `otp_required_for_login` tinyint(1) DEFAULT NULL,
  `otp_backup_codes` text COLLATE utf8mb4_bin,
  `plain_otp_secret` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_users_on_email` (`email`(191)),
  UNIQUE KEY `index_users_on_username` (`username`(191)),
  UNIQUE KEY `index_users_on_authentication_token` (`authentication_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-04-28 14:27:43

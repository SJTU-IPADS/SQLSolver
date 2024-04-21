-- MySQL dump 10.13  Distrib 5.7.29, for osx10.15 (x86_64)
--
-- Host: localhost    Database: fat_free_crm_test
-- ------------------------------------------------------
-- Server version	5.7.29

/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE = @@TIME_ZONE */;
/*!40103 SET TIME_ZONE = '+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS = @@UNIQUE_CHECKS, UNIQUE_CHECKS = 0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

--
-- Table structure for table `account_contacts`
--

DROP TABLE IF EXISTS `account_contacts`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_contacts`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `account_id` int(11)  DEFAULT NULL,
    `contact_id` int(11)  DEFAULT NULL,
    `deleted_at` datetime DEFAULT NULL,
    `created_at` datetime DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_account_contacts_on_account_id_and_contact_id` (`account_id`, `contact_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 32
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `account_opportunities`
--

DROP TABLE IF EXISTS `account_opportunities`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `account_opportunities`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `account_id`     int(11)  DEFAULT NULL,
    `opportunity_id` int(11)  DEFAULT NULL,
    `deleted_at`     datetime DEFAULT NULL,
    `created_at`     datetime DEFAULT NULL,
    `updated_at`     datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_account_opportunities_on_account_id_and_opportunity_id` (`account_id`, `opportunity_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 270
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `accounts`
--

DROP TABLE IF EXISTS `accounts`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `accounts`
(
    `id`                  int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`             int(11)              DEFAULT NULL,
    `assigned_to`         int(11)              DEFAULT NULL,
    `name`                varchar(64) NOT NULL DEFAULT '',
    `access`              varchar(8)           DEFAULT 'Public',
    `website`             varchar(64)          DEFAULT NULL,
    `toll_free_phone`     varchar(32)          DEFAULT NULL,
    `phone`               varchar(32)          DEFAULT NULL,
    `fax`                 varchar(32)          DEFAULT NULL,
    `deleted_at`          datetime             DEFAULT NULL,
    `created_at`          datetime             DEFAULT NULL,
    `updated_at`          datetime             DEFAULT NULL,
    `email`               varchar(254)         DEFAULT NULL,
    `background_info`     varchar(255)         DEFAULT NULL,
    `rating`              int(11)     NOT NULL DEFAULT '0',
    `category`            varchar(32)          DEFAULT NULL,
    `subscribed_users`    text,
    `contacts_count`      int(11)              DEFAULT '0',
    `opportunities_count` int(11)              DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_accounts_on_user_id_and_name_and_deleted_at` (`user_id`, `name`, `deleted_at`),
    KEY `index_accounts_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1305
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `activities`
--

DROP TABLE IF EXISTS `activities`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `activities`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `user_id`      int(11)      DEFAULT NULL,
    `subject_type` varchar(255) DEFAULT NULL,
    `subject_id`   int(11)      DEFAULT NULL,
    `action`       varchar(32)  DEFAULT 'created',
    `info`         varchar(255) DEFAULT '',
    `private`      tinyint(1)   DEFAULT '0',
    `created_at`   datetime     DEFAULT NULL,
    `updated_at`   datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_activities_on_user_id` (`user_id`),
    KEY `index_activities_on_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `addresses`
--

DROP TABLE IF EXISTS `addresses`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `addresses`
(
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `street1`          varchar(255) DEFAULT NULL,
    `street2`          varchar(255) DEFAULT NULL,
    `city`             varchar(64)  DEFAULT NULL,
    `state`            varchar(64)  DEFAULT NULL,
    `zipcode`          varchar(16)  DEFAULT NULL,
    `country`          varchar(64)  DEFAULT NULL,
    `full_address`     varchar(255) DEFAULT NULL,
    `address_type`     varchar(16)  DEFAULT NULL,
    `addressable_type` varchar(255) DEFAULT NULL,
    `addressable_id`   int(11)      DEFAULT NULL,
    `created_at`       datetime     DEFAULT NULL,
    `updated_at`       datetime     DEFAULT NULL,
    `deleted_at`       datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_addresses_on_addressable_id_and_addressable_type` (`addressable_id`, `addressable_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ar_internal_metadata`
--

DROP TABLE IF EXISTS `ar_internal_metadata`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ar_internal_metadata`
(
    `key`        varchar(255) NOT NULL,
    `value`      varchar(255) DEFAULT NULL,
    `created_at` datetime     NOT NULL,
    `updated_at` datetime     NOT NULL,
    PRIMARY KEY (`key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `avatars`
--

DROP TABLE IF EXISTS `avatars`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `avatars`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `user_id`            int(11)      DEFAULT NULL,
    `entity_type`        varchar(255) DEFAULT NULL,
    `entity_id`          int(11)      DEFAULT NULL,
    `image_file_size`    int(11)      DEFAULT NULL,
    `image_file_name`    varchar(255) DEFAULT NULL,
    `image_content_type` varchar(255) DEFAULT NULL,
    `created_at`         datetime     DEFAULT NULL,
    `updated_at`         datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 8
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `campaigns`
--

DROP TABLE IF EXISTS `campaigns`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `campaigns`
(
    `id`                  int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`             int(11)              DEFAULT NULL,
    `assigned_to`         int(11)              DEFAULT NULL,
    `name`                varchar(64) NOT NULL DEFAULT '',
    `access`              varchar(8)           DEFAULT 'Public',
    `status`              varchar(64)          DEFAULT NULL,
    `budget`              decimal(12, 2)       DEFAULT NULL,
    `target_leads`        int(11)              DEFAULT NULL,
    `target_conversion`   float                DEFAULT NULL,
    `target_revenue`      decimal(12, 2)       DEFAULT NULL,
    `leads_count`         int(11)              DEFAULT NULL,
    `opportunities_count` int(11)              DEFAULT NULL,
    `revenue`             decimal(12, 2)       DEFAULT NULL,
    `starts_on`           date                 DEFAULT NULL,
    `ends_on`             date                 DEFAULT NULL,
    `objectives`          text,
    `deleted_at`          datetime             DEFAULT NULL,
    `created_at`          datetime             DEFAULT NULL,
    `updated_at`          datetime             DEFAULT NULL,
    `background_info`     varchar(255)         DEFAULT NULL,
    `subscribed_users`    text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_campaigns_on_user_id_and_name_and_deleted_at` (`user_id`, `name`, `deleted_at`),
    KEY `index_campaigns_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 777
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `comments`
(
    `id`               int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`          int(11)              DEFAULT NULL,
    `commentable_type` varchar(255)         DEFAULT NULL,
    `commentable_id`   int(11)              DEFAULT NULL,
    `private`          tinyint(1)           DEFAULT NULL,
    `title`            varchar(255)         DEFAULT '',
    `comment`          text,
    `created_at`       datetime             DEFAULT NULL,
    `updated_at`       datetime             DEFAULT NULL,
    `state`            varchar(16) NOT NULL DEFAULT 'Expanded',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 77
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contact_opportunities`
--

DROP TABLE IF EXISTS `contact_opportunities`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contact_opportunities`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `contact_id`     int(11)     DEFAULT NULL,
    `opportunity_id` int(11)     DEFAULT NULL,
    `role`           varchar(32) DEFAULT NULL,
    `deleted_at`     datetime    DEFAULT NULL,
    `created_at`     datetime    DEFAULT NULL,
    `updated_at`     datetime    DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_contact_opportunities_on_contact_id_and_opportunity_id` (`contact_id`, `opportunity_id`),
    KEY `index_contact_opportunities_on_opportunity_id_and_contact_id` (`opportunity_id`, `contact_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 28
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `contacts`
--

DROP TABLE IF EXISTS `contacts`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `contacts`
(
    `id`               int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`          int(11)              DEFAULT NULL,
    `lead_id`          int(11)              DEFAULT NULL,
    `assigned_to`      int(11)              DEFAULT NULL,
    `reports_to`       int(11)              DEFAULT NULL,
    `first_name`       varchar(64) NOT NULL DEFAULT '',
    `last_name`        varchar(64) NOT NULL DEFAULT '',
    `access`           varchar(8)           DEFAULT 'Public',
    `title`            varchar(64)          DEFAULT NULL,
    `department`       varchar(64)          DEFAULT NULL,
    `source`           varchar(32)          DEFAULT NULL,
    `email`            varchar(254)         DEFAULT NULL,
    `alt_email`        varchar(254)         DEFAULT NULL,
    `phone`            varchar(32)          DEFAULT NULL,
    `mobile`           varchar(32)          DEFAULT NULL,
    `fax`              varchar(32)          DEFAULT NULL,
    `blog`             varchar(128)         DEFAULT NULL,
    `linkedin`         varchar(128)         DEFAULT NULL,
    `facebook`         varchar(128)         DEFAULT NULL,
    `twitter`          varchar(128)         DEFAULT NULL,
    `born_on`          date                 DEFAULT NULL,
    `do_not_call`      tinyint(1)  NOT NULL DEFAULT '0',
    `deleted_at`       datetime             DEFAULT NULL,
    `created_at`       datetime             DEFAULT NULL,
    `updated_at`       datetime             DEFAULT NULL,
    `background_info`  varchar(255)         DEFAULT NULL,
    `skype`            varchar(128)         DEFAULT NULL,
    `subscribed_users` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id_last_name_deleted` (`user_id`, `last_name`, `deleted_at`),
    KEY `index_contacts_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 2125
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `emails`
--

DROP TABLE IF EXISTS `emails`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `emails`
(
    `id`              int(11)      NOT NULL AUTO_INCREMENT,
    `imap_message_id` varchar(255) NOT NULL,
    `user_id`         int(11)               DEFAULT NULL,
    `mediator_type`   varchar(255)          DEFAULT NULL,
    `mediator_id`     int(11)               DEFAULT NULL,
    `sent_from`       varchar(255) NOT NULL,
    `sent_to`         varchar(255) NOT NULL,
    `cc`              varchar(255)          DEFAULT NULL,
    `bcc`             varchar(255)          DEFAULT NULL,
    `subject`         varchar(255)          DEFAULT NULL,
    `body`            text,
    `header`          text,
    `sent_at`         datetime              DEFAULT NULL,
    `received_at`     datetime              DEFAULT NULL,
    `deleted_at`      datetime              DEFAULT NULL,
    `created_at`      datetime              DEFAULT NULL,
    `updated_at`      datetime              DEFAULT NULL,
    `state`           varchar(16)  NOT NULL DEFAULT 'Expanded',
    PRIMARY KEY (`id`),
    KEY `index_emails_on_mediator_id_and_mediator_type` (`mediator_id`, `mediator_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 25
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `field_groups`
--

DROP TABLE IF EXISTS `field_groups`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `field_groups`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `name`       varchar(64)  DEFAULT NULL,
    `label`      varchar(128) DEFAULT NULL,
    `position`   int(11)      DEFAULT NULL,
    `hint`       varchar(255) DEFAULT NULL,
    `created_at` datetime     DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    `tag_id`     int(11)      DEFAULT NULL,
    `klass_name` varchar(32)  DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fields`
--

DROP TABLE IF EXISTS `fields`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fields`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `type`           varchar(255) DEFAULT NULL,
    `field_group_id` int(11)      DEFAULT NULL,
    `position`       int(11)      DEFAULT NULL,
    `name`           varchar(64)  DEFAULT NULL,
    `label`          varchar(128) DEFAULT NULL,
    `hint`           varchar(255) DEFAULT NULL,
    `placeholder`    varchar(255) DEFAULT NULL,
    `as`             varchar(32)  DEFAULT NULL,
    `collection`     text,
    `disabled`       tinyint(1)   DEFAULT NULL,
    `required`       tinyint(1)   DEFAULT NULL,
    `maxlength`      int(11)      DEFAULT NULL,
    `created_at`     datetime     DEFAULT NULL,
    `updated_at`     datetime     DEFAULT NULL,
    `pair_id`        int(11)      DEFAULT NULL,
    `settings`       text,
    `minlength`      int(11)      DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `index_fields_on_name` (`name`),
    KEY `index_fields_on_field_group_id` (`field_group_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 4
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `groups`
--

DROP TABLE IF EXISTS `groups`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `groups`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `name`       varchar(255) DEFAULT NULL,
    `created_at` datetime     DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 21
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `groups_users`
--

DROP TABLE IF EXISTS `groups_users`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `groups_users`
(
    `group_id` int(11) DEFAULT NULL,
    `user_id`  int(11) DEFAULT NULL,
    KEY `index_groups_users_on_group_id` (`group_id`),
    KEY `index_groups_users_on_user_id` (`user_id`),
    KEY `index_groups_users_on_group_id_and_user_id` (`group_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `leads`
--

DROP TABLE IF EXISTS `leads`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `leads`
(
    `id`               int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`          int(11)              DEFAULT NULL,
    `campaign_id`      int(11)              DEFAULT NULL,
    `assigned_to`      int(11)              DEFAULT NULL,
    `first_name`       varchar(64) NOT NULL DEFAULT '',
    `last_name`        varchar(64) NOT NULL DEFAULT '',
    `access`           varchar(8)           DEFAULT 'Public',
    `title`            varchar(64)          DEFAULT NULL,
    `company`          varchar(64)          DEFAULT NULL,
    `source`           varchar(32)          DEFAULT NULL,
    `status`           varchar(32)          DEFAULT NULL,
    `referred_by`      varchar(64)          DEFAULT NULL,
    `email`            varchar(254)         DEFAULT NULL,
    `alt_email`        varchar(254)         DEFAULT NULL,
    `phone`            varchar(32)          DEFAULT NULL,
    `mobile`           varchar(32)          DEFAULT NULL,
    `blog`             varchar(128)         DEFAULT NULL,
    `linkedin`         varchar(128)         DEFAULT NULL,
    `facebook`         varchar(128)         DEFAULT NULL,
    `twitter`          varchar(128)         DEFAULT NULL,
    `rating`           int(11)     NOT NULL DEFAULT '0',
    `do_not_call`      tinyint(1)  NOT NULL DEFAULT '0',
    `deleted_at`       datetime             DEFAULT NULL,
    `created_at`       datetime             DEFAULT NULL,
    `updated_at`       datetime             DEFAULT NULL,
    `background_info`  varchar(255)         DEFAULT NULL,
    `skype`            varchar(128)         DEFAULT NULL,
    `subscribed_users` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_leads_on_user_id_and_last_name_and_deleted_at` (`user_id`, `last_name`, `deleted_at`),
    KEY `index_leads_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 541
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lists`
--

DROP TABLE IF EXISTS `lists`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lists`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `name`       varchar(255) DEFAULT NULL,
    `url`        text,
    `created_at` datetime     DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    `user_id`    int(11)      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_lists_on_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `opportunities`
--

DROP TABLE IF EXISTS `opportunities`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `opportunities`
(
    `id`               int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`          int(11)              DEFAULT NULL,
    `campaign_id`      int(11)              DEFAULT NULL,
    `assigned_to`      int(11)              DEFAULT NULL,
    `name`             varchar(64) NOT NULL DEFAULT '',
    `access`           varchar(8)           DEFAULT 'Public',
    `source`           varchar(32)          DEFAULT NULL,
    `stage`            varchar(32)          DEFAULT NULL,
    `probability`      int(11)              DEFAULT NULL,
    `amount`           decimal(12, 2)       DEFAULT NULL,
    `discount`         decimal(12, 2)       DEFAULT NULL,
    `closes_on`        date                 DEFAULT NULL,
    `deleted_at`       datetime             DEFAULT NULL,
    `created_at`       datetime             DEFAULT NULL,
    `updated_at`       datetime             DEFAULT NULL,
    `background_info`  varchar(255)         DEFAULT NULL,
    `subscribed_users` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `id_name_deleted` (`user_id`, `name`, `deleted_at`),
    KEY `index_opportunities_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1218
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permissions`
(
    `id`         int(11) NOT NULL AUTO_INCREMENT,
    `user_id`    int(11)      DEFAULT NULL,
    `asset_type` varchar(255) DEFAULT NULL,
    `asset_id`   int(11)      DEFAULT NULL,
    `created_at` datetime     DEFAULT NULL,
    `updated_at` datetime     DEFAULT NULL,
    `group_id`   int(11)      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_permissions_on_user_id` (`user_id`),
    KEY `index_permissions_on_asset_id_and_asset_type` (`asset_id`, `asset_type`),
    KEY `index_permissions_on_group_id` (`group_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 68
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `preferences`
--

DROP TABLE IF EXISTS `preferences`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `preferences`
(
    `id`         int(11)     NOT NULL AUTO_INCREMENT,
    `user_id`    int(11)              DEFAULT NULL,
    `name`       varchar(32) NOT NULL DEFAULT '',
    `value`      text,
    `created_at` datetime             DEFAULT NULL,
    `updated_at` datetime             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_preferences_on_user_id_and_name` (`user_id`, `name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 82
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `schema_migrations`
--

DROP TABLE IF EXISTS `schema_migrations`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `schema_migrations`
(
    `version` varchar(255) NOT NULL,
    PRIMARY KEY (`version`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sessions`
--

DROP TABLE IF EXISTS `sessions`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sessions`
(
    `id`         int(11)      NOT NULL AUTO_INCREMENT,
    `session_id` varchar(255) NOT NULL,
    `data`       text,
    `created_at` datetime DEFAULT NULL,
    `updated_at` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_sessions_on_session_id` (`session_id`),
    KEY `index_sessions_on_updated_at` (`updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `settings`
--

DROP TABLE IF EXISTS `settings`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `settings`
(
    `id`         int(11)     NOT NULL AUTO_INCREMENT,
    `name`       varchar(32) NOT NULL DEFAULT '',
    `value`      text,
    `created_at` datetime             DEFAULT NULL,
    `updated_at` datetime             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_settings_on_name` (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 34
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `taggings`
--

DROP TABLE IF EXISTS `taggings`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `taggings`
(
    `id`            int(11) NOT NULL AUTO_INCREMENT,
    `tag_id`        int(11)      DEFAULT NULL,
    `taggable_id`   int(11)      DEFAULT NULL,
    `tagger_id`     int(11)      DEFAULT NULL,
    `tagger_type`   varchar(255) DEFAULT NULL,
    `taggable_type` varchar(50)  DEFAULT NULL,
    `context`       varchar(50)  DEFAULT NULL,
    `created_at`    datetime     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `taggings_idx` (`tag_id`, `taggable_id`, `taggable_type`, `context`),
    KEY `index_taggings_on_taggable_id_and_taggable_type_and_context` (`taggable_id`, `taggable_type`, `context`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags`
(
    `id`             int(11) NOT NULL AUTO_INCREMENT,
    `name`           varchar(255) DEFAULT NULL,
    `taggings_count` int(11)      DEFAULT '0',
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_tags_on_name` (`name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 9
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tasks`
(
    `id`               int(11)      NOT NULL AUTO_INCREMENT,
    `user_id`          int(11)               DEFAULT NULL,
    `assigned_to`      int(11)               DEFAULT NULL,
    `completed_by`     int(11)               DEFAULT NULL,
    `name`             varchar(255) NOT NULL DEFAULT '',
    `asset_type`       varchar(255)          DEFAULT NULL,
    `asset_id`         int(11)               DEFAULT NULL,
    `priority`         varchar(32)           DEFAULT NULL,
    `category`         varchar(32)           DEFAULT NULL,
    `bucket`           varchar(32)           DEFAULT NULL,
    `due_at`           datetime              DEFAULT NULL,
    `completed_at`     datetime              DEFAULT NULL,
    `deleted_at`       datetime              DEFAULT NULL,
    `created_at`       datetime              DEFAULT NULL,
    `updated_at`       datetime              DEFAULT NULL,
    `background_info`  varchar(255)          DEFAULT NULL,
    `subscribed_users` text,
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_tasks_on_user_id_and_name_and_deleted_at` (`user_id`, `name`, `deleted_at`),
    KEY `index_tasks_on_assigned_to` (`assigned_to`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 1142
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users`
(
    `id`                     int(11)      NOT NULL AUTO_INCREMENT,
    `username`               varchar(32)  NOT NULL DEFAULT '',
    `email`                  varchar(254) NOT NULL DEFAULT '',
    `first_name`             varchar(32)           DEFAULT NULL,
    `last_name`              varchar(32)           DEFAULT NULL,
    `title`                  varchar(64)           DEFAULT NULL,
    `company`                varchar(64)           DEFAULT NULL,
    `alt_email`              varchar(254)          DEFAULT NULL,
    `phone`                  varchar(32)           DEFAULT NULL,
    `mobile`                 varchar(32)           DEFAULT NULL,
    `aim`                    varchar(32)           DEFAULT NULL,
    `yahoo`                  varchar(32)           DEFAULT NULL,
    `google`                 varchar(32)           DEFAULT NULL,
    `skype`                  varchar(32)           DEFAULT NULL,
    `encrypted_password`     varchar(255) NOT NULL DEFAULT '',
    `password_salt`          varchar(255) NOT NULL DEFAULT '',
    `last_sign_in_at`        datetime              DEFAULT NULL,
    `current_sign_in_at`     datetime              DEFAULT NULL,
    `last_sign_in_ip`        varchar(255)          DEFAULT NULL,
    `current_sign_in_ip`     varchar(255)          DEFAULT NULL,
    `sign_in_count`          int(11)      NOT NULL DEFAULT '0',
    `deleted_at`             datetime              DEFAULT NULL,
    `created_at`             datetime              DEFAULT NULL,
    `updated_at`             datetime              DEFAULT NULL,
    `admin`                  tinyint(1)   NOT NULL DEFAULT '0',
    `suspended_at`           datetime              DEFAULT NULL,
    `unconfirmed_email`      varchar(254)          DEFAULT NULL,
    `reset_password_token`   varchar(255)          DEFAULT NULL,
    `reset_password_sent_at` datetime              DEFAULT NULL,
    `remember_token`         varchar(255)          DEFAULT NULL,
    `remember_created_at`    datetime              DEFAULT NULL,
    `authentication_token`   varchar(255)          DEFAULT NULL,
    `confirmation_token`     varchar(255)          DEFAULT NULL,
    `confirmed_at`           timestamp    NULL     DEFAULT NULL,
    `confirmation_sent_at`   timestamp    NULL     DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `index_users_on_username_and_deleted_at` (`username`, `deleted_at`),
    UNIQUE KEY `index_users_on_reset_password_token` (`reset_password_token`),
    UNIQUE KEY `index_users_on_remember_token` (`remember_token`),
    UNIQUE KEY `index_users_on_confirmation_token` (`confirmation_token`),
    UNIQUE KEY `index_users_on_authentication_token` (`authentication_token`),
    KEY `index_users_on_email` (`email`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 3246
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `versions`
--

DROP TABLE IF EXISTS `versions`;
/*!40101 SET @saved_cs_client = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `versions`
(
    `id`             int(11)      NOT NULL AUTO_INCREMENT,
    `item_type`      varchar(255) NOT NULL,
    `item_id`        int(11)      NOT NULL,
    `event`          varchar(512) NOT NULL,
    `whodunnit`      varchar(255) DEFAULT NULL,
    `object`         text,
    `created_at`     datetime     DEFAULT NULL,
    `object_changes` text,
    `related_id`     int(11)      DEFAULT NULL,
    `related_type`   varchar(255) DEFAULT NULL,
    `transaction_id` int(11)      DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `index_versions_on_item_type_and_item_id` (`item_type`, `item_id`),
    KEY `index_versions_on_whodunnit` (`whodunnit`),
    KEY `index_versions_on_created_at` (`created_at`),
    KEY `index_versions_on_transaction_id` (`transaction_id`),
    KEY `index_versions_on_related_id_and_related_type` (`related_id`, `related_type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 274
  DEFAULT CHARSET = utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE = @OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS = @OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;

-- Dump completed on 2020-05-15 10:46:26

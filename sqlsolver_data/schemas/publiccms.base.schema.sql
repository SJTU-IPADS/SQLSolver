-- MySQL dump 10.13  Distrib 5.7.28, for Linux (x86_64)
--
-- Host: 10.0.0.102    Database: publiccms_zz
-- ------------------------------------------------------
-- Server version	5.7.25-log

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
-- Table structure for table `cms_category`
--

DROP TABLE IF EXISTS `cms_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_category` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `parent_id` int(11) DEFAULT NULL COMMENT '父分类ID',
  `type_id` int(11) DEFAULT NULL COMMENT '分类类型',
  `child_ids` text COMMENT '所有子分类ID',
  `tag_type_ids` text COMMENT '标签分类',
  `code` varchar(50) NOT NULL COMMENT '编码',
  `template_path` varchar(255) DEFAULT NULL COMMENT '模板路径',
  `path` varchar(1000) DEFAULT NULL COMMENT '首页路径',
  `only_url` tinyint(1) NOT NULL COMMENT '外链',
  `has_static` tinyint(1) NOT NULL COMMENT '已经静态化',
  `url` varchar(1000) DEFAULT NULL COMMENT '首页地址',
  `content_path` varchar(1000) DEFAULT NULL COMMENT '内容路径',
  `contain_child` tinyint(1) NOT NULL DEFAULT '1' COMMENT '包含子分类内容',
  `page_size` int(11) DEFAULT NULL COMMENT '每页数据条数',
  `allow_contribute` tinyint(1) NOT NULL COMMENT '允许投稿',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '顺序',
  `hidden` tinyint(1) NOT NULL COMMENT '隐藏',
  `disabled` tinyint(1) NOT NULL COMMENT '是否删除',
  `extend_id` int(11) DEFAULT NULL COMMENT '扩展ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`site_id`,`code`),
  KEY `sort` (`sort`),
  KEY `type_id` (`type_id`,`allow_contribute`),
  KEY `site_id` (`site_id`,`parent_id`,`hidden`,`disabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_category_attribute`
--

DROP TABLE IF EXISTS `cms_category_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_category_attribute` (
  `category_id` int(11) NOT NULL COMMENT '分类ID',
  `title` varchar(80) DEFAULT NULL COMMENT '标题',
  `keywords` varchar(100) DEFAULT NULL COMMENT '关键词',
  `description` varchar(300) DEFAULT NULL COMMENT '描述',
  `data` longtext COMMENT '数据JSON',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_category_model`
--

DROP TABLE IF EXISTS `cms_category_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_category_model` (
  `category_id` int(11) NOT NULL COMMENT '分类ID',
  `model_id` varchar(20) NOT NULL COMMENT '模型编码',
  `template_path` varchar(200) DEFAULT NULL COMMENT '内容模板路径',
  PRIMARY KEY (`category_id`,`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类模型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_category_type`
--

DROP TABLE IF EXISTS `cms_category_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_category_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `sort` int(11) NOT NULL COMMENT '排序',
  `extend_id` int(11) DEFAULT NULL COMMENT '扩展ID',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_comment`
--

DROP TABLE IF EXISTS `cms_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `reply_id` bigint(20) DEFAULT NULL COMMENT '回复ID',
  `reply_user_id` bigint(20) DEFAULT NULL COMMENT '回复用户ID',
  `content_id` bigint(20) NOT NULL COMMENT '文章内容',
  `check_user_id` bigint(20) DEFAULT NULL COMMENT '审核用户',
  `check_date` datetime DEFAULT NULL COMMENT '审核日期',
  `update_date` datetime DEFAULT NULL COMMENT '更新日期',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `status` int(11) NOT NULL COMMENT '状态：1、已发布 2、待审核',
  `disabled` tinyint(1) NOT NULL COMMENT '已禁用',
  `text` text COMMENT '内容',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`,`content_id`,`status`,`disabled`),
  KEY `update_date` (`update_date`,`create_date`),
  KEY `reply_id` (`site_id`,`reply_user_id`,`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_content`
--

DROP TABLE IF EXISTS `cms_content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_content` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `user_id` bigint(20) NOT NULL COMMENT '发表用户',
  `check_user_id` bigint(20) DEFAULT NULL COMMENT '审核用户',
  `category_id` int(11) NOT NULL COMMENT '分类',
  `model_id` varchar(20) NOT NULL COMMENT '模型',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父内容ID',
  `quote_content_id` bigint(20) DEFAULT NULL COMMENT '引用内容ID',
  `copied` tinyint(1) NOT NULL COMMENT '是否转载',
  `author` varchar(50) DEFAULT NULL COMMENT '作者',
  `editor` varchar(50) DEFAULT NULL COMMENT '编辑',
  `only_url` tinyint(1) NOT NULL COMMENT '外链',
  `has_images` tinyint(1) NOT NULL COMMENT '拥有图片列表',
  `has_files` tinyint(1) NOT NULL COMMENT '拥有附件列表',
  `has_static` tinyint(1) NOT NULL COMMENT '已经静态化',
  `url` varchar(1000) DEFAULT NULL COMMENT '地址',
  `description` varchar(300) DEFAULT NULL COMMENT '简介',
  `tag_ids` text COMMENT '标签',
  `dictionar_values` text COMMENT '数据字典值',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面',
  `childs` int(11) NOT NULL COMMENT '子内容数',
  `scores` int(11) NOT NULL COMMENT '分数',
  `comments` int(11) NOT NULL COMMENT '评论数',
  `clicks` int(11) NOT NULL COMMENT '点击数',
  `publish_date` datetime NOT NULL COMMENT '发布日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '过期日期',
  `check_date` datetime DEFAULT NULL COMMENT '审核日期',
  `update_date` datetime DEFAULT NULL COMMENT '更新日期',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int(11) NOT NULL COMMENT '状态：0、草稿 1、已发布 2、待审核',
  `disabled` tinyint(1) NOT NULL COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `check_date` (`check_date`,`update_date`),
  KEY `scores` (`scores`,`comments`,`clicks`),
  KEY `only_url` (`only_url`,`has_images`,`has_files`,`user_id`),
  KEY `status` (`site_id`,`status`,`category_id`,`disabled`,`model_id`,`parent_id`,`sort`,`publish_date`,`expiry_date`),
  KEY `quote_content_id` (`site_id`,`quote_content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_content_attribute`
--

DROP TABLE IF EXISTS `cms_content_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_content_attribute` (
  `content_id` bigint(20) NOT NULL,
  `source` varchar(50) DEFAULT NULL COMMENT '内容来源',
  `source_url` varchar(1000) DEFAULT NULL COMMENT '来源地址',
  `data` longtext COMMENT '数据JSON',
  `search_text` longtext COMMENT '全文索引文本',
  `text` longtext COMMENT '内容',
  `word_count` int(11) NOT NULL COMMENT '字数',
  PRIMARY KEY (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_content_file`
--

DROP TABLE IF EXISTS `cms_content_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_content_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_id` bigint(20) NOT NULL COMMENT '内容',
  `user_id` bigint(20) NOT NULL COMMENT '用户',
  `file_path` varchar(255) NOT NULL COMMENT '文件路径',
  `file_type` varchar(20) NOT NULL COMMENT '文件类型',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小',
  `clicks` int(11) NOT NULL COMMENT '点击数',
  `sort` int(11) NOT NULL COMMENT '排序',
  `description` varchar(300) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  KEY `content_id` (`content_id`),
  KEY `sort` (`sort`),
  KEY `file_type` (`file_type`),
  KEY `file_size` (`file_size`),
  KEY `clicks` (`clicks`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容附件';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_content_related`
--

DROP TABLE IF EXISTS `cms_content_related`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_content_related` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content_id` bigint(20) NOT NULL COMMENT '内容',
  `related_content_id` bigint(20) DEFAULT NULL COMMENT '推荐内容',
  `user_id` bigint(20) NOT NULL COMMENT '推荐用户',
  `url` varchar(1000) DEFAULT NULL COMMENT '推荐链接地址',
  `title` varchar(255) DEFAULT NULL COMMENT '推荐标题',
  `description` varchar(300) DEFAULT NULL COMMENT '推荐简介',
  `clicks` int(11) NOT NULL COMMENT '点击数',
  `sort` int(11) NOT NULL COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `user_id` (`content_id`,`related_content_id`,`user_id`,`clicks`,`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐推荐';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_dictionary`
--

DROP TABLE IF EXISTS `cms_dictionary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_dictionary` (
  `id` varchar(20) NOT NULL,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `multiple` tinyint(1) NOT NULL COMMENT '允许多选',
  PRIMARY KEY (`id`,`site_id`),
  KEY `site_id` (`site_id`,`multiple`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_dictionary_data`
--

DROP TABLE IF EXISTS `cms_dictionary_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_dictionary_data` (
  `dictionary_id` varchar(20) NOT NULL COMMENT '字典',
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `value` varchar(50) NOT NULL COMMENT '值',
  `text` varchar(100) NOT NULL COMMENT '文字',
  PRIMARY KEY (`dictionary_id`,`site_id`,`value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_place`
--

DROP TABLE IF EXISTS `cms_place`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_place` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `path` varchar(100) NOT NULL COMMENT '模板路径',
  `user_id` bigint(20) DEFAULT NULL COMMENT '提交用户',
  `check_user_id` bigint(20) DEFAULT NULL COMMENT '审核用户',
  `item_type` varchar(50) DEFAULT NULL COMMENT '推荐项目类型',
  `item_id` bigint(20) DEFAULT NULL COMMENT '推荐项目ID',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `url` varchar(1000) DEFAULT NULL COMMENT '超链接',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `publish_date` datetime NOT NULL COMMENT '发布日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '过期日期',
  `status` int(11) NOT NULL COMMENT '状态：0、前台提交 1、已发布 ',
  `clicks` int(11) NOT NULL COMMENT '点击数',
  `disabled` tinyint(1) NOT NULL COMMENT '已禁用',
  PRIMARY KEY (`id`),
  KEY `clicks` (`clicks`),
  KEY `site_id` (`site_id`,`path`,`status`,`disabled`),
  KEY `item_type` (`item_type`,`item_id`),
  KEY `user_id` (`user_id`,`check_user_id`),
  KEY `publish_date` (`publish_date`,`create_date`,`expiry_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_place_attribute`
--

DROP TABLE IF EXISTS `cms_place_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_place_attribute` (
  `place_id` bigint(20) NOT NULL COMMENT '位置ID',
  `data` longtext COMMENT '数据JSON',
  PRIMARY KEY (`place_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐位数据扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_tag`
--

DROP TABLE IF EXISTS `cms_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_tag` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `type_id` int(11) DEFAULT NULL COMMENT '分类ID',
  `search_count` int(11) NOT NULL COMMENT '搜索次数',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_tag_type`
--

DROP TABLE IF EXISTS `cms_tag_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_tag_type` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `count` int(11) NOT NULL COMMENT '标签数',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签类型';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `cms_word`
--

DROP TABLE IF EXISTS `cms_word`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_word` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点',
  `name` varchar(100) NOT NULL COMMENT '名称',
  `search_count` int(11) NOT NULL COMMENT '搜索次数',
  `hidden` tinyint(1) NOT NULL COMMENT '隐藏',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`,`site_id`),
  KEY `hidden` (`hidden`),
  KEY `create_date` (`create_date`),
  KEY `search_count` (`search_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索词';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_login`
--

DROP TABLE IF EXISTS `log_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_login` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '用户名',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `ip` varchar(64) NOT NULL COMMENT 'IP',
  `channel` varchar(50) NOT NULL COMMENT '登录渠道',
  `result` tinyint(1) NOT NULL COMMENT '结果',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `error_password` varchar(100) DEFAULT NULL COMMENT '错误密码',
  PRIMARY KEY (`id`),
  KEY `result` (`result`),
  KEY `user_id` (`user_id`),
  KEY `create_date` (`create_date`),
  KEY `ip` (`ip`),
  KEY `site_id` (`site_id`),
  KEY `channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_operate`
--

DROP TABLE IF EXISTS `log_operate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_operate` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `channel` varchar(50) NOT NULL COMMENT '操作渠道',
  `operate` varchar(40) NOT NULL COMMENT '操作',
  `ip` varchar(64) DEFAULT NULL COMMENT 'IP',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `content` text NOT NULL COMMENT '内容',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `operate` (`operate`),
  KEY `create_date` (`create_date`),
  KEY `ip` (`ip`),
  KEY `site_id` (`site_id`),
  KEY `channel` (`channel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_task`
--

DROP TABLE IF EXISTS `log_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `task_id` int(11) NOT NULL COMMENT '任务',
  `begintime` datetime NOT NULL COMMENT '开始时间',
  `endtime` datetime DEFAULT NULL COMMENT '结束时间',
  `success` tinyint(1) NOT NULL COMMENT '执行成功',
  `result` longtext COMMENT '执行结果',
  PRIMARY KEY (`id`),
  KEY `task_id` (`task_id`),
  KEY `success` (`success`),
  KEY `site_id` (`site_id`),
  KEY `begintime` (`begintime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务计划日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `log_upload`
--

DROP TABLE IF EXISTS `log_upload`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `log_upload` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `channel` varchar(50) NOT NULL COMMENT '操作渠道',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原文件名',
  `file_type` varchar(20) NOT NULL COMMENT '文件类型',
  `file_size` bigint(20) NOT NULL COMMENT '文件大小',
  `ip` varchar(64) DEFAULT NULL COMMENT 'IP',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `file_path` varchar(500) NOT NULL COMMENT '文件路径',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `create_date` (`create_date`),
  KEY `ip` (`ip`),
  KEY `site_id` (`site_id`),
  KEY `channel` (`channel`),
  KEY `file_type` (`file_type`),
  KEY `file_size` (`file_size`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_app`
--

DROP TABLE IF EXISTS `sys_app`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_app` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `channel` varchar(50) NOT NULL COMMENT '渠道',
  `app_key` varchar(50) NOT NULL COMMENT 'APP key',
  `app_secret` varchar(50) NOT NULL COMMENT 'APP secret',
  `authorized_apis` text COMMENT '授权API',
  `expiry_minutes` int(11) DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key` (`app_key`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_app_client`
--

DROP TABLE IF EXISTS `sys_app_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_app_client` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `channel` varchar(20) NOT NULL COMMENT '渠道',
  `uuid` varchar(50) NOT NULL COMMENT '唯一标识',
  `user_id` bigint(20) DEFAULT NULL COMMENT '绑定用户',
  `client_version` varchar(50) DEFAULT NULL COMMENT '版本',
  `last_login_date` datetime DEFAULT NULL COMMENT '上次登录时间',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '上次登录IP',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `disabled` tinyint(1) NOT NULL COMMENT '是否禁用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `site_id` (`site_id`,`channel`,`uuid`),
  KEY `user_id` (`user_id`,`disabled`,`create_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用客户端';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_app_token`
--

DROP TABLE IF EXISTS `sys_app_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_app_token` (
  `auth_token` varchar(40) NOT NULL COMMENT '授权验证',
  `app_id` int(11) NOT NULL COMMENT '应用ID',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '过期日期',
  PRIMARY KEY (`auth_token`),
  KEY `app_id` (`app_id`),
  KEY `create_date` (`create_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用授权';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_cluster`
--

DROP TABLE IF EXISTS `sys_cluster`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_cluster` (
  `uuid` varchar(40) NOT NULL COMMENT 'uuid',
  `create_date` datetime NOT NULL COMMENT '创建时间',
  `heartbeat_date` datetime NOT NULL COMMENT '心跳时间',
  `master` tinyint(1) NOT NULL COMMENT '是否管理',
  `cms_version` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`uuid`),
  KEY `create_date` (`create_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务器集群';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_config_data`
--

DROP TABLE IF EXISTS `sys_config_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config_data` (
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `code` varchar(50) NOT NULL COMMENT '配置项编码',
  `data` longtext NOT NULL COMMENT '值',
  PRIMARY KEY (`site_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept`
--

DROP TABLE IF EXISTS `sys_dept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `parent_id` int(11) DEFAULT NULL COMMENT '父部门ID',
  `description` varchar(300) DEFAULT NULL COMMENT '描述',
  `user_id` bigint(20) DEFAULT NULL COMMENT '负责人',
  `max_sort` int(11) NOT NULL DEFAULT '1000' COMMENT '最大内容置顶级别',
  `owns_all_category` tinyint(1) NOT NULL COMMENT '拥有全部分类权限',
  `owns_all_page` tinyint(1) NOT NULL COMMENT '拥有全部页面权限',
  `owns_all_config` tinyint(1) NOT NULL DEFAULT '1' COMMENT '拥有全部配置权限',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept_category`
--

DROP TABLE IF EXISTS `sys_dept_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept_category` (
  `dept_id` int(11) NOT NULL COMMENT '部门ID',
  `category_id` int(11) NOT NULL COMMENT '分类ID',
  PRIMARY KEY (`dept_id`,`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门分类';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept_config`
--

DROP TABLE IF EXISTS `sys_dept_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept_config` (
  `dept_id` int(11) NOT NULL COMMENT '部门ID',
  `config` varchar(100) NOT NULL COMMENT '配置',
  PRIMARY KEY (`dept_id`,`config`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_dept_page`
--

DROP TABLE IF EXISTS `sys_dept_page`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dept_page` (
  `dept_id` int(11) NOT NULL COMMENT '部门ID',
  `page` varchar(100) NOT NULL COMMENT '页面',
  PRIMARY KEY (`dept_id`,`page`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门页面';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_domain`
--

DROP TABLE IF EXISTS `sys_domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_domain` (
  `name` varchar(100) NOT NULL COMMENT '域名',
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `wild` tinyint(1) NOT NULL COMMENT '通配域名',
  `path` varchar(100) DEFAULT NULL COMMENT '路径',
  PRIMARY KEY (`name`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域名';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_email_token`
--

DROP TABLE IF EXISTS `sys_email_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_email_token` (
  `auth_token` varchar(40) NOT NULL COMMENT '验证码',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `email` varchar(100) NOT NULL COMMENT '邮件地址',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `expiry_date` datetime NOT NULL COMMENT '过期日期',
  PRIMARY KEY (`auth_token`),
  KEY `create_date` (`create_date`),
  KEY `user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件地址验证日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_extend`
--

DROP TABLE IF EXISTS `sys_extend`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_extend` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `item_type` varchar(20) NOT NULL COMMENT '扩展类型',
  `item_id` int(11) NOT NULL COMMENT '扩展项目ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扩展';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_extend_field`
--

DROP TABLE IF EXISTS `sys_extend_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_extend_field` (
  `extend_id` int(11) NOT NULL COMMENT '扩展ID',
  `code` varchar(20) NOT NULL COMMENT '编码',
  `required` tinyint(1) NOT NULL COMMENT '是否必填',
  `searchable` tinyint(1) NOT NULL COMMENT '是否可搜索',
  `maxlength` int(11) DEFAULT NULL COMMENT '最大长度',
  `name` varchar(20) NOT NULL COMMENT '名称',
  `description` varchar(100) DEFAULT NULL COMMENT '解释',
  `input_type` varchar(20) NOT NULL COMMENT '表单类型',
  `default_value` varchar(50) DEFAULT NULL COMMENT '默认值',
  `dictionary_id` varchar(20) DEFAULT NULL COMMENT '数据字典ID',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '顺序',
  PRIMARY KEY (`extend_id`,`code`),
  KEY `sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扩展字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_module`
--

DROP TABLE IF EXISTS `sys_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_module` (
  `id` varchar(30) NOT NULL,
  `url` varchar(255) DEFAULT NULL COMMENT '链接地址',
  `authorized_url` text COMMENT '授权地址',
  `attached` varchar(50) DEFAULT NULL COMMENT '标题附加',
  `parent_id` varchar(30) DEFAULT NULL COMMENT '父模块',
  `menu` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否菜单',
  `sort` int(11) NOT NULL COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `parent_id` (`parent_id`,`menu`),
  KEY `sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_module_lang`
--

DROP TABLE IF EXISTS `sys_module_lang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_module_lang` (
  `module_id` varchar(30) NOT NULL COMMENT '模块ID',
  `lang` varchar(20) NOT NULL COMMENT '语言',
  `value` varchar(100) DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`module_id`,`lang`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块语言';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `owns_all_right` tinyint(1) NOT NULL COMMENT '拥有全部权限',
  `show_all_module` tinyint(1) NOT NULL COMMENT '显示全部模块',
  PRIMARY KEY (`id`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_authorized`
--

DROP TABLE IF EXISTS `sys_role_authorized`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_authorized` (
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  `url` varchar(100) NOT NULL COMMENT '授权地址',
  PRIMARY KEY (`role_id`,`url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色授权地址';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_module`
--

DROP TABLE IF EXISTS `sys_role_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_module` (
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  `module_id` varchar(30) NOT NULL COMMENT '模块ID',
  PRIMARY KEY (`role_id`,`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色授权模块';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_role_user`
--

DROP TABLE IF EXISTS `sys_role_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_user` (
  `role_id` int(11) NOT NULL COMMENT '角色ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  PRIMARY KEY (`role_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_site`
--

DROP TABLE IF EXISTS `sys_site`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_site` (
  `id` smallint(6) NOT NULL AUTO_INCREMENT,
  `parent_id` smallint(6) DEFAULT NULL COMMENT '父站点ID',
  `name` varchar(50) NOT NULL COMMENT '站点名',
  `use_static` tinyint(1) NOT NULL COMMENT '启用静态化',
  `site_path` varchar(255) NOT NULL COMMENT '站点地址',
  `use_ssi` tinyint(1) NOT NULL COMMENT '启用服务器端包含',
  `dynamic_path` varchar(255) NOT NULL COMMENT '动态站点地址',
  `disabled` tinyint(1) NOT NULL COMMENT '禁用',
  PRIMARY KEY (`id`),
  KEY `disabled` (`disabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_task`
--

DROP TABLE IF EXISTS `sys_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_task` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `status` int(11) NOT NULL COMMENT '状态',
  `cron_expression` varchar(50) NOT NULL COMMENT '表达式',
  `description` varchar(300) DEFAULT NULL COMMENT '描述',
  `file_path` varchar(255) DEFAULT NULL COMMENT '文件路径',
  `update_date` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `status` (`status`),
  KEY `site_id` (`site_id`),
  KEY `update_date` (`update_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务计划';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `name` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(128) NOT NULL COMMENT '密码',
  `salt` varchar(20) DEFAULT NULL COMMENT '混淆码,为空时则密码为md5,为10位时sha512(sha512(password)+salt)',
  `weak_password` tinyint(1) NOT NULL DEFAULT '0' COMMENT '弱密码',
  `nick_name` varchar(45) NOT NULL COMMENT '昵称',
  `dept_id` int(11) DEFAULT NULL COMMENT '部门',
  `owns_all_content` tinyint(1) NOT NULL DEFAULT '1' COMMENT '拥有所有内容权限',
  `roles` text COMMENT '角色',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱地址',
  `email_checked` tinyint(1) NOT NULL COMMENT '已验证邮箱',
  `superuser_access` tinyint(1) NOT NULL COMMENT '是否管理员',
  `disabled` tinyint(1) NOT NULL COMMENT '是否禁用',
  `last_login_date` datetime DEFAULT NULL COMMENT '最后登录日期',
  `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录ip',
  `login_count` int(11) NOT NULL COMMENT '登录次数',
  `registered_date` datetime DEFAULT NULL COMMENT '注册日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`,`site_id`),
#   UNIQUE KEY `nick_name` (`nick_name`,`site_id`),
  KEY `nick_name` (`nick_name`,`site_id`),
  KEY `email` (`email`),
  KEY `disabled` (`disabled`),
  KEY `lastLoginDate` (`last_login_date`),
  KEY `email_checked` (`email_checked`),
  KEY `dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sys_user_token`
--

DROP TABLE IF EXISTS `sys_user_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_token` (
  `auth_token` varchar(40) NOT NULL COMMENT '登录授权',
  `site_id` smallint(6) NOT NULL COMMENT '站点ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `channel` varchar(50) NOT NULL COMMENT '渠道',
  `create_date` datetime NOT NULL COMMENT '创建日期',
  `expiry_date` datetime DEFAULT NULL COMMENT '过期日期',
  `login_ip` varchar(64) NOT NULL COMMENT '登录IP',
  PRIMARY KEY (`auth_token`),
  KEY `user_id` (`user_id`),
  KEY `create_date` (`create_date`),
  KEY `channel` (`channel`),
  KEY `site_id` (`site_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户令牌';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-02-12 15:52:39

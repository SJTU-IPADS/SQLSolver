-- MySQL dump 10.13  Distrib 5.7.29, for Linux (x86_64)
--
-- Host: 10.0.0.102    Database: shopizer_opt
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
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category` (
  `CATEGORY_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CATEGORY_IMAGE` varchar(100) DEFAULT NULL,
  `CATEGORY_STATUS` bit(1) DEFAULT NULL,
  `CODE` varchar(100) NOT NULL,
  `DEPTH` int(11) DEFAULT NULL,
  `FEATURED` bit(1) DEFAULT NULL,
  `LINEAGE` varchar(255) DEFAULT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `VISIBLE` bit(1) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  `PARENT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CATEGORY_ID`),
  UNIQUE KEY `UK3mq9i6qmgquvoieslx39pej6x` (`MERCHANT_ID`,`CODE`),
  KEY `FKn3kekntr7pm8g9v8ask698ato` (`PARENT_ID`),
  CONSTRAINT `FK8a09asq5fcx0a88i4m8nsixy` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKn3kekntr7pm8g9v8ask698ato` FOREIGN KEY (`PARENT_ID`) REFERENCES `category` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category_description`
--

DROP TABLE IF EXISTS `category_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `CATEGORY_HIGHLIGHT` varchar(255) DEFAULT NULL,
  `META_DESCRIPTION` varchar(255) DEFAULT NULL,
  `META_KEYWORDS` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(120) DEFAULT NULL,
  `SEF_URL` varchar(120) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `CATEGORY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKbuesqq6cyx7e5hy3mf30cfieq` (`CATEGORY_ID`,`LANGUAGE_ID`),
  KEY `FKl4j5boteutpu1p8f67kydpnmd` (`LANGUAGE_ID`),
  CONSTRAINT `FKa58u7d0ydfgref1iaux5efyov` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `category` (`CATEGORY_ID`),
  CONSTRAINT `FKl4j5boteutpu1p8f67kydpnmd` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `content`
--

DROP TABLE IF EXISTS `content`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content` (
  `CONTENT_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CODE` varchar(100) NOT NULL,
  `CONTENT_POSITION` varchar(10) DEFAULT NULL,
  `CONTENT_TYPE` varchar(10) DEFAULT NULL,
  `LINK_TO_MENU` bit(1) DEFAULT NULL,
  `PRODUCT_GROUP` varchar(255) DEFAULT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `VISIBLE` bit(1) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`CONTENT_ID`),
  UNIQUE KEY `UKt1v2ld0mrwviquqourql4uub0` (`MERCHANT_ID`,`CODE`),
  CONSTRAINT `FKfmoi0fkjbtfty3o8fs94t11r1` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `content_description`
--

DROP TABLE IF EXISTS `content_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `content_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `META_DESCRIPTION` varchar(255) DEFAULT NULL,
  `META_KEYWORDS` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(255) DEFAULT NULL,
  `SEF_URL` varchar(120) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `CONTENT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKn0w5r7ctbp88r4rvk7ayklofm` (`CONTENT_ID`,`LANGUAGE_ID`),
  KEY `FK47yxf681u0rfw2kvarhqb0r3v` (`LANGUAGE_ID`),
  CONSTRAINT `FK47yxf681u0rfw2kvarhqb0r3v` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKk7fabfxn2flvcofwwpyg5sys` FOREIGN KEY (`CONTENT_ID`) REFERENCES `content` (`CONTENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `country`
--

DROP TABLE IF EXISTS `country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country` (
  `COUNTRY_ID` int(11) NOT NULL,
  `COUNTRY_ISOCODE` varchar(255) NOT NULL,
  `COUNTRY_SUPPORTED` bit(1) DEFAULT NULL,
  `GEOZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`COUNTRY_ID`),
  UNIQUE KEY `UK_dqb99v22pt27v0tgeqo958e6x` (`COUNTRY_ISOCODE`),
  KEY `FKd2q9e14kh1j6tm1gpbct2xwws` (`GEOZONE_ID`),
  CONSTRAINT `FKd2q9e14kh1j6tm1gpbct2xwws` FOREIGN KEY (`GEOZONE_ID`) REFERENCES `geozone` (`GEOZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `country_description`
--

DROP TABLE IF EXISTS `country_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `country_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `COUNTRY_ID` int(11) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKt7nshki1rbp6157ed0v6cx4y4` (`COUNTRY_ID`,`LANGUAGE_ID`),
  KEY `FKersrbjot9p9nfukxfd2l27c7t` (`LANGUAGE_ID`),
  CONSTRAINT `FKersrbjot9p9nfukxfd2l27c7t` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKkd2sy7q97wr2ahvyiiqc4txji` FOREIGN KEY (`COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `currency`
--

DROP TABLE IF EXISTS `currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `currency` (
  `CURRENCY_ID` bigint(20) NOT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  `CURRENCY_CURRENCY_CODE` varchar(255) NOT NULL,
  `CURRENCY_NAME` varchar(255) DEFAULT NULL,
  `CURRENCY_SUPPORTED` bit(1) DEFAULT NULL,
  PRIMARY KEY (`CURRENCY_ID`),
  UNIQUE KEY `UK_m7ku15ekud52vp67ry73a36te` (`CURRENCY_CURRENCY_CODE`),
  UNIQUE KEY `UK_1ubr7n96hjajamtggqp090a4x` (`CURRENCY_CODE`),
  UNIQUE KEY `UK_7r1k69cbk5giewqr5c9r4v6f` (`CURRENCY_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer`
--

DROP TABLE IF EXISTS `customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer` (
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `CUSTOMER_ANONYMOUS` bit(1) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `BILLING_STREET_ADDRESS` varchar(256) DEFAULT NULL,
  `BILLING_CITY` varchar(100) DEFAULT NULL,
  `BILLING_COMPANY` varchar(100) DEFAULT NULL,
  `BILLING_FIRST_NAME` varchar(64) NOT NULL,
  `BILLING_LAST_NAME` varchar(64) NOT NULL,
  `LATITUDE` varchar(100) DEFAULT NULL,
  `LONGITUDE` varchar(100) DEFAULT NULL,
  `BILLING_POSTCODE` varchar(20) DEFAULT NULL,
  `BILLING_STATE` varchar(100) DEFAULT NULL,
  `BILLING_TELEPHONE` varchar(32) DEFAULT NULL,
  `CUSTOMER_COMPANY` varchar(100) DEFAULT NULL,
  `REVIEW_AVG` decimal(19,2) DEFAULT NULL,
  `REVIEW_COUNT` int(11) DEFAULT NULL,
  `CUSTOMER_DOB` datetime DEFAULT NULL,
  `DELIVERY_STREET_ADDRESS` varchar(256) DEFAULT NULL,
  `DELIVERY_CITY` varchar(100) DEFAULT NULL,
  `DELIVERY_COMPANY` varchar(100) DEFAULT NULL,
  `DELIVERY_FIRST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_LAST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_POSTCODE` varchar(20) DEFAULT NULL,
  `DELIVERY_STATE` varchar(100) DEFAULT NULL,
  `DELIVERY_TELEPHONE` varchar(32) DEFAULT NULL,
  `CUSTOMER_EMAIL_ADDRESS` varchar(96) NOT NULL,
  `CUSTOMER_GENDER` varchar(1) DEFAULT NULL,
  `CUSTOMER_NICK` varchar(96) DEFAULT NULL,
  `CUSTOMER_PASSWORD` varchar(60) DEFAULT NULL,
  `PROVIDER` varchar(255) DEFAULT NULL,
  `BILLING_COUNTRY_ID` int(11) NOT NULL,
  `BILLING_ZONE_ID` bigint(20) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `DELIVERY_COUNTRY_ID` int(11) DEFAULT NULL,
  `DELIVERY_ZONE_ID` bigint(20) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`CUSTOMER_ID`),
  KEY `FK5pas8t9mknk4kkin55t4v300l` (`BILLING_COUNTRY_ID`),
  KEY `FKp0xcpa3i2mgdr0kq43xiibx40` (`BILLING_ZONE_ID`),
  KEY `FKdgjqmj04qt89gmfloo4ofojcw` (`LANGUAGE_ID`),
  KEY `FKbxyooiceli2ko29bupdye6jgn` (`DELIVERY_COUNTRY_ID`),
  KEY `FK3k21jw28bbx043c2mnhevg9w4` (`DELIVERY_ZONE_ID`),
  KEY `FK8122nrpakxu3umk1od4v0xxoa` (`MERCHANT_ID`),
  CONSTRAINT `FK3k21jw28bbx043c2mnhevg9w4` FOREIGN KEY (`DELIVERY_ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FK5pas8t9mknk4kkin55t4v300l` FOREIGN KEY (`BILLING_COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FK8122nrpakxu3umk1od4v0xxoa` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKbxyooiceli2ko29bupdye6jgn` FOREIGN KEY (`DELIVERY_COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FKdgjqmj04qt89gmfloo4ofojcw` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKp0xcpa3i2mgdr0kq43xiibx40` FOREIGN KEY (`BILLING_ZONE_ID`) REFERENCES `zone` (`ZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_attribute`
--

DROP TABLE IF EXISTS `customer_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_attribute` (
  `CUSTOMER_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `CUSTOMER_ATTR_TXT_VAL` varchar(255) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `OPTION_ID` bigint(20) NOT NULL,
  `OPTION_VALUE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_ATTRIBUTE_ID`),
  UNIQUE KEY `UK46kbpre88yh963gewm3kmdni1` (`OPTION_ID`,`CUSTOMER_ID`),
  KEY `FKc3318o13i2bpxkci1bh52we5a` (`CUSTOMER_ID`),
  KEY `FK9fl7iexvdeeeoch9fh35o5vw4` (`OPTION_VALUE_ID`),
  CONSTRAINT `FK4xugs9yd9w4o3sw11fisb8tj5` FOREIGN KEY (`OPTION_ID`) REFERENCES `customer_option` (`CUSTOMER_OPTION_ID`),
  CONSTRAINT `FK9fl7iexvdeeeoch9fh35o5vw4` FOREIGN KEY (`OPTION_VALUE_ID`) REFERENCES `customer_option_value` (`CUSTOMER_OPTION_VALUE_ID`),
  CONSTRAINT `FKc3318o13i2bpxkci1bh52we5a` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_group`
--

DROP TABLE IF EXISTS `customer_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_group` (
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `GROUP_ID` int(11) NOT NULL,
  KEY `FKgrr5v89l1m9sl2qol62bbctq4` (`GROUP_ID`),
  KEY `FK257h3e27f4ujw08doqtq46hho` (`CUSTOMER_ID`),
  CONSTRAINT `FK257h3e27f4ujw08doqtq46hho` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKgrr5v89l1m9sl2qol62bbctq4` FOREIGN KEY (`GROUP_ID`) REFERENCES `sm_group` (`GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_opt_val_description`
--

DROP TABLE IF EXISTS `customer_opt_val_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_opt_val_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `CUSTOMER_OPT_VAL_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKge7f2t1d31r87wnk09h9u1tnv` (`CUSTOMER_OPT_VAL_ID`,`LANGUAGE_ID`),
  KEY `FK6rfssi3qfx4pswicxrfb18c1` (`LANGUAGE_ID`),
  CONSTRAINT `FK6rfssi3qfx4pswicxrfb18c1` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKhwrs6fyqk6vh11yvcflu42yef` FOREIGN KEY (`CUSTOMER_OPT_VAL_ID`) REFERENCES `customer_option_value` (`CUSTOMER_OPTION_VALUE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_optin`
--

DROP TABLE IF EXISTS `customer_optin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_optin` (
  `CUSTOMER_OPTIN_ID` bigint(20) NOT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `FIRST` varchar(255) DEFAULT NULL,
  `LAST` varchar(255) DEFAULT NULL,
  `OPTIN_DATE` datetime DEFAULT NULL,
  `VALUE` longtext,
  `MERCHANT_ID` int(11) NOT NULL,
  `OPTIN_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CUSTOMER_OPTIN_ID`),
  UNIQUE KEY `UKc4fnyu0pvxxtrbko10rm1jqyw` (`EMAIL`,`OPTIN_ID`),
  KEY `FKk5v94dvhsgibaw89hv4m8o5yw` (`MERCHANT_ID`),
  KEY `FK7qym878m07cwvs4foe68lvqjt` (`OPTIN_ID`),
  CONSTRAINT `FK7qym878m07cwvs4foe68lvqjt` FOREIGN KEY (`OPTIN_ID`) REFERENCES `optin` (`OPTIN_ID`),
  CONSTRAINT `FKk5v94dvhsgibaw89hv4m8o5yw` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_option`
--

DROP TABLE IF EXISTS `customer_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_option` (
  `CUSTOMER_OPTION_ID` bigint(20) NOT NULL,
  `CUSTOMER_OPT_ACTIVE` bit(1) DEFAULT NULL,
  `CUSTOMER_OPT_CODE` varchar(255) NOT NULL,
  `CUSTOMER_OPTION_TYPE` varchar(10) DEFAULT NULL,
  `CUSTOMER_OPT_PUBLIC` bit(1) DEFAULT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`CUSTOMER_OPTION_ID`),
  UNIQUE KEY `UKrov34a6g4dhhiqukvhp1ggm0u` (`MERCHANT_ID`,`CUSTOMER_OPT_CODE`),
  KEY `CUST_OPT_CODE_IDX` (`CUSTOMER_OPT_CODE`),
  CONSTRAINT `FKcmqnh0rn2hukdfowean5tdy8k` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_option_desc`
--

DROP TABLE IF EXISTS `customer_option_desc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_option_desc` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `CUSTOMER_OPTION_COMMENT` longtext,
  `LANGUAGE_ID` int(11) NOT NULL,
  `CUSTOMER_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UK6ovl4t1ciag1wubtcebaoo7vi` (`CUSTOMER_OPTION_ID`,`LANGUAGE_ID`),
  KEY `FKm4iu7v9db17wk2a03xqbqdlfa` (`LANGUAGE_ID`),
  CONSTRAINT `FKc2yiucjbw0wjha8ww7a01qfeo` FOREIGN KEY (`CUSTOMER_OPTION_ID`) REFERENCES `customer_option` (`CUSTOMER_OPTION_ID`),
  CONSTRAINT `FKm4iu7v9db17wk2a03xqbqdlfa` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_option_set`
--

DROP TABLE IF EXISTS `customer_option_set`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_option_set` (
  `CUSTOMER_OPTIONSET_ID` bigint(20) NOT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `CUSTOMER_OPTION_ID` bigint(20) NOT NULL,
  `CUSTOMER_OPTION_VALUE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_OPTIONSET_ID`),
  UNIQUE KEY `UK4peli2ritnnq2xqpyq188srm6` (`CUSTOMER_OPTION_ID`,`CUSTOMER_OPTION_VALUE_ID`),
  KEY `FKj9vnvyh6hhhftjbcsymgiodm9` (`CUSTOMER_OPTION_VALUE_ID`),
  CONSTRAINT `FK1y5qtsuabhpwft3dyhqrgmtb4` FOREIGN KEY (`CUSTOMER_OPTION_ID`) REFERENCES `customer_option` (`CUSTOMER_OPTION_ID`),
  CONSTRAINT `FKj9vnvyh6hhhftjbcsymgiodm9` FOREIGN KEY (`CUSTOMER_OPTION_VALUE_ID`) REFERENCES `customer_option_value` (`CUSTOMER_OPTION_VALUE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_option_value`
--

DROP TABLE IF EXISTS `customer_option_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_option_value` (
  `CUSTOMER_OPTION_VALUE_ID` bigint(20) NOT NULL,
  `CUSTOMER_OPT_VAL_CODE` varchar(255) NOT NULL,
  `CUSTOMER_OPT_VAL_IMAGE` varchar(255) DEFAULT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`CUSTOMER_OPTION_VALUE_ID`),
  UNIQUE KEY `UKcb1fmv71nrx7m1rlx1ff5qvdt` (`MERCHANT_ID`,`CUSTOMER_OPT_VAL_CODE`),
  KEY `CUST_OPT_VAL_CODE_IDX` (`CUSTOMER_OPT_VAL_CODE`),
  CONSTRAINT `FKho87ssg5rnvwauj3y690a96g6` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_review`
--

DROP TABLE IF EXISTS `customer_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_review` (
  `CUSTOMER_REVIEW_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `REVIEW_DATE` datetime DEFAULT NULL,
  `REVIEWS_RATING` double DEFAULT NULL,
  `REVIEWS_READ` bigint(20) DEFAULT NULL,
  `STATUS` int(11) DEFAULT NULL,
  `CUSTOMERS_ID` bigint(20) DEFAULT NULL,
  `REVIEWED_CUSTOMER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CUSTOMER_REVIEW_ID`),
  UNIQUE KEY `UK2momthbfrtgico2yyod8w18pk` (`CUSTOMERS_ID`,`REVIEWED_CUSTOMER_ID`),
  KEY `FK7pmqdk9od2af7cl6alx82fkek` (`REVIEWED_CUSTOMER_ID`),
  CONSTRAINT `FK7pmqdk9od2af7cl6alx82fkek` FOREIGN KEY (`REVIEWED_CUSTOMER_ID`) REFERENCES `customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKayt6tbxp7d4g1qyg8crw2n73p` FOREIGN KEY (`CUSTOMERS_ID`) REFERENCES `customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `customer_review_description`
--

DROP TABLE IF EXISTS `customer_review_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `customer_review_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `CUSTOMER_REVIEW_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UK1va9q0nhoe3wli25ktpmouvyh` (`CUSTOMER_REVIEW_ID`,`LANGUAGE_ID`),
  KEY `FK5pkgrlk32uqaxkrbve5mws1hj` (`LANGUAGE_ID`),
  CONSTRAINT `FK5pkgrlk32uqaxkrbve5mws1hj` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKhf88oagf6t62k28afn8uaijc7` FOREIGN KEY (`CUSTOMER_REVIEW_ID`) REFERENCES `customer_review` (`CUSTOMER_REVIEW_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_history`
--

DROP TABLE IF EXISTS `file_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_history` (
  `FILE_HISTORY_ID` bigint(20) NOT NULL,
  `ACCOUNTED_DATE` datetime DEFAULT NULL,
  `DATE_ADDED` datetime NOT NULL,
  `DATE_DELETED` datetime DEFAULT NULL,
  `DOWNLOAD_COUNT` int(11) NOT NULL,
  `FILE_ID` bigint(20) DEFAULT NULL,
  `FILESIZE` int(11) NOT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`FILE_HISTORY_ID`),
  UNIQUE KEY `UKav35sb3v4nxq8v1n1rkxufir` (`MERCHANT_ID`,`FILE_ID`),
  CONSTRAINT `FK2k8h4penkjlbtc23vamwyek2g` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `geozone`
--

DROP TABLE IF EXISTS `geozone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `geozone` (
  `GEOZONE_ID` bigint(20) NOT NULL,
  `GEOZONE_CODE` varchar(255) DEFAULT NULL,
  `GEOZONE_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`GEOZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `geozone_description`
--

DROP TABLE IF EXISTS `geozone_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `geozone_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `GEOZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKsoq8o99w3c8ys3ntamt5i4mat` (`GEOZONE_ID`,`LANGUAGE_ID`),
  KEY `FK1t2hp628edebe5d6co2whbla9` (`LANGUAGE_ID`),
  CONSTRAINT `FK1t2hp628edebe5d6co2whbla9` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKn82te2yb2st4hk2qlhl8ileb9` FOREIGN KEY (`GEOZONE_ID`) REFERENCES `geozone` (`GEOZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `language`
--

DROP TABLE IF EXISTS `language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `language` (
  `LANGUAGE_ID` int(11) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CODE` varchar(255) NOT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  PRIMARY KEY (`LANGUAGE_ID`),
  KEY `LANGUAGE_code` (`CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `manufacturer`
--

DROP TABLE IF EXISTS `manufacturer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `manufacturer` (
  `MANUFACTURER_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CODE` varchar(100) NOT NULL,
  `MANUFACTURER_IMAGE` varchar(255) DEFAULT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`MANUFACTURER_ID`),
  UNIQUE KEY `UK6brqfdkga7jc78n8dh3v595y3` (`MERCHANT_ID`,`CODE`),
  CONSTRAINT `FKhswph4nthrqwffjekccudsrt2` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `manufacturer_description`
--

DROP TABLE IF EXISTS `manufacturer_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `manufacturer_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `DATE_LAST_CLICK` datetime DEFAULT NULL,
  `MANUFACTURERS_URL` varchar(255) DEFAULT NULL,
  `URL_CLICKED` int(11) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `MANUFACTURER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKlpv09p83sc887clxe04nroup6` (`MANUFACTURER_ID`,`LANGUAGE_ID`),
  KEY `FK20t33wr4tp1kt1uyw7s8a3afl` (`LANGUAGE_ID`),
  CONSTRAINT `FK20t33wr4tp1kt1uyw7s8a3afl` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKre4iys57n5cfbgpg3qqgewtrh` FOREIGN KEY (`MANUFACTURER_ID`) REFERENCES `manufacturer` (`MANUFACTURER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_configuration`
--

DROP TABLE IF EXISTS `merchant_configuration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant_configuration` (
  `MERCHANT_CONFIG_ID` bigint(20) NOT NULL,
  `ACTIVE` bit(1) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CONFIG_KEY` varchar(255) DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  `VALUE` longtext,
  `MERCHANT_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`MERCHANT_CONFIG_ID`),
  UNIQUE KEY `UKj0c3h8onw3m6hjcr3yylst9fb` (`MERCHANT_ID`,`CONFIG_KEY`),
  CONSTRAINT `FKf9bkgf0ysbp5fo9j69shm0pri` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_language`
--

DROP TABLE IF EXISTS `merchant_language`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant_language` (
  `stores_MERCHANT_ID` int(11) NOT NULL,
  `languages_LANGUAGE_ID` int(11) NOT NULL,
  KEY `FKjwy0pjijh1qmcoivq50o2jgec` (`languages_LANGUAGE_ID`),
  KEY `FKiisj0tmoujv6n3iqmytvo39kn` (`stores_MERCHANT_ID`),
  CONSTRAINT `FKiisj0tmoujv6n3iqmytvo39kn` FOREIGN KEY (`stores_MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKjwy0pjijh1qmcoivq50o2jgec` FOREIGN KEY (`languages_LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_log`
--

DROP TABLE IF EXISTS `merchant_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant_log` (
  `MERCHANT_LOG_ID` bigint(20) NOT NULL,
  `LOG` longtext,
  `MODULE` varchar(25) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`MERCHANT_LOG_ID`),
  KEY `FKto727b9r68qrtn2vvdqdvd4ic` (`MERCHANT_ID`),
  CONSTRAINT `FKto727b9r68qrtn2vvdqdvd4ic` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `merchant_store`
--

DROP TABLE IF EXISTS `merchant_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `merchant_store` (
  `MERCHANT_ID` int(11) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `STORE_CODE` varchar(100) NOT NULL,
  `CONTINUESHOPPINGURL` varchar(150) DEFAULT NULL,
  `CURRENCY_FORMAT_NATIONAL` bit(1) DEFAULT NULL,
  `DOMAIN_NAME` varchar(80) DEFAULT NULL,
  `IN_BUSINESS_SINCE` date DEFAULT NULL,
  `INVOICE_TEMPLATE` varchar(25) DEFAULT NULL,
  `SEIZEUNITCODE` varchar(5) DEFAULT NULL,
  `STORE_EMAIL` varchar(60) NOT NULL,
  `STORE_LOGO` varchar(100) DEFAULT NULL,
  `STORE_TEMPLATE` varchar(25) DEFAULT NULL,
  `STORE_ADDRESS` varchar(255) DEFAULT NULL,
  `STORE_CITY` varchar(100) NOT NULL,
  `STORE_NAME` varchar(100) NOT NULL,
  `STORE_PHONE` varchar(50) NOT NULL,
  `STORE_POSTAL_CODE` varchar(15) NOT NULL,
  `STORE_STATE_PROV` varchar(100) DEFAULT NULL,
  `USE_CACHE` bit(1) DEFAULT NULL,
  `WEIGHTUNITCODE` varchar(5) DEFAULT NULL,
  `COUNTRY_ID` int(11) NOT NULL,
  `CURRENCY_ID` bigint(20) NOT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `ZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`MERCHANT_ID`),
  UNIQUE KEY `UK_4pvtsnqv4nlao8725n9ldpguf` (`STORE_CODE`),
  KEY `FK2gn7vpkd9x832urw7c6jlawnn` (`COUNTRY_ID`),
  KEY `FK63hlw9wp1k1x3f5tke7t2us7s` (`CURRENCY_ID`),
  KEY `FKdnemo9tl8tjhkxko83psvkv19` (`LANGUAGE_ID`),
  KEY `FK5o24aky9161jyofyxmg0g53vv` (`ZONE_ID`),
  CONSTRAINT `FK2gn7vpkd9x832urw7c6jlawnn` FOREIGN KEY (`COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FK5o24aky9161jyofyxmg0g53vv` FOREIGN KEY (`ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FK63hlw9wp1k1x3f5tke7t2us7s` FOREIGN KEY (`CURRENCY_ID`) REFERENCES `currency` (`CURRENCY_ID`),
  CONSTRAINT `FKdnemo9tl8tjhkxko83psvkv19` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `module_configuration`
--

DROP TABLE IF EXISTS `module_configuration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `module_configuration` (
  `MODULE_CONF_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CODE` varchar(255) NOT NULL,
  `DETAILS` longtext,
  `CONFIGURATION` longtext,
  `CUSTOM_IND` bit(1) DEFAULT NULL,
  `IMAGE` varchar(255) DEFAULT NULL,
  `MODULE` varchar(255) DEFAULT NULL,
  `REGIONS` varchar(255) DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`MODULE_CONF_ID`),
  KEY `MODULE_CONFIGURATION_module` (`MODULE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `optin`
--

DROP TABLE IF EXISTS `optin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `optin` (
  `OPTIN_ID` bigint(20) NOT NULL,
  `CODE` varchar(255) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `END_DATE` datetime DEFAULT NULL,
  `TYPE` varchar(255) NOT NULL,
  `START_DATE` datetime DEFAULT NULL,
  `MERCHANT_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`OPTIN_ID`),
  UNIQUE KEY `UKmanlx6siq6ddf14cud40k8gw6` (`MERCHANT_ID`,`CODE`),
  CONSTRAINT `FK37xvfo4the20avv7f1e1771fh` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_account`
--

DROP TABLE IF EXISTS `order_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_account` (
  `ORDER_ACCOUNT_ID` bigint(20) NOT NULL,
  `ORDER_ACCOUNT_BILL_DAY` int(11) NOT NULL,
  `ORDER_ACCOUNT_END_DATE` date DEFAULT NULL,
  `ORDER_ACCOUNT_START_DATE` date NOT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ACCOUNT_ID`),
  KEY `FKi6l5isodh81m5hy8ua06hx73n` (`ORDER_ID`),
  CONSTRAINT `FKi6l5isodh81m5hy8ua06hx73n` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_account_product`
--

DROP TABLE IF EXISTS `order_account_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_account_product` (
  `ORDER_ACCOUNT_PRODUCT_ID` bigint(20) NOT NULL,
  `ORDER_ACCOUNT_PRODUCT_ACCNT_DT` date DEFAULT NULL,
  `ORDER_ACCOUNT_PRODUCT_END_DT` date DEFAULT NULL,
  `ORDER_ACCOUNT_PRODUCT_EOT` datetime DEFAULT NULL,
  `ORDER_ACCOUNT_PRODUCT_L_ST_DT` datetime DEFAULT NULL,
  `ORDER_ACCOUNT_PRODUCT_L_TRX_ST` int(11) NOT NULL,
  `ORDER_ACCOUNT_PRODUCT_PM_FR_TY` int(11) NOT NULL,
  `ORDER_ACCOUNT_PRODUCT_ST_DT` date NOT NULL,
  `ORDER_ACCOUNT_PRODUCT_STATUS` int(11) NOT NULL,
  `ORDER_ACCOUNT_ID` bigint(20) NOT NULL,
  `ORDER_PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ACCOUNT_PRODUCT_ID`),
  KEY `FK7oxc8ygov7vd2ajt185jhiwts` (`ORDER_ACCOUNT_ID`),
  KEY `FK5kiyyb8ekqi9bfowytww8atcx` (`ORDER_PRODUCT_ID`),
  CONSTRAINT `FK5kiyyb8ekqi9bfowytww8atcx` FOREIGN KEY (`ORDER_PRODUCT_ID`) REFERENCES `order_product` (`ORDER_PRODUCT_ID`),
  CONSTRAINT `FK7oxc8ygov7vd2ajt185jhiwts` FOREIGN KEY (`ORDER_ACCOUNT_ID`) REFERENCES `order_account` (`ORDER_ACCOUNT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_attribute`
--

DROP TABLE IF EXISTS `order_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_attribute` (
  `ORDER_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `IDENTIFIER` varchar(255) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ATTRIBUTE_ID`),
  KEY `FK4nw5yrtgb4in6leve76bmdnua` (`ORDER_ID`),
  CONSTRAINT `FK4nw5yrtgb4in6leve76bmdnua` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_product`
--

DROP TABLE IF EXISTS `order_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_product` (
  `ORDER_PRODUCT_ID` bigint(20) NOT NULL,
  `ONETIME_CHARGE` decimal(19,2) NOT NULL,
  `PRODUCT_NAME` varchar(64) NOT NULL,
  `PRODUCT_QUANTITY` int(11) DEFAULT NULL,
  `PRODUCT_SKU` varchar(255) DEFAULT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_PRODUCT_ID`),
  KEY `FKf0sghmn59s14cxrjtrvkvi5yk` (`ORDER_ID`),
  CONSTRAINT `FKf0sghmn59s14cxrjtrvkvi5yk` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_product_attribute`
--

DROP TABLE IF EXISTS `order_product_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_product_attribute` (
  `ORDER_PRODUCT_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `PRODUCT_ATTRIBUTE_IS_FREE` bit(1) NOT NULL,
  `PRODUCT_ATTRIBUTE_NAME` varchar(255) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_PRICE` decimal(15,4) NOT NULL,
  `PRODUCT_ATTRIBUTE_VAL_NAME` varchar(255) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_WEIGHT` decimal(15,4) DEFAULT NULL,
  `PRODUCT_OPTION_ID` bigint(20) NOT NULL,
  `PRODUCT_OPTION_VALUE_ID` bigint(20) NOT NULL,
  `ORDER_PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_PRODUCT_ATTRIBUTE_ID`),
  KEY `FK7j86rvwaysbok1nuofrnmhmkx` (`ORDER_PRODUCT_ID`),
  CONSTRAINT `FK7j86rvwaysbok1nuofrnmhmkx` FOREIGN KEY (`ORDER_PRODUCT_ID`) REFERENCES `order_product` (`ORDER_PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_product_download`
--

DROP TABLE IF EXISTS `order_product_download`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_product_download` (
  `ORDER_PRODUCT_DOWNLOAD_ID` bigint(20) NOT NULL,
  `DOWNLOAD_COUNT` int(11) NOT NULL,
  `DOWNLOAD_MAXDAYS` int(11) NOT NULL,
  `ORDER_PRODUCT_FILENAME` varchar(255) NOT NULL,
  `ORDER_PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_PRODUCT_DOWNLOAD_ID`),
  KEY `FKstrda0eweharld63j8pxa2o2r` (`ORDER_PRODUCT_ID`),
  CONSTRAINT `FKstrda0eweharld63j8pxa2o2r` FOREIGN KEY (`ORDER_PRODUCT_ID`) REFERENCES `order_product` (`ORDER_PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_product_price`
--

DROP TABLE IF EXISTS `order_product_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_product_price` (
  `ORDER_PRODUCT_PRICE_ID` bigint(20) NOT NULL,
  `DEFAULT_PRICE` bit(1) NOT NULL,
  `PRODUCT_PRICE` decimal(19,2) NOT NULL,
  `PRODUCT_PRICE_CODE` varchar(64) NOT NULL,
  `PRODUCT_PRICE_NAME` varchar(255) DEFAULT NULL,
  `PRODUCT_PRICE_SPECIAL` decimal(19,2) DEFAULT NULL,
  `PRD_PRICE_SPECIAL_END_DT` datetime DEFAULT NULL,
  `PRD_PRICE_SPECIAL_ST_DT` datetime DEFAULT NULL,
  `ORDER_PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_PRODUCT_PRICE_ID`),
  KEY `FKnkukiqxrieonyulercgnh857s` (`ORDER_PRODUCT_ID`),
  CONSTRAINT `FKnkukiqxrieonyulercgnh857s` FOREIGN KEY (`ORDER_PRODUCT_ID`) REFERENCES `order_product` (`ORDER_PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_status_history`
--

DROP TABLE IF EXISTS `order_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_status_history` (
  `ORDER_STATUS_HISTORY_ID` bigint(20) NOT NULL,
  `COMMENTS` longtext,
  `CUSTOMER_NOTIFIED` int(11) DEFAULT NULL,
  `DATE_ADDED` datetime NOT NULL,
  `status` varchar(255) DEFAULT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_STATUS_HISTORY_ID`),
  KEY `FKmhghgf1xy3o0npsp8xkj6wyvq` (`ORDER_ID`),
  CONSTRAINT `FKmhghgf1xy3o0npsp8xkj6wyvq` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `order_total`
--

DROP TABLE IF EXISTS `order_total`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `order_total` (
  `ORDER_ACCOUNT_ID` bigint(20) NOT NULL,
  `MODULE` varchar(60) DEFAULT NULL,
  `CODE` varchar(255) NOT NULL,
  `ORDER_TOTAL_TYPE` varchar(255) DEFAULT NULL,
  `ORDER_VALUE_TYPE` varchar(255) DEFAULT NULL,
  `SORT_ORDER` int(11) NOT NULL,
  `TEXT` longtext,
  `TITLE` varchar(255) DEFAULT NULL,
  `VALUE` decimal(15,4) NOT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ACCOUNT_ID`),
  KEY `FK1tfvgk5smm80efdcc8uop4he3` (`ORDER_ID`),
  CONSTRAINT `FK1tfvgk5smm80efdcc8uop4he3` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `orders` (
  `ORDER_ID` bigint(20) NOT NULL,
  `BILLING_STREET_ADDRESS` varchar(256) DEFAULT NULL,
  `BILLING_CITY` varchar(100) DEFAULT NULL,
  `BILLING_COMPANY` varchar(100) DEFAULT NULL,
  `BILLING_FIRST_NAME` varchar(64) NOT NULL,
  `BILLING_LAST_NAME` varchar(64) NOT NULL,
  `LATITUDE` varchar(100) DEFAULT NULL,
  `LONGITUDE` varchar(100) DEFAULT NULL,
  `BILLING_POSTCODE` varchar(20) DEFAULT NULL,
  `BILLING_STATE` varchar(100) DEFAULT NULL,
  `BILLING_TELEPHONE` varchar(32) DEFAULT NULL,
  `CHANNEL` varchar(255) DEFAULT NULL,
  `CONFIRMED_ADDRESS` bit(1) DEFAULT NULL,
  `CARD_TYPE` varchar(255) DEFAULT NULL,
  `CC_CVV` varchar(255) DEFAULT NULL,
  `CC_EXPIRES` varchar(255) DEFAULT NULL,
  `CC_NUMBER` varchar(255) DEFAULT NULL,
  `CC_OWNER` varchar(255) DEFAULT NULL,
  `CURRENCY_VALUE` decimal(19,2) DEFAULT NULL,
  `CUSTOMER_AGREED` bit(1) DEFAULT NULL,
  `CUSTOMER_EMAIL_ADDRESS` varchar(50) NOT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  `DATE_PURCHASED` date DEFAULT NULL,
  `DELIVERY_STREET_ADDRESS` varchar(256) DEFAULT NULL,
  `DELIVERY_CITY` varchar(100) DEFAULT NULL,
  `DELIVERY_COMPANY` varchar(100) DEFAULT NULL,
  `DELIVERY_FIRST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_LAST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_POSTCODE` varchar(20) DEFAULT NULL,
  `DELIVERY_STATE` varchar(100) DEFAULT NULL,
  `DELIVERY_TELEPHONE` varchar(32) DEFAULT NULL,
  `IP_ADDRESS` varchar(255) DEFAULT NULL,
  `LAST_MODIFIED` datetime DEFAULT NULL,
  `LOCALE` varchar(255) DEFAULT NULL,
  `ORDER_DATE_FINISHED` datetime DEFAULT NULL,
  `ORDER_TYPE` varchar(255) DEFAULT NULL,
  `PAYMENT_MODULE_CODE` varchar(255) DEFAULT NULL,
  `PAYMENT_TYPE` varchar(255) DEFAULT NULL,
  `SHIPPING_MODULE_CODE` varchar(255) DEFAULT NULL,
  `ORDER_STATUS` varchar(255) DEFAULT NULL,
  `ORDER_TOTAL` decimal(19,2) DEFAULT NULL,
  `BILLING_COUNTRY_ID` int(11) NOT NULL,
  `BILLING_ZONE_ID` bigint(20) DEFAULT NULL,
  `CURRENCY_ID` bigint(20) DEFAULT NULL,
  `DELIVERY_COUNTRY_ID` int(11) DEFAULT NULL,
  `DELIVERY_ZONE_ID` bigint(20) DEFAULT NULL,
  `MERCHANTID` int(11) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`),
  KEY `FKipesu5tupnriahutgle6xu9ed` (`BILLING_COUNTRY_ID`),
  KEY `FKit6ti99mv5uvuxqskhurv3y59` (`BILLING_ZONE_ID`),
  KEY `FKfusivmw6q3gjxnmp47n9s74qi` (`CURRENCY_ID`),
  KEY `FKnlx97vjyorunxglhy5bird06c` (`DELIVERY_COUNTRY_ID`),
  KEY `FKn9uvjl8105fsly4doo8rqnv5b` (`DELIVERY_ZONE_ID`),
  KEY `FKaodv5ffayq8x50q311o2y8m1` (`MERCHANTID`),
  CONSTRAINT `FKaodv5ffayq8x50q311o2y8m1` FOREIGN KEY (`MERCHANTID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKfusivmw6q3gjxnmp47n9s74qi` FOREIGN KEY (`CURRENCY_ID`) REFERENCES `currency` (`CURRENCY_ID`),
  CONSTRAINT `FKipesu5tupnriahutgle6xu9ed` FOREIGN KEY (`BILLING_COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FKit6ti99mv5uvuxqskhurv3y59` FOREIGN KEY (`BILLING_ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FKn9uvjl8105fsly4doo8rqnv5b` FOREIGN KEY (`DELIVERY_ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FKnlx97vjyorunxglhy5bird06c` FOREIGN KEY (`DELIVERY_COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permission` (
  `PERMISSION_ID` int(11) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `PERMISSION_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`PERMISSION_ID`),
  UNIQUE KEY `UK_ss26hgwetkj8ms5y5jn2co4j3` (`PERMISSION_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `permission_group`
--

DROP TABLE IF EXISTS `permission_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permission_group` (
  `PERMISSION_ID` int(11) NOT NULL,
  `GROUP_ID` int(11) NOT NULL,
  KEY `FKr7ylutdgqp1nrlbhjwit6y17g` (`GROUP_ID`),
  KEY `FK77ly3khyuu40odly02d351s84` (`PERMISSION_ID`),
  CONSTRAINT `FK77ly3khyuu40odly02d351s84` FOREIGN KEY (`PERMISSION_ID`) REFERENCES `permission` (`PERMISSION_ID`),
  CONSTRAINT `FKr7ylutdgqp1nrlbhjwit6y17g` FOREIGN KEY (`GROUP_ID`) REFERENCES `sm_group` (`GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product` (
  `PRODUCT_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `AVAILABLE` bit(1) DEFAULT NULL,
  `COND` int(11) DEFAULT NULL,
  `DATE_AVAILABLE` datetime DEFAULT NULL,
  `PREORDER` bit(1) DEFAULT NULL,
  `PRODUCT_HEIGHT` decimal(19,2) DEFAULT NULL,
  `PRODUCT_FREE` bit(1) DEFAULT NULL,
  `PRODUCT_LENGTH` decimal(19,2) DEFAULT NULL,
  `QUANTITY_ORDERED` int(11) DEFAULT NULL,
  `REVIEW_AVG` decimal(19,2) DEFAULT NULL,
  `REVIEW_COUNT` int(11) DEFAULT NULL,
  `PRODUCT_SHIP` bit(1) DEFAULT NULL,
  `PRODUCT_VIRTUAL` bit(1) DEFAULT NULL,
  `PRODUCT_WEIGHT` decimal(19,2) DEFAULT NULL,
  `PRODUCT_WIDTH` decimal(19,2) DEFAULT NULL,
  `REF_SKU` varchar(255) DEFAULT NULL,
  `RENTAL_DURATION` int(11) DEFAULT NULL,
  `RENTAL_PERIOD` int(11) DEFAULT NULL,
  `RENTAL_STATUS` int(11) DEFAULT NULL,
  `SKU` varchar(255) NOT NULL,
  `SORT_ORDER` int(11) DEFAULT NULL,
  `MANUFACTURER_ID` bigint(20) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  `TAX_CLASS_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_TYPE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_ID`),
  UNIQUE KEY `UKs8ofsn9pehdrstjg52j5qabxh` (`MERCHANT_ID`,`SKU`),
  KEY `FKra5mmrdxn3ci86hod7q1u3vu9` (`MANUFACTURER_ID`),
  KEY `FKqtt5f0aht5h7ough5rbkkcb33` (`CUSTOMER_ID`),
  KEY `FKb8oqtc3j8sqo0t8xdrne7pg69` (`TAX_CLASS_ID`),
  KEY `FKeiirvj8eu40h103fth8es1mt0` (`PRODUCT_TYPE_ID`),
  CONSTRAINT `FKb8oqtc3j8sqo0t8xdrne7pg69` FOREIGN KEY (`TAX_CLASS_ID`) REFERENCES `tax_class` (`TAX_CLASS_ID`),
  CONSTRAINT `FKeiirvj8eu40h103fth8es1mt0` FOREIGN KEY (`PRODUCT_TYPE_ID`) REFERENCES `product_type` (`PRODUCT_TYPE_ID`),
  CONSTRAINT `FKhhoq1nd9e0i4m7rt8gkh7d67h` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKqtt5f0aht5h7ough5rbkkcb33` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKra5mmrdxn3ci86hod7q1u3vu9` FOREIGN KEY (`MANUFACTURER_ID`) REFERENCES `manufacturer` (`MANUFACTURER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_attribute`
--

DROP TABLE IF EXISTS `product_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_attribute` (
  `PRODUCT_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `PRODUCT_ATTRIBUTE_DEFAULT` bit(1) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_DISCOUNTED` bit(1) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_FOR_DISP` bit(1) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_REQUIRED` bit(1) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_FREE` bit(1) DEFAULT NULL,
  `PRODUCT_ATRIBUTE_PRICE` decimal(19,2) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_WEIGHT` decimal(19,2) DEFAULT NULL,
  `PRODUCT_ATTRIBUTE_SORT_ORD` int(11) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  `OPTION_ID` bigint(20) NOT NULL,
  `OPTION_VALUE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_ATTRIBUTE_ID`),
  UNIQUE KEY `UKo0c6cfxcfejwfa2877gfgpuco` (`OPTION_ID`,`OPTION_VALUE_ID`,`PRODUCT_ID`),
  KEY `FKml3nvemdjya159a7669qt1gjd` (`PRODUCT_ID`),
  KEY `FK3rleultg9fn2dxruefbb18d5t` (`OPTION_VALUE_ID`),
  CONSTRAINT `FK2st60u9twmvvaowwn88mt3lrx` FOREIGN KEY (`OPTION_ID`) REFERENCES `product_option` (`PRODUCT_OPTION_ID`),
  CONSTRAINT `FK3rleultg9fn2dxruefbb18d5t` FOREIGN KEY (`OPTION_VALUE_ID`) REFERENCES `product_option_value` (`PRODUCT_OPTION_VALUE_ID`),
  CONSTRAINT `FKml3nvemdjya159a7669qt1gjd` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_availability`
--

DROP TABLE IF EXISTS `product_availability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_availability` (
  `PRODUCT_AVAIL_ID` bigint(20) NOT NULL,
  `DATE_AVAILABLE` date DEFAULT NULL,
  `FREE_SHIPPING` bit(1) DEFAULT NULL,
  `QUANTITY` int(11) NOT NULL,
  `QUANTITY_ORD_MAX` int(11) DEFAULT NULL,
  `QUANTITY_ORD_MIN` int(11) DEFAULT NULL,
  `STATUS` bit(1) DEFAULT NULL,
  `REGION` varchar(255) DEFAULT NULL,
  `REGION_VARIANT` varchar(255) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_AVAIL_ID`),
  KEY `FK5sbh4dx25pmjcqx958hr9ys8h` (`PRODUCT_ID`),
  CONSTRAINT `FK5sbh4dx25pmjcqx958hr9ys8h` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_category`
--

DROP TABLE IF EXISTS `product_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_category` (
  `PRODUCT_ID` bigint(20) NOT NULL,
  `CATEGORY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_ID`,`CATEGORY_ID`),
  KEY `FK3xw1sbaa29r534jvedimdd7md` (`CATEGORY_ID`),
  CONSTRAINT `FK3xw1sbaa29r534jvedimdd7md` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `category` (`CATEGORY_ID`),
  CONSTRAINT `FKa7245ly271mb0crlhxwhhppsq` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_description`
--

DROP TABLE IF EXISTS `product_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `META_DESCRIPTION` varchar(255) DEFAULT NULL,
  `META_KEYWORDS` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(255) DEFAULT NULL,
  `DOWNLOAD_LNK` varchar(255) DEFAULT NULL,
  `PRODUCT_HIGHLIGHT` varchar(255) DEFAULT NULL,
  `SEF_URL` varchar(255) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKq4dnkx5b776ayqas2h4rr2d8q` (`PRODUCT_ID`,`LANGUAGE_ID`),
  KEY `FK6esjdaa6vu2t5vjin788a8og6` (`LANGUAGE_ID`),
  KEY `product_description_sef_url` (`SEF_URL`),
  CONSTRAINT `FK6esjdaa6vu2t5vjin788a8og6` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKm46yjcu59q79qrokgglwq2ove` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_digital`
--

DROP TABLE IF EXISTS `product_digital`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_digital` (
  `PRODUCT_DIGITAL_ID` bigint(20) NOT NULL,
  `FILE_NAME` varchar(255) NOT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_DIGITAL_ID`),
  UNIQUE KEY `UKjuk1qgkh9v5w7ghvb18krwo8v` (`PRODUCT_ID`,`FILE_NAME`),
  CONSTRAINT `FK47fmb5cg68pws7k26txyl1il6` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_image`
--

DROP TABLE IF EXISTS `product_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_image` (
  `PRODUCT_IMAGE_ID` bigint(20) NOT NULL,
  `DEFAULT_IMAGE` bit(1) DEFAULT NULL,
  `IMAGE_CROP` bit(1) DEFAULT NULL,
  `IMAGE_TYPE` int(11) DEFAULT NULL,
  `PRODUCT_IMAGE` varchar(255) DEFAULT NULL,
  `PRODUCT_IMAGE_URL` varchar(255) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_IMAGE_ID`),
  KEY `FKgab836d8rxqg8vv55nm02r65i` (`PRODUCT_ID`),
  CONSTRAINT `FKgab836d8rxqg8vv55nm02r65i` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_image_description`
--

DROP TABLE IF EXISTS `product_image_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_image_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `ALT_TAG` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_IMAGE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKn7yhdj6ccydgf201gibb882cd` (`PRODUCT_IMAGE_ID`,`LANGUAGE_ID`),
  KEY `FKlhdnpki4sf98wev0pcj2bvnih` (`LANGUAGE_ID`),
  CONSTRAINT `FK1dhldo18nj9l2y6qympgucynq` FOREIGN KEY (`PRODUCT_IMAGE_ID`) REFERENCES `product_image` (`PRODUCT_IMAGE_ID`),
  CONSTRAINT `FKlhdnpki4sf98wev0pcj2bvnih` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_option`
--

DROP TABLE IF EXISTS `product_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_option` (
  `PRODUCT_OPTION_ID` bigint(20) NOT NULL,
  `PRODUCT_OPTION_CODE` varchar(255) NOT NULL,
  `PRODUCT_OPTION_SORT_ORD` int(11) DEFAULT NULL,
  `PRODUCT_OPTION_TYPE` varchar(10) DEFAULT NULL,
  `PRODUCT_OPTION_READ` bit(1) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`PRODUCT_OPTION_ID`),
  UNIQUE KEY `UKhfcw5oi9ulljlog1b7ns1r9tu` (`MERCHANT_ID`,`PRODUCT_OPTION_CODE`),
  KEY `PRD_OPTION_CODE_IDX` (`PRODUCT_OPTION_CODE`),
  CONSTRAINT `FKp8cski5t5f5m4et4fw0uilcgu` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_option_desc`
--

DROP TABLE IF EXISTS `product_option_desc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_option_desc` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `PRODUCT_OPTION_COMMENT` longtext,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKmkcm8isyyyqbjd1yyb8mrpkuw` (`PRODUCT_OPTION_ID`,`LANGUAGE_ID`),
  KEY `FK8fiwk5o1gbn2r2u8529yaf9xt` (`LANGUAGE_ID`),
  CONSTRAINT `FK8fiwk5o1gbn2r2u8529yaf9xt` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKgjqmfofile4hwv867irsnvuc0` FOREIGN KEY (`PRODUCT_OPTION_ID`) REFERENCES `product_option` (`PRODUCT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_option_value`
--

DROP TABLE IF EXISTS `product_option_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_option_value` (
  `PRODUCT_OPTION_VALUE_ID` bigint(20) NOT NULL,
  `PRODUCT_OPTION_VAL_CODE` varchar(255) NOT NULL,
  `PRODUCT_OPT_FOR_DISP` bit(1) DEFAULT NULL,
  `PRODUCT_OPT_VAL_IMAGE` varchar(255) DEFAULT NULL,
  `PRODUCT_OPT_VAL_SORT_ORD` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`PRODUCT_OPTION_VALUE_ID`),
  UNIQUE KEY `UKixbpi4hxrhljh935c3xfvnvsh` (`MERCHANT_ID`,`PRODUCT_OPTION_VAL_CODE`),
  KEY `PRD_OPTION_VAL_CODE_IDX` (`PRODUCT_OPTION_VAL_CODE`),
  CONSTRAINT `FKnd3nw0mamlk8bkxo8ad5m85pq` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_option_value_description`
--

DROP TABLE IF EXISTS `product_option_value_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_option_value_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_OPTION_VALUE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKasgc60ot1wy0uho96n0j8429p` (`PRODUCT_OPTION_VALUE_ID`,`LANGUAGE_ID`),
  KEY `FK19mnby7atlt85exlypxdxhacx` (`LANGUAGE_ID`),
  CONSTRAINT `FK19mnby7atlt85exlypxdxhacx` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKqttc6b79yp2s1hyrhg4thag6s` FOREIGN KEY (`PRODUCT_OPTION_VALUE_ID`) REFERENCES `product_option_value` (`PRODUCT_OPTION_VALUE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_price`
--

DROP TABLE IF EXISTS `product_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_price` (
  `PRODUCT_PRICE_ID` bigint(20) NOT NULL,
  `PRODUCT_PRICE_CODE` varchar(255) NOT NULL,
  `DEFAULT_PRICE` bit(1) DEFAULT NULL,
  `PRODUCT_PRICE_AMOUNT` decimal(19,2) NOT NULL,
  `PRODUCT_PRICE_SPECIAL_AMOUNT` decimal(19,2) DEFAULT NULL,
  `PRODUCT_PRICE_SPECIAL_END_DATE` date DEFAULT NULL,
  `PRODUCT_PRICE_SPECIAL_ST_DATE` date DEFAULT NULL,
  `PRODUCT_PRICE_TYPE` varchar(20) DEFAULT NULL,
  `PRODUCT_AVAIL_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_PRICE_ID`),
  KEY `FK1dic7jnnk1qikgvwcrf4dw12r` (`PRODUCT_AVAIL_ID`),
  CONSTRAINT `FK1dic7jnnk1qikgvwcrf4dw12r` FOREIGN KEY (`PRODUCT_AVAIL_ID`) REFERENCES `product_availability` (`PRODUCT_AVAIL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_price_description`
--

DROP TABLE IF EXISTS `product_price_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_price_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_PRICE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKfrsw8d41sxxogvxxoyd8nwaxu` (`PRODUCT_PRICE_ID`,`LANGUAGE_ID`),
  KEY `FK7bmbrjr8ar5icwdpt8myj6gei` (`LANGUAGE_ID`),
  CONSTRAINT `FK7bmbrjr8ar5icwdpt8myj6gei` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKbwxw861ipjsct606j3dagdjsf` FOREIGN KEY (`PRODUCT_PRICE_ID`) REFERENCES `product_price` (`PRODUCT_PRICE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_relationship`
--

DROP TABLE IF EXISTS `product_relationship`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_relationship` (
  `PRODUCT_RELATIONSHIP_ID` bigint(20) NOT NULL,
  `ACTIVE` bit(1) DEFAULT NULL,
  `CODE` varchar(255) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  `RELATED_PRODUCT_ID` bigint(20) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`PRODUCT_RELATIONSHIP_ID`),
  KEY `FKso3cvinykac5wdwu1tjgfotor` (`PRODUCT_ID`),
  KEY `FKfskwtawyt85g9h6761fa69ya5` (`RELATED_PRODUCT_ID`),
  KEY `FKnprvswtbgrm6bjfq3cbdl3qsm` (`MERCHANT_ID`),
  CONSTRAINT `FKfskwtawyt85g9h6761fa69ya5` FOREIGN KEY (`RELATED_PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`),
  CONSTRAINT `FKnprvswtbgrm6bjfq3cbdl3qsm` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKso3cvinykac5wdwu1tjgfotor` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_review`
--

DROP TABLE IF EXISTS `product_review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_review` (
  `PRODUCT_REVIEW_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `REVIEW_DATE` datetime DEFAULT NULL,
  `REVIEWS_RATING` double DEFAULT NULL,
  `REVIEWS_READ` bigint(20) DEFAULT NULL,
  `STATUS` int(11) DEFAULT NULL,
  `CUSTOMERS_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_REVIEW_ID`),
  UNIQUE KEY `UK9ew5idgdbk8a77534hbnhd4yb` (`CUSTOMERS_ID`,`PRODUCT_ID`),
  KEY `FKbfi8de7kxultg1vevq6jc1hn7` (`PRODUCT_ID`),
  CONSTRAINT `FK7tm0jrt0hiugo3ep49t3subou` FOREIGN KEY (`CUSTOMERS_ID`) REFERENCES `customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKbfi8de7kxultg1vevq6jc1hn7` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_review_description`
--

DROP TABLE IF EXISTS `product_review_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_review_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `PRODUCT_REVIEW_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKqno5wjdtcj8pm3ykkkh7t4rxj` (`PRODUCT_REVIEW_ID`,`LANGUAGE_ID`),
  KEY `FK7byc5jsf5bm4lk674ac44e50m` (`LANGUAGE_ID`),
  CONSTRAINT `FK7byc5jsf5bm4lk674ac44e50m` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKmjivhigdcxmytndlpjuhf4o25` FOREIGN KEY (`PRODUCT_REVIEW_ID`) REFERENCES `product_review` (`PRODUCT_REVIEW_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `product_type`
--

DROP TABLE IF EXISTS `product_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_type` (
  `PRODUCT_TYPE_ID` bigint(20) NOT NULL,
  `PRD_TYPE_ADD_TO_CART` bit(1) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `PRD_TYPE_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_TYPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shiping_origin`
--

DROP TABLE IF EXISTS `shiping_origin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shiping_origin` (
  `SHIP_ORIGIN_ID` bigint(20) NOT NULL,
  `ACTIVE` bit(1) DEFAULT NULL,
  `STREET_ADDRESS` varchar(256) NOT NULL,
  `CITY` varchar(100) NOT NULL,
  `POSTCODE` varchar(20) NOT NULL,
  `STATE` varchar(100) DEFAULT NULL,
  `COUNTRY_ID` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  `ZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SHIP_ORIGIN_ID`),
  KEY `FKpqig59usqvs9h0dw4lm8rv7yy` (`COUNTRY_ID`),
  KEY `FKp0dbwsv3sdsp57ex7j5k9b0oq` (`MERCHANT_ID`),
  KEY `FK6k73f1n18kr7mqp708aiwq047` (`ZONE_ID`),
  CONSTRAINT `FK6k73f1n18kr7mqp708aiwq047` FOREIGN KEY (`ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FKp0dbwsv3sdsp57ex7j5k9b0oq` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKpqig59usqvs9h0dw4lm8rv7yy` FOREIGN KEY (`COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shipping_quote`
--

DROP TABLE IF EXISTS `shipping_quote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shipping_quote` (
  `SHIPPING_QUOTE_ID` bigint(20) NOT NULL,
  `CART_ID` bigint(20) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  `DELIVERY_STREET_ADDRESS` varchar(256) DEFAULT NULL,
  `DELIVERY_CITY` varchar(100) DEFAULT NULL,
  `DELIVERY_COMPANY` varchar(100) DEFAULT NULL,
  `DELIVERY_FIRST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_LAST_NAME` varchar(64) DEFAULT NULL,
  `DELIVERY_POSTCODE` varchar(20) DEFAULT NULL,
  `DELIVERY_STATE` varchar(100) DEFAULT NULL,
  `DELIVERY_TELEPHONE` varchar(32) DEFAULT NULL,
  `SHIPPING_NUMBER_DAYS` int(11) DEFAULT NULL,
  `FREE_SHIPPING` bit(1) DEFAULT NULL,
  `QUOTE_HANDLING` decimal(19,2) DEFAULT NULL,
  `MODULE` varchar(255) NOT NULL,
  `OPTION_CODE` varchar(255) DEFAULT NULL,
  `OPTION_DELIVERY_DATE` datetime DEFAULT NULL,
  `OPTION_NAME` varchar(255) DEFAULT NULL,
  `OPTION_SHIPPING_DATE` datetime DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  `QUOTE_PRICE` decimal(19,2) DEFAULT NULL,
  `QUOTE_DATE` datetime DEFAULT NULL,
  `DELIVERY_COUNTRY_ID` int(11) DEFAULT NULL,
  `DELIVERY_ZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SHIPPING_QUOTE_ID`),
  KEY `FK9vb7tbjl8ivygdiqw883fewx7` (`DELIVERY_COUNTRY_ID`),
  KEY `FKiioesp0vl6x4om1jeajj4uy1t` (`DELIVERY_ZONE_ID`),
  CONSTRAINT `FK9vb7tbjl8ivygdiqw883fewx7` FOREIGN KEY (`DELIVERY_COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FKiioesp0vl6x4om1jeajj4uy1t` FOREIGN KEY (`DELIVERY_ZONE_ID`) REFERENCES `zone` (`ZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shopping_cart`
--

DROP TABLE IF EXISTS `shopping_cart`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shopping_cart` (
  `SHP_CART_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  `SHP_CART_CODE` varchar(255) NOT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`SHP_CART_ID`),
  UNIQUE KEY `UK_8ld8p40fwrjobi7t3n95pna35` (`SHP_CART_CODE`),
  KEY `SHP_CART_CODE_IDX` (`SHP_CART_CODE`),
  KEY `SHP_CART_CUSTOMER_IDX` (`CUSTOMER_ID`),
  KEY `FKqvghr5rmjefe3lw9mcolk30a0` (`MERCHANT_ID`),
  CONSTRAINT `FKqvghr5rmjefe3lw9mcolk30a0` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shopping_cart_attr_item`
--

DROP TABLE IF EXISTS `shopping_cart_attr_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shopping_cart_attr_item` (
  `SHP_CART_ATTR_ITEM_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `PRODUCT_ATTR_ID` bigint(20) NOT NULL,
  `SHP_CART_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SHP_CART_ATTR_ITEM_ID`),
  KEY `FKp42tpa623hyo9ww69v0ohb3er` (`SHP_CART_ITEM_ID`),
  CONSTRAINT `FKp42tpa623hyo9ww69v0ohb3er` FOREIGN KEY (`SHP_CART_ITEM_ID`) REFERENCES `shopping_cart_item` (`SHP_CART_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shopping_cart_item`
--

DROP TABLE IF EXISTS `shopping_cart_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shopping_cart_item` (
  `SHP_CART_ITEM_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  `QUANTITY` int(11) DEFAULT NULL,
  `SHP_CART_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SHP_CART_ITEM_ID`),
  KEY `FK2gbimdwe9uysd5xadnfl0xq83` (`SHP_CART_ID`),
  CONSTRAINT `FK2gbimdwe9uysd5xadnfl0xq83` FOREIGN KEY (`SHP_CART_ID`) REFERENCES `shopping_cart` (`SHP_CART_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sm_group`
--

DROP TABLE IF EXISTS `sm_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sm_group` (
  `GROUP_ID` int(11) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `GROUP_NAME` varchar(255) NOT NULL,
  `GROUP_TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`GROUP_ID`),
  UNIQUE KEY `UK_t83rjsoml3o785oj37lpqpyko` (`GROUP_NAME`),
  KEY `SM_GROUP_group_type` (`GROUP_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sm_sequencer`
--

DROP TABLE IF EXISTS `sm_sequencer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sm_sequencer` (
  `SEQ_NAME` varchar(255) NOT NULL,
  `SEQ_COUNT` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SEQ_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sm_transaction`
--

DROP TABLE IF EXISTS `sm_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sm_transaction` (
  `TRANSACTION_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,2) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DETAILS` longtext,
  `PAYMENT_TYPE` varchar(255) DEFAULT NULL,
  `TRANSACTION_DATE` datetime DEFAULT NULL,
  `TRANSACTION_TYPE` varchar(255) DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`TRANSACTION_ID`),
  KEY `FK7j0s1gqh2tue1fyh5nyj5kwkp` (`ORDER_ID`),
  CONSTRAINT `FK7j0s1gqh2tue1fyh5nyj5kwkp` FOREIGN KEY (`ORDER_ID`) REFERENCES `orders` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_configuration`
--

DROP TABLE IF EXISTS `system_configuration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `system_configuration` (
  `SYSTEM_CONFIG_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `CONFIG_KEY` varchar(255) DEFAULT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SYSTEM_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `system_notification`
--

DROP TABLE IF EXISTS `system_notification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `system_notification` (
  `SYSTEM_NOTIF_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `END_DATE` date DEFAULT NULL,
  `CONFIG_KEY` varchar(255) DEFAULT NULL,
  `START_DATE` date DEFAULT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `MERCHANT_ID` int(11) DEFAULT NULL,
  `USER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SYSTEM_NOTIF_ID`),
  UNIQUE KEY `UKnpdnlc390vgr2mhepib1mtrmr` (`MERCHANT_ID`,`CONFIG_KEY`),
  KEY `FKa54891emcl0fo27a1qk54slvk` (`USER_ID`),
  CONSTRAINT `FKa54891emcl0fo27a1qk54slvk` FOREIGN KEY (`USER_ID`) REFERENCES `user` (`USER_ID`),
  CONSTRAINT `FKs6qk7l06e0s6m9n04momedgt7` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tax_class`
--

DROP TABLE IF EXISTS `tax_class`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tax_class` (
  `TAX_CLASS_ID` bigint(20) NOT NULL,
  `TAX_CLASS_CODE` varchar(10) NOT NULL,
  `TAX_CLASS_TITLE` varchar(32) NOT NULL,
  `MERCHANT_ID` int(11) DEFAULT NULL,
  PRIMARY KEY (`TAX_CLASS_ID`),
  UNIQUE KEY `UKa4q5q57a8oeh2ojeo8dhr935k` (`MERCHANT_ID`,`TAX_CLASS_CODE`),
  KEY `tax_class_tax_class_code` (`TAX_CLASS_CODE`),
  CONSTRAINT `FK82i8puujghcv7fc82qwsgjg8w` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tax_rate`
--

DROP TABLE IF EXISTS `tax_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tax_rate` (
  `TAX_RATE_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `TAX_CODE` varchar(255) NOT NULL,
  `PIGGYBACK` bit(1) DEFAULT NULL,
  `STORE_STATE_PROV` varchar(100) DEFAULT NULL,
  `TAX_PRIORITY` int(11) DEFAULT NULL,
  `TAX_RATE` decimal(7,4) NOT NULL,
  `COUNTRY_ID` int(11) NOT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  `PARENT_ID` bigint(20) DEFAULT NULL,
  `TAX_CLASS_ID` bigint(20) NOT NULL,
  `ZONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`TAX_RATE_ID`),
  UNIQUE KEY `UK8gh6l9n0xq03b91sglp62oelu` (`TAX_CODE`,`MERCHANT_ID`),
  KEY `FK6wm34jcwoembe1qsmle2wtwnv` (`COUNTRY_ID`),
  KEY `FKfwp6yka2qps9jna473e6c6yc1` (`MERCHANT_ID`),
  KEY `FKt8isen27i3ioa0tw3bl8qlvdh` (`PARENT_ID`),
  KEY `FK7bpa9pbl1gnj5y3xbgs3wc0eg` (`TAX_CLASS_ID`),
  KEY `FKm9snpf6o1nb4j1t80nas8d1ix` (`ZONE_ID`),
  CONSTRAINT `FK6wm34jcwoembe1qsmle2wtwnv` FOREIGN KEY (`COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`),
  CONSTRAINT `FK7bpa9pbl1gnj5y3xbgs3wc0eg` FOREIGN KEY (`TAX_CLASS_ID`) REFERENCES `tax_class` (`TAX_CLASS_ID`),
  CONSTRAINT `FKfwp6yka2qps9jna473e6c6yc1` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FKm9snpf6o1nb4j1t80nas8d1ix` FOREIGN KEY (`ZONE_ID`) REFERENCES `zone` (`ZONE_ID`),
  CONSTRAINT `FKt8isen27i3ioa0tw3bl8qlvdh` FOREIGN KEY (`PARENT_ID`) REFERENCES `tax_rate` (`TAX_RATE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tax_rate_description`
--

DROP TABLE IF EXISTS `tax_rate_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tax_rate_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `TAX_RATE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKt3xg8pl88yacdxg49nb46effg` (`TAX_RATE_ID`,`LANGUAGE_ID`),
  KEY `FKsicb2ydx42o04pvlnxw2mlx0w` (`LANGUAGE_ID`),
  CONSTRAINT `FK65c2lqslk5kx25dpkem2r0vxq` FOREIGN KEY (`TAX_RATE_ID`) REFERENCES `tax_rate` (`TAX_RATE_ID`),
  CONSTRAINT `FKsicb2ydx42o04pvlnxw2mlx0w` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `USER_ID` bigint(20) NOT NULL,
  `ACTIVE` bit(1) DEFAULT NULL,
  `ADMIN_EMAIL` varchar(255) NOT NULL,
  `ADMIN_NAME` varchar(100) NOT NULL,
  `ADMIN_PASSWORD` varchar(60) NOT NULL,
  `ADMIN_A1` varchar(255) DEFAULT NULL,
  `ADMIN_A2` varchar(255) DEFAULT NULL,
  `ADMIN_A3` varchar(255) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `ADMIN_FIRST_NAME` varchar(255) DEFAULT NULL,
  `LAST_ACCESS` datetime DEFAULT NULL,
  `ADMIN_LAST_NAME` varchar(255) DEFAULT NULL,
  `LOGIN_ACCESS` datetime DEFAULT NULL,
  `ADMIN_Q1` varchar(255) DEFAULT NULL,
  `ADMIN_Q2` varchar(255) DEFAULT NULL,
  `ADMIN_Q3` varchar(255) DEFAULT NULL,
  `LANGUAGE_ID` int(11) DEFAULT NULL,
  `MERCHANT_ID` int(11) NOT NULL,
  PRIMARY KEY (`USER_ID`),
  UNIQUE KEY `UK_7rbcj0gstolij2mp5g3xc7xfu` (`ADMIN_NAME`),
  KEY `FK3sh6qxgt118m71ttvkubgd9y8` (`LANGUAGE_ID`),
  KEY `FK2yn065l2n7nw9rofjs4hwpij2` (`MERCHANT_ID`),
  CONSTRAINT `FK2yn065l2n7nw9rofjs4hwpij2` FOREIGN KEY (`MERCHANT_ID`) REFERENCES `merchant_store` (`MERCHANT_ID`),
  CONSTRAINT `FK3sh6qxgt118m71ttvkubgd9y8` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_group`
--

DROP TABLE IF EXISTS `user_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_group` (
  `USER_ID` bigint(20) NOT NULL,
  `GROUP_ID` int(11) NOT NULL,
  KEY `FK75kainrhn4kh8j3sw2xbe7v61` (`GROUP_ID`),
  KEY `FKdonp1m2n25ua1465rhice3qna` (`USER_ID`),
  CONSTRAINT `FK75kainrhn4kh8j3sw2xbe7v61` FOREIGN KEY (`GROUP_ID`) REFERENCES `sm_group` (`GROUP_ID`),
  CONSTRAINT `FKdonp1m2n25ua1465rhice3qna` FOREIGN KEY (`USER_ID`) REFERENCES `user` (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `userconnection`
--

DROP TABLE IF EXISTS `userconnection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `userconnection` (
  `providerId` varchar(255) NOT NULL,
  `providerUserId` varchar(255) NOT NULL,
  `userId` varchar(255) NOT NULL,
  `accessToken` varchar(255) DEFAULT NULL,
  `displayName` varchar(255) DEFAULT NULL,
  `expireTime` bigint(20) DEFAULT NULL,
  `imageUrl` varchar(255) DEFAULT NULL,
  `profileUrl` varchar(255) DEFAULT NULL,
  `refreshToken` varchar(255) DEFAULT NULL,
  `secret` varchar(255) DEFAULT NULL,
  `userRank` int(11) NOT NULL,
  PRIMARY KEY (`providerId`,`providerUserId`,`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zone`
--

DROP TABLE IF EXISTS `zone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zone` (
  `ZONE_ID` bigint(20) NOT NULL,
  `ZONE_CODE` varchar(255) NOT NULL,
  `COUNTRY_ID` int(11) NOT NULL,
  PRIMARY KEY (`ZONE_ID`),
  UNIQUE KEY `UK_4tq3p5w8k4h4easyf5t3n1jdr` (`ZONE_CODE`),
  KEY `FKhn2c1w3e1twhjg7tiwv7vuk67` (`COUNTRY_ID`),
  CONSTRAINT `FKhn2c1w3e1twhjg7tiwv7vuk67` FOREIGN KEY (`COUNTRY_ID`) REFERENCES `country` (`COUNTRY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zone_description`
--

DROP TABLE IF EXISTS `zone_description`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `zone_description` (
  `DESCRIPTION_ID` bigint(20) NOT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_MODIFIED` datetime DEFAULT NULL,
  `UPDT_ID` varchar(20) DEFAULT NULL,
  `DESCRIPTION` longtext,
  `NAME` varchar(120) NOT NULL,
  `TITLE` varchar(100) DEFAULT NULL,
  `LANGUAGE_ID` int(11) NOT NULL,
  `ZONE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DESCRIPTION_ID`),
  UNIQUE KEY `UKm64laxgrv9fxm6io232ap4su9` (`ZONE_ID`,`LANGUAGE_ID`),
  KEY `FK69ybu7r3bgpcq65c77ji1udh3` (`LANGUAGE_ID`),
  CONSTRAINT `FK69ybu7r3bgpcq65c77ji1udh3` FOREIGN KEY (`LANGUAGE_ID`) REFERENCES `language` (`LANGUAGE_ID`),
  CONSTRAINT `FKpv4elin6w3b03756obqvk447f` FOREIGN KEY (`ZONE_ID`) REFERENCES `zone` (`ZONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-04-01 15:19:28

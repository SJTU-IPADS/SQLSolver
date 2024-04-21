-- MySQL dump 10.13  Distrib 5.7.29, for Linux (x86_64)
--
-- Host: 10.0.0.102    Database: broadleaf_opt
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
-- Table structure for table `blc_additional_offer_info`
--

DROP TABLE IF EXISTS `blc_additional_offer_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_additional_offer_info` (
  `BLC_ORDER_ORDER_ID` bigint(20) NOT NULL,
  `OFFER_INFO_ID` bigint(20) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`BLC_ORDER_ORDER_ID`,`OFFER_ID`),
  KEY `FKlkk2kdjpv0v0ybxnc11p7hg4e` (`OFFER_INFO_ID`),
  KEY `FK40nm1ylfeiv2t6ojcmehu8gcr` (`OFFER_ID`),
  CONSTRAINT `FK40nm1ylfeiv2t6ojcmehu8gcr` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`),
  CONSTRAINT `FKlkk2kdjpv0v0ybxnc11p7hg4e` FOREIGN KEY (`OFFER_INFO_ID`) REFERENCES `blc_offer_info` (`OFFER_INFO_ID`),
  CONSTRAINT `FKrfc8a02u7yp8qqk206ug62lnb` FOREIGN KEY (`BLC_ORDER_ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_address`
--

DROP TABLE IF EXISTS `blc_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_address` (
  `ADDRESS_ID` bigint(20) NOT NULL,
  `ADDRESS_LINE1` varchar(255) NOT NULL,
  `ADDRESS_LINE2` varchar(255) DEFAULT NULL,
  `ADDRESS_LINE3` varchar(255) DEFAULT NULL,
  `CITY` varchar(255) NOT NULL,
  `COMPANY_NAME` varchar(255) DEFAULT NULL,
  `COUNTY` varchar(255) DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `FAX` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) DEFAULT NULL,
  `FULL_NAME` varchar(255) DEFAULT NULL,
  `IS_ACTIVE` bit(1) DEFAULT NULL,
  `IS_BUSINESS` bit(1) DEFAULT NULL,
  `IS_DEFAULT` bit(1) DEFAULT NULL,
  `IS_MAILING` bit(1) DEFAULT NULL,
  `IS_STREET` bit(1) DEFAULT NULL,
  `ISO_COUNTRY_SUB` varchar(255) DEFAULT NULL,
  `LAST_NAME` varchar(255) DEFAULT NULL,
  `POSTAL_CODE` varchar(255) DEFAULT NULL,
  `PRIMARY_PHONE` varchar(255) DEFAULT NULL,
  `SECONDARY_PHONE` varchar(255) DEFAULT NULL,
  `STANDARDIZED` bit(1) DEFAULT NULL,
  `SUB_STATE_PROV_REG` varchar(255) DEFAULT NULL,
  `TOKENIZED_ADDRESS` varchar(255) DEFAULT NULL,
  `VERIFICATION_LEVEL` varchar(255) DEFAULT NULL,
  `ZIP_FOUR` varchar(255) DEFAULT NULL,
  `COUNTRY` varchar(255) DEFAULT NULL,
  `ISO_COUNTRY_ALPHA2` varchar(255) DEFAULT NULL,
  `PHONE_FAX_ID` bigint(20) DEFAULT NULL,
  `PHONE_PRIMARY_ID` bigint(20) DEFAULT NULL,
  `PHONE_SECONDARY_ID` bigint(20) DEFAULT NULL,
  `STATE_PROV_REGION` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ADDRESS_ID`),
  KEY `ADDRESS_COUNTRY_INDEX` (`COUNTRY`),
  KEY `ADDRESS_ISO_COUNTRY_IDX` (`ISO_COUNTRY_ALPHA2`),
  KEY `ADDRESS_PHONE_FAX_IDX` (`PHONE_FAX_ID`),
  KEY `ADDRESS_PHONE_PRI_IDX` (`PHONE_PRIMARY_ID`),
  KEY `ADDRESS_PHONE_SEC_IDX` (`PHONE_SECONDARY_ID`),
  KEY `ADDRESS_STATE_INDEX` (`STATE_PROV_REGION`),
  CONSTRAINT `FK6hgxmyv131s9xr4ei6uih9opw` FOREIGN KEY (`COUNTRY`) REFERENCES `blc_country` (`ABBREVIATION`),
  CONSTRAINT `FKdg3srpny8h476fl59expu2cv0` FOREIGN KEY (`STATE_PROV_REGION`) REFERENCES `blc_state` (`ABBREVIATION`),
  CONSTRAINT `FKlafhchfputda32qhub54fa726` FOREIGN KEY (`PHONE_PRIMARY_ID`) REFERENCES `blc_phone` (`PHONE_ID`),
  CONSTRAINT `FKlbxqgy7cjnjn5ey2wqvpjnhe5` FOREIGN KEY (`PHONE_SECONDARY_ID`) REFERENCES `blc_phone` (`PHONE_ID`),
  CONSTRAINT `FKp37ru1cyeu6fq48ohmjmyvjej` FOREIGN KEY (`ISO_COUNTRY_ALPHA2`) REFERENCES `blc_iso_country` (`ALPHA_2`),
  CONSTRAINT `FKrgw6kfwuqepeo3u7i75t57l8w` FOREIGN KEY (`PHONE_FAX_ID`) REFERENCES `blc_phone` (`PHONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_module`
--

DROP TABLE IF EXISTS `blc_admin_module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_module` (
  `ADMIN_MODULE_ID` bigint(20) NOT NULL,
  `DISPLAY_ORDER` int(11) DEFAULT NULL,
  `ICON` varchar(255) DEFAULT NULL,
  `MODULE_KEY` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ADMIN_MODULE_ID`),
  KEY `ADMINMODULE_NAME_INDEX` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_password_token`
--

DROP TABLE IF EXISTS `blc_admin_password_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_password_token` (
  `PASSWORD_TOKEN` varchar(255) NOT NULL,
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  `CREATE_DATE` datetime NOT NULL,
  `TOKEN_USED_DATE` datetime DEFAULT NULL,
  `TOKEN_USED_FLAG` bit(1) NOT NULL,
  PRIMARY KEY (`PASSWORD_TOKEN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_permission`
--

DROP TABLE IF EXISTS `blc_admin_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_permission` (
  `ADMIN_PERMISSION_ID` bigint(20) NOT NULL,
  `DESCRIPTION` varchar(255) NOT NULL,
  `IS_FRIENDLY` bit(1) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `PERMISSION_TYPE` varchar(255) NOT NULL,
  PRIMARY KEY (`ADMIN_PERMISSION_ID`),
  KEY `BLC_ADMIN_PERMISSION_description` (`DESCRIPTION`),
  KEY `blc_admin_permission_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_permission_entity`
--

DROP TABLE IF EXISTS `blc_admin_permission_entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_permission_entity` (
  `ADMIN_PERMISSION_ENTITY_ID` bigint(20) NOT NULL,
  `CEILING_ENTITY` varchar(255) NOT NULL,
  `ADMIN_PERMISSION_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ADMIN_PERMISSION_ENTITY_ID`),
  KEY `FKr7lum3wwl9kacdlgw4cwdrsas` (`ADMIN_PERMISSION_ID`),
  CONSTRAINT `FKr7lum3wwl9kacdlgw4cwdrsas` FOREIGN KEY (`ADMIN_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_permission_xref`
--

DROP TABLE IF EXISTS `blc_admin_permission_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_permission_xref` (
  `CHILD_PERMISSION_ID` bigint(20) NOT NULL,
  `ADMIN_PERMISSION_ID` bigint(20) NOT NULL,
  KEY `FK1m3h00oqtternnpeiupslooan` (`ADMIN_PERMISSION_ID`),
  KEY `FK9gfarfrwe5wnew41w9oyd3j6y` (`CHILD_PERMISSION_ID`),
  CONSTRAINT `FK1m3h00oqtternnpeiupslooan` FOREIGN KEY (`ADMIN_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`),
  CONSTRAINT `FK9gfarfrwe5wnew41w9oyd3j6y` FOREIGN KEY (`CHILD_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_role`
--

DROP TABLE IF EXISTS `blc_admin_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_role` (
  `ADMIN_ROLE_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DESCRIPTION` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ADMIN_ROLE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_role_permission_xref`
--

DROP TABLE IF EXISTS `blc_admin_role_permission_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_role_permission_xref` (
  `ADMIN_ROLE_ID` bigint(20) NOT NULL,
  `ADMIN_PERMISSION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ADMIN_PERMISSION_ID`,`ADMIN_ROLE_ID`),
  KEY `FKl1jm8qymrs3laxvyawcb7mlbt` (`ADMIN_ROLE_ID`),
  CONSTRAINT `FKl1jm8qymrs3laxvyawcb7mlbt` FOREIGN KEY (`ADMIN_ROLE_ID`) REFERENCES `blc_admin_role` (`ADMIN_ROLE_ID`),
  CONSTRAINT `FKoj1ji2ummmtfdm0xb9jesi7g` FOREIGN KEY (`ADMIN_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_sec_perm_xref`
--

DROP TABLE IF EXISTS `blc_admin_sec_perm_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_sec_perm_xref` (
  `ADMIN_SECTION_ID` bigint(20) NOT NULL,
  `ADMIN_PERMISSION_ID` bigint(20) NOT NULL,
  KEY `FKns2d7kvauk8kgskridssn1gcv` (`ADMIN_PERMISSION_ID`),
  KEY `FK3k1buujb5let82ixj1k9nha3r` (`ADMIN_SECTION_ID`),
  CONSTRAINT `FK3k1buujb5let82ixj1k9nha3r` FOREIGN KEY (`ADMIN_SECTION_ID`) REFERENCES `blc_admin_section` (`ADMIN_SECTION_ID`),
  CONSTRAINT `FKns2d7kvauk8kgskridssn1gcv` FOREIGN KEY (`ADMIN_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_section`
--

DROP TABLE IF EXISTS `blc_admin_section`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_section` (
  `ADMIN_SECTION_ID` bigint(20) NOT NULL,
  `CEILING_ENTITY` varchar(255) DEFAULT NULL,
  `DISPLAY_CONTROLLER` varchar(255) DEFAULT NULL,
  `DISPLAY_ORDER` int(11) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `SECTION_KEY` varchar(255) NOT NULL,
  `URL` varchar(255) DEFAULT NULL,
  `USE_DEFAULT_HANDLER` bit(1) DEFAULT NULL,
  `ADMIN_MODULE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ADMIN_SECTION_ID`),
  UNIQUE KEY `UK_2l8u0qyluf4fwp2iiqp3p4jrn` (`SECTION_KEY`),
  KEY `ADMINSECTION_MODULE_INDEX` (`ADMIN_MODULE_ID`),
  KEY `ADMINSECTION_NAME_INDEX` (`NAME`),
  KEY `BLC_ADMIN_SECTION_ceiling_entity` (`CEILING_ENTITY`),
  KEY `BLC_ADMIN_SECTION_url` (`URL`),
  CONSTRAINT `FK2gpd1e839i00bosr6e54mdnn2` FOREIGN KEY (`ADMIN_MODULE_ID`) REFERENCES `blc_admin_module` (`ADMIN_MODULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_user`
--

DROP TABLE IF EXISTS `blc_admin_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_user` (
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  `ACTIVE_STATUS_FLAG` bit(1) DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `EMAIL` varchar(255) NOT NULL,
  `LOGIN` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `PHONE_NUMBER` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ADMIN_USER_ID`),
  KEY `ADMINPERM_EMAIL_INDEX` (`EMAIL`),
  KEY `ADMINUSER_NAME_INDEX` (`NAME`),
  KEY `BLC_ADMIN_USER_login` (`LOGIN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_user_addtl_fields`
--

DROP TABLE IF EXISTS `blc_admin_user_addtl_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_user_addtl_fields` (
  `ATTRIBUTE_ID` bigint(20) NOT NULL,
  `FIELD_NAME` varchar(255) NOT NULL,
  `FIELD_VALUE` varchar(255) DEFAULT NULL,
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ATTRIBUTE_ID`),
  KEY `ADMINUSERATTRIBUTE_INDEX` (`ADMIN_USER_ID`),
  KEY `ADMINUSERATTRIBUTE_NAME_INDEX` (`FIELD_NAME`),
  CONSTRAINT `FKiiateds21bej9b6qvrom06ayr` FOREIGN KEY (`ADMIN_USER_ID`) REFERENCES `blc_admin_user` (`ADMIN_USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_user_permission_xref`
--

DROP TABLE IF EXISTS `blc_admin_user_permission_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_user_permission_xref` (
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  `ADMIN_PERMISSION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ADMIN_PERMISSION_ID`,`ADMIN_USER_ID`),
  KEY `FKj7ms4sgplv582id7ftu4thyn3` (`ADMIN_USER_ID`),
  CONSTRAINT `FK8ia4c6mqqvm9pt1aghjbvdmtb` FOREIGN KEY (`ADMIN_PERMISSION_ID`) REFERENCES `blc_admin_permission` (`ADMIN_PERMISSION_ID`),
  CONSTRAINT `FKj7ms4sgplv582id7ftu4thyn3` FOREIGN KEY (`ADMIN_USER_ID`) REFERENCES `blc_admin_user` (`ADMIN_USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_user_role_xref`
--

DROP TABLE IF EXISTS `blc_admin_user_role_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_user_role_xref` (
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  `ADMIN_ROLE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ADMIN_ROLE_ID`,`ADMIN_USER_ID`),
  KEY `FKa48q2nut9wd1cktfjp3l2f3xv` (`ADMIN_USER_ID`),
  CONSTRAINT `FK4skhb24d5kego6i7iw4y1a448` FOREIGN KEY (`ADMIN_ROLE_ID`) REFERENCES `blc_admin_role` (`ADMIN_ROLE_ID`),
  CONSTRAINT `FKa48q2nut9wd1cktfjp3l2f3xv` FOREIGN KEY (`ADMIN_USER_ID`) REFERENCES `blc_admin_user` (`ADMIN_USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_admin_user_sandbox`
--

DROP TABLE IF EXISTS `blc_admin_user_sandbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_admin_user_sandbox` (
  `SANDBOX_ID` bigint(20) DEFAULT NULL,
  `ADMIN_USER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ADMIN_USER_ID`),
  KEY `FKehxq8fct257ml7j0rbya7ripb` (`SANDBOX_ID`),
  CONSTRAINT `FKay43c311x89bqu7lbswc7xan6` FOREIGN KEY (`ADMIN_USER_ID`) REFERENCES `blc_admin_user` (`ADMIN_USER_ID`),
  CONSTRAINT `FKehxq8fct257ml7j0rbya7ripb` FOREIGN KEY (`SANDBOX_ID`) REFERENCES `blc_sandbox` (`SANDBOX_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_asset_desc_map`
--

DROP TABLE IF EXISTS `blc_asset_desc_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_asset_desc_map` (
  `STATIC_ASSET_ID` bigint(20) NOT NULL,
  `STATIC_ASSET_DESC_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  PRIMARY KEY (`STATIC_ASSET_ID`,`MAP_KEY`),
  KEY `FKheybfelcjp3ave1pxgmjl78eo` (`STATIC_ASSET_DESC_ID`),
  CONSTRAINT `FKheybfelcjp3ave1pxgmjl78eo` FOREIGN KEY (`STATIC_ASSET_DESC_ID`) REFERENCES `blc_static_asset_desc` (`STATIC_ASSET_DESC_ID`),
  CONSTRAINT `FKhrepj8vehjv59lcn3xbiq7ays` FOREIGN KEY (`STATIC_ASSET_ID`) REFERENCES `blc_static_asset` (`STATIC_ASSET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_bank_account_payment`
--

DROP TABLE IF EXISTS `blc_bank_account_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_bank_account_payment` (
  `PAYMENT_ID` bigint(20) NOT NULL,
  `ACCOUNT_NUMBER` varchar(255) NOT NULL,
  `REFERENCE_NUMBER` varchar(255) NOT NULL,
  `ROUTING_NUMBER` varchar(255) NOT NULL,
  PRIMARY KEY (`PAYMENT_ID`),
  KEY `BANKACCOUNT_INDEX` (`REFERENCE_NUMBER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_bund_item_fee_price`
--

DROP TABLE IF EXISTS `blc_bund_item_fee_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_bund_item_fee_price` (
  `BUND_ITEM_FEE_PRICE_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,5) DEFAULT NULL,
  `IS_TAXABLE` bit(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REPORTING_CODE` varchar(255) DEFAULT NULL,
  `BUND_ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`BUND_ITEM_FEE_PRICE_ID`),
  KEY `FKmlwh6qvntrxs81h26syrft6bj` (`BUND_ORDER_ITEM_ID`),
  CONSTRAINT `FKmlwh6qvntrxs81h26syrft6bj` FOREIGN KEY (`BUND_ORDER_ITEM_ID`) REFERENCES `blc_bundle_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_bundle_order_item`
--

DROP TABLE IF EXISTS `blc_bundle_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_bundle_order_item` (
  `BASE_RETAIL_PRICE` decimal(19,5) DEFAULT NULL,
  `BASE_SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  `PRODUCT_BUNDLE_ID` bigint(20) DEFAULT NULL,
  `SKU_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`),
  KEY `FKo17ovssehxe4y3b38xjxodmrq` (`PRODUCT_BUNDLE_ID`),
  KEY `FKepomqf1wy0rsw6utuc5r7gflq` (`SKU_ID`),
  CONSTRAINT `FKepomqf1wy0rsw6utuc5r7gflq` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`),
  CONSTRAINT `FKhbcblyyh5lfrmrt1avy8wajgx` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKo17ovssehxe4y3b38xjxodmrq` FOREIGN KEY (`PRODUCT_BUNDLE_ID`) REFERENCES `blc_product_bundle` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_candidate_fg_offer`
--

DROP TABLE IF EXISTS `blc_candidate_fg_offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_candidate_fg_offer` (
  `CANDIDATE_FG_OFFER_ID` bigint(20) NOT NULL,
  `DISCOUNTED_PRICE` decimal(19,5) DEFAULT NULL,
  `FULFILLMENT_GROUP_ID` bigint(20) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CANDIDATE_FG_OFFER_ID`),
  KEY `CANDIDATE_FG_INDEX` (`FULFILLMENT_GROUP_ID`),
  KEY `CANDIDATE_FGOFFER_INDEX` (`OFFER_ID`),
  CONSTRAINT `FKg5qmns7vl5e1pu96axl8uknal` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`),
  CONSTRAINT `FKh9csft0rxeopd0s4in7qp15am` FOREIGN KEY (`FULFILLMENT_GROUP_ID`) REFERENCES `blc_fulfillment_group` (`FULFILLMENT_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_candidate_item_offer`
--

DROP TABLE IF EXISTS `blc_candidate_item_offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_candidate_item_offer` (
  `CANDIDATE_ITEM_OFFER_ID` bigint(20) NOT NULL,
  `DISCOUNTED_PRICE` decimal(19,5) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CANDIDATE_ITEM_OFFER_ID`),
  KEY `CANDIDATE_ITEMOFFER_INDEX` (`OFFER_ID`),
  KEY `CANDIDATE_ITEM_INDEX` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKb14jq3w7049s1h61pthy51m92` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKno8jmqw67ef9lpuwoumdyh7wj` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_candidate_order_offer`
--

DROP TABLE IF EXISTS `blc_candidate_order_offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_candidate_order_offer` (
  `CANDIDATE_ORDER_OFFER_ID` bigint(20) NOT NULL,
  `DISCOUNTED_PRICE` decimal(19,5) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CANDIDATE_ORDER_OFFER_ID`),
  KEY `CANDIDATE_ORDEROFFER_INDEX` (`OFFER_ID`),
  KEY `CANDIDATE_ORDER_INDEX` (`ORDER_ID`),
  CONSTRAINT `FK59se4s0394sw56c1rvdw5aepf` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`),
  CONSTRAINT `FK7ckpnvor07qankv258p1vxwww` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cat_search_facet_excl_xref`
--

DROP TABLE IF EXISTS `blc_cat_search_facet_excl_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cat_search_facet_excl_xref` (
  `CAT_EXCL_SEARCH_FACET_ID` bigint(20) NOT NULL,
  `SEQUENCE` decimal(19,2) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `SEARCH_FACET_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CAT_EXCL_SEARCH_FACET_ID`),
  KEY `FK66xu32canhiu19e6or98vufcw` (`CATEGORY_ID`),
  KEY `FKmmy51xuqakfxoflomh4dgl7on` (`SEARCH_FACET_ID`),
  CONSTRAINT `FK66xu32canhiu19e6or98vufcw` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKmmy51xuqakfxoflomh4dgl7on` FOREIGN KEY (`SEARCH_FACET_ID`) REFERENCES `blc_search_facet` (`SEARCH_FACET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cat_search_facet_xref`
--

DROP TABLE IF EXISTS `blc_cat_search_facet_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cat_search_facet_xref` (
  `CATEGORY_SEARCH_FACET_ID` bigint(20) NOT NULL,
  `SEQUENCE` decimal(19,2) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `SEARCH_FACET_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CATEGORY_SEARCH_FACET_ID`),
  KEY `FK15e8rvxxafd6h16c1ul3pynqh` (`CATEGORY_ID`),
  KEY `FK68dqudo00pmvd760r53lmcq1q` (`SEARCH_FACET_ID`),
  CONSTRAINT `FK15e8rvxxafd6h16c1ul3pynqh` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FK68dqudo00pmvd760r53lmcq1q` FOREIGN KEY (`SEARCH_FACET_ID`) REFERENCES `blc_search_facet` (`SEARCH_FACET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cat_site_map_gen_cfg`
--

DROP TABLE IF EXISTS `blc_cat_site_map_gen_cfg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cat_site_map_gen_cfg` (
  `ENDING_DEPTH` int(11) NOT NULL,
  `STARTING_DEPTH` int(11) NOT NULL,
  `GEN_CONFIG_ID` bigint(20) NOT NULL,
  `ROOT_CATEGORY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`GEN_CONFIG_ID`),
  KEY `FKerl6k0300vd4y8haxljr92rmo` (`ROOT_CATEGORY_ID`),
  CONSTRAINT `FKerl6k0300vd4y8haxljr92rmo` FOREIGN KEY (`ROOT_CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKn5liq0ue5rtn6h7bmsv7q85nn` FOREIGN KEY (`GEN_CONFIG_ID`) REFERENCES `blc_site_map_gen_cfg` (`GEN_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_catalog`
--

DROP TABLE IF EXISTS `blc_catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_catalog` (
  `CATALOG_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CATALOG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_category`
--

DROP TABLE IF EXISTS `blc_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_category` (
  `CATEGORY_ID` bigint(20) NOT NULL,
  `ACTIVE_END_DATE` datetime DEFAULT NULL,
  `ACTIVE_START_DATE` datetime DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `DISPLAY_TEMPLATE` varchar(255) DEFAULT NULL,
  `EXTERNAL_ID` varchar(255) DEFAULT NULL,
  `FULFILLMENT_TYPE` varchar(255) DEFAULT NULL,
  `INVENTORY_TYPE` varchar(255) DEFAULT NULL,
  `LONG_DESCRIPTION` longtext,
  `META_DESC` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `OVERRIDE_GENERATED_URL` bit(1) DEFAULT NULL,
  `PRODUCT_DESC_PATTERN_OVERRIDE` varchar(255) DEFAULT NULL,
  `PRODUCT_TITLE_PATTERN_OVERRIDE` varchar(255) DEFAULT NULL,
  `ROOT_DISPLAY_ORDER` decimal(10,6) DEFAULT NULL,
  `TAX_CODE` varchar(255) DEFAULT NULL,
  `URL` varchar(255) DEFAULT NULL,
  `URL_KEY` varchar(255) DEFAULT NULL,
  `DEFAULT_PARENT_CATEGORY_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CATEGORY_ID`),
  KEY `CATEGORY_PARENT_INDEX` (`DEFAULT_PARENT_CATEGORY_ID`),
  KEY `CATEGORY_E_ID_INDEX` (`EXTERNAL_ID`),
  KEY `CATEGORY_NAME_INDEX` (`NAME`),
  KEY `CATEGORY_URL_INDEX` (`URL`),
  KEY `CATEGORY_URLKEY_INDEX` (`URL_KEY`),
  CONSTRAINT `FK6lf7a3qgmh5m8aq8o8url408t` FOREIGN KEY (`DEFAULT_PARENT_CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_category_attribute`
--

DROP TABLE IF EXISTS `blc_category_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_category_attribute` (
  `CATEGORY_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CATEGORY_ATTRIBUTE_ID`),
  KEY `CATEGORYATTRIBUTE_INDEX` (`CATEGORY_ID`),
  KEY `CATEGORYATTRIBUTE_NAME_INDEX` (`NAME`),
  CONSTRAINT `FKhkechh91jg8iog16ry7089anf` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_category_media_map`
--

DROP TABLE IF EXISTS `blc_category_media_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_category_media_map` (
  `CATEGORY_MEDIA_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  `BLC_CATEGORY_CATEGORY_ID` bigint(20) NOT NULL,
  `MEDIA_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`CATEGORY_MEDIA_ID`),
  KEY `FKfi64uesmjfu96gc0i4urxyf12` (`BLC_CATEGORY_CATEGORY_ID`),
  KEY `FKel78nfydgqxta46k7uvsh5q3x` (`MEDIA_ID`),
  CONSTRAINT `FKel78nfydgqxta46k7uvsh5q3x` FOREIGN KEY (`MEDIA_ID`) REFERENCES `blc_media` (`MEDIA_ID`),
  CONSTRAINT `FKfi64uesmjfu96gc0i4urxyf12` FOREIGN KEY (`BLC_CATEGORY_CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_category_product_xref`
--

DROP TABLE IF EXISTS `blc_category_product_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_category_product_xref` (
  `CATEGORY_PRODUCT_ID` bigint(20) NOT NULL,
  `DEFAULT_REFERENCE` bit(1) DEFAULT NULL,
  `DISPLAY_ORDER` decimal(10,6) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) NOT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CATEGORY_PRODUCT_ID`),
  KEY `FKj8gn00lwi7fih0ueqwdat589e` (`CATEGORY_ID`),
  KEY `FKnwoet42m887na9hjfvqqgr58v` (`PRODUCT_ID`),
  CONSTRAINT `FKj8gn00lwi7fih0ueqwdat589e` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKnwoet42m887na9hjfvqqgr58v` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_category_xref`
--

DROP TABLE IF EXISTS `blc_category_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_category_xref` (
  `CATEGORY_XREF_ID` bigint(20) NOT NULL,
  `DEFAULT_REFERENCE` bit(1) DEFAULT NULL,
  `DISPLAY_ORDER` decimal(10,6) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) NOT NULL,
  `SUB_CATEGORY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CATEGORY_XREF_ID`),
  KEY `FKe9p1rqdyircs0atgu7e5xlwmx` (`CATEGORY_ID`),
  KEY `FKgrlcy8qrn7lqyiou3vu1piuk1` (`SUB_CATEGORY_ID`),
  CONSTRAINT `FKe9p1rqdyircs0atgu7e5xlwmx` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKgrlcy8qrn7lqyiou3vu1piuk1` FOREIGN KEY (`SUB_CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_challenge_question`
--

DROP TABLE IF EXISTS `blc_challenge_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_challenge_question` (
  `QUESTION_ID` bigint(20) NOT NULL,
  `QUESTION` varchar(255) NOT NULL,
  PRIMARY KEY (`QUESTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cms_menu`
--

DROP TABLE IF EXISTS `blc_cms_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cms_menu` (
  `MENU_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`MENU_ID`),
  KEY `BLC_CMS_MENU_name` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cms_menu_item`
--

DROP TABLE IF EXISTS `blc_cms_menu_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cms_menu_item` (
  `MENU_ITEM_ID` bigint(20) NOT NULL,
  `ACTION_URL` varchar(255) DEFAULT NULL,
  `ALT_TEXT` varchar(255) DEFAULT NULL,
  `CUSTOM_HTML` longtext,
  `IMAGE_URL` varchar(255) DEFAULT NULL,
  `LABEL` varchar(255) DEFAULT NULL,
  `SEQUENCE` decimal(10,6) DEFAULT NULL,
  `MENU_ITEM_TYPE` varchar(255) DEFAULT NULL,
  `LINKED_MENU_ID` bigint(20) DEFAULT NULL,
  `LINKED_PAGE_ID` bigint(20) DEFAULT NULL,
  `PARENT_MENU_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`MENU_ITEM_ID`),
  KEY `FKsfd7p9istk4908bchapktbnr0` (`LINKED_MENU_ID`),
  KEY `FKbgy5higr7beta0sxdqvkm9k7r` (`LINKED_PAGE_ID`),
  KEY `FKa7ogt4huutal1mirsufnmy9lr` (`PARENT_MENU_ID`),
  CONSTRAINT `FKa7ogt4huutal1mirsufnmy9lr` FOREIGN KEY (`PARENT_MENU_ID`) REFERENCES `blc_cms_menu` (`MENU_ID`),
  CONSTRAINT `FKbgy5higr7beta0sxdqvkm9k7r` FOREIGN KEY (`LINKED_PAGE_ID`) REFERENCES `blc_page` (`PAGE_ID`),
  CONSTRAINT `FKsfd7p9istk4908bchapktbnr0` FOREIGN KEY (`LINKED_MENU_ID`) REFERENCES `blc_cms_menu` (`MENU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_code_types`
--

DROP TABLE IF EXISTS `blc_code_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_code_types` (
  `CODE_ID` bigint(20) NOT NULL,
  `CODE_TYPE` varchar(255) NOT NULL,
  `CODE_DESC` varchar(255) DEFAULT NULL,
  `CODE_KEY` varchar(255) NOT NULL,
  `MODIFIABLE` char(1) DEFAULT NULL,
  PRIMARY KEY (`CODE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_country`
--

DROP TABLE IF EXISTS `blc_country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_country` (
  `ABBREVIATION` varchar(255) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ABBREVIATION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_country_sub`
--

DROP TABLE IF EXISTS `blc_country_sub`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_country_sub` (
  `ABBREVIATION` varchar(255) NOT NULL,
  `ALT_ABBREVIATION` varchar(255) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `COUNTRY_SUB_CAT` bigint(20) DEFAULT NULL,
  `COUNTRY` varchar(255) NOT NULL,
  PRIMARY KEY (`ABBREVIATION`),
  KEY `COUNTRY_SUB_ALT_ABRV_IDX` (`ALT_ABBREVIATION`),
  KEY `COUNTRY_SUB_NAME_IDX` (`NAME`),
  KEY `FKtjleoo0nukky2den29i7mlx0c` (`COUNTRY_SUB_CAT`),
  KEY `FKapicr4ced87ut6dfyh5fuway8` (`COUNTRY`),
  CONSTRAINT `FKapicr4ced87ut6dfyh5fuway8` FOREIGN KEY (`COUNTRY`) REFERENCES `blc_country` (`ABBREVIATION`),
  CONSTRAINT `FKtjleoo0nukky2den29i7mlx0c` FOREIGN KEY (`COUNTRY_SUB_CAT`) REFERENCES `blc_country_sub_cat` (`COUNTRY_SUB_CAT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_country_sub_cat`
--

DROP TABLE IF EXISTS `blc_country_sub_cat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_country_sub_cat` (
  `COUNTRY_SUB_CAT_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`COUNTRY_SUB_CAT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_credit_card_payment`
--

DROP TABLE IF EXISTS `blc_credit_card_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_credit_card_payment` (
  `PAYMENT_ID` bigint(20) NOT NULL,
  `EXPIRATION_MONTH` int(11) NOT NULL,
  `EXPIRATION_YEAR` int(11) NOT NULL,
  `NAME_ON_CARD` varchar(255) NOT NULL,
  `PAN` varchar(255) NOT NULL,
  `REFERENCE_NUMBER` varchar(255) NOT NULL,
  PRIMARY KEY (`PAYMENT_ID`),
  KEY `CREDITCARD_INDEX` (`REFERENCE_NUMBER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_currency`
--

DROP TABLE IF EXISTS `blc_currency`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_currency` (
  `CURRENCY_CODE` varchar(255) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DEFAULT_FLAG` bit(1) DEFAULT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CURRENCY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_cust_site_map_gen_cfg`
--

DROP TABLE IF EXISTS `blc_cust_site_map_gen_cfg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_cust_site_map_gen_cfg` (
  `GEN_CONFIG_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`GEN_CONFIG_ID`),
  CONSTRAINT `FKs5s4vmpbxh4edqjtbted9gxmw` FOREIGN KEY (`GEN_CONFIG_ID`) REFERENCES `blc_site_map_gen_cfg` (`GEN_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer`
--

DROP TABLE IF EXISTS `blc_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer` (
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `CHALLENGE_ANSWER` varchar(255) DEFAULT NULL,
  `DEACTIVATED` bit(1) DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `EXTERNAL_ID` varchar(255) DEFAULT NULL,
  `FIRST_NAME` varchar(255) DEFAULT NULL,
  `IS_TAX_EXEMPT` bit(1) DEFAULT NULL,
  `LAST_NAME` varchar(255) DEFAULT NULL,
  `PASSWORD` varchar(255) DEFAULT NULL,
  `PASSWORD_CHANGE_REQUIRED` bit(1) DEFAULT NULL,
  `IS_PREVIEW` bit(1) DEFAULT NULL,
  `RECEIVE_EMAIL` bit(1) DEFAULT NULL,
  `IS_REGISTERED` bit(1) DEFAULT NULL,
  `TAX_EXEMPTION_CODE` varchar(255) DEFAULT NULL,
  `USER_NAME` varchar(255) DEFAULT NULL,
  `CHALLENGE_QUESTION_ID` bigint(20) DEFAULT NULL,
  `LOCALE_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`CUSTOMER_ID`),
  KEY `CUSTOMER_CHALLENGE_INDEX` (`CHALLENGE_QUESTION_ID`),
  KEY `CUSTOMER_EMAIL_INDEX` (`EMAIL_ADDRESS`),
  KEY `FK4utjhbg9600iwhb05m40wspj1` (`LOCALE_CODE`),
  KEY `BLC_CUSTOMER_user_name` (`USER_NAME`),
  CONSTRAINT `FK4utjhbg9600iwhb05m40wspj1` FOREIGN KEY (`LOCALE_CODE`) REFERENCES `blc_locale` (`LOCALE_CODE`),
  CONSTRAINT `FKsgsex6rdheq2nm6pl23gggtqs` FOREIGN KEY (`CHALLENGE_QUESTION_ID`) REFERENCES `blc_challenge_question` (`QUESTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_address`
--

DROP TABLE IF EXISTS `blc_customer_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_address` (
  `CUSTOMER_ADDRESS_ID` bigint(20) NOT NULL,
  `ADDRESS_NAME` varchar(255) DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `ADDRESS_ID` bigint(20) NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_ADDRESS_ID`),
  KEY `CUSTOMERADDRESS_ADDRESS_INDEX` (`ADDRESS_ID`),
  KEY `FKrpdahw86mewf46g63nitq0w9p` (`CUSTOMER_ID`),
  CONSTRAINT `FKn79uhm41n1b23e6brajb4ggpw` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`),
  CONSTRAINT `FKrpdahw86mewf46g63nitq0w9p` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_attribute`
--

DROP TABLE IF EXISTS `blc_customer_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_attribute` (
  `CUSTOMER_ATTR_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_ATTR_ID`),
  KEY `FKo7j035lp80xu9wncbv96a1ry0` (`CUSTOMER_ID`),
  CONSTRAINT `FKo7j035lp80xu9wncbv96a1ry0` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_offer_xref`
--

DROP TABLE IF EXISTS `blc_customer_offer_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_offer_xref` (
  `CUSTOMER_OFFER_ID` bigint(20) NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_OFFER_ID`),
  KEY `CUSTOFFER_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `CUSTOFFER_OFFER_INDEX` (`OFFER_ID`),
  CONSTRAINT `FKg81dq5yxrtsy6cjivd0afkxcj` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKrks1nkejqmm3n7y4xo5rs6wuk` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_password_token`
--

DROP TABLE IF EXISTS `blc_customer_password_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_password_token` (
  `PASSWORD_TOKEN` varchar(255) NOT NULL,
  `CREATE_DATE` datetime NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `TOKEN_USED_DATE` datetime DEFAULT NULL,
  `TOKEN_USED_FLAG` bit(1) NOT NULL,
  PRIMARY KEY (`PASSWORD_TOKEN`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_payment`
--

DROP TABLE IF EXISTS `blc_customer_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_payment` (
  `CUSTOMER_PAYMENT_ID` bigint(20) NOT NULL,
  `IS_DEFAULT` bit(1) DEFAULT NULL,
  `GATEWAY_TYPE` varchar(255) DEFAULT NULL,
  `PAYMENT_TOKEN` varchar(255) DEFAULT NULL,
  `PAYMENT_TYPE` varchar(255) DEFAULT NULL,
  `ADDRESS_ID` bigint(20) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_PAYMENT_ID`),
  UNIQUE KEY `CSTMR_PAY_UNIQUE_CNSTRNT` (`CUSTOMER_ID`,`PAYMENT_TOKEN`),
  KEY `CUSTOMERPAYMENT_TYPE_INDEX` (`PAYMENT_TYPE`),
  KEY `FKouuqjxsn30esr7ftg7i5mmr4p` (`ADDRESS_ID`),
  CONSTRAINT `FKhd53v4ilet9h8jxbjh7m2k7yj` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKouuqjxsn30esr7ftg7i5mmr4p` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_payment_fields`
--

DROP TABLE IF EXISTS `blc_customer_payment_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_payment_fields` (
  `CUSTOMER_PAYMENT_ID` bigint(20) NOT NULL,
  `FIELD_VALUE` longtext,
  `FIELD_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`CUSTOMER_PAYMENT_ID`,`FIELD_NAME`),
  CONSTRAINT `FKpwpbmvuo4pd8y76dswmq4cr00` FOREIGN KEY (`CUSTOMER_PAYMENT_ID`) REFERENCES `blc_customer_payment` (`CUSTOMER_PAYMENT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_phone`
--

DROP TABLE IF EXISTS `blc_customer_phone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_phone` (
  `CUSTOMER_PHONE_ID` bigint(20) NOT NULL,
  `PHONE_NAME` varchar(255) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `PHONE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_PHONE_ID`),
  UNIQUE KEY `CSTMR_PHONE_UNIQUE_CNSTRNT` (`CUSTOMER_ID`,`PHONE_NAME`),
  KEY `CUSTPHONE_PHONE_INDEX` (`PHONE_ID`),
  CONSTRAINT `FK1uy5spxqx6kxiqnvg5la7bjbb` FOREIGN KEY (`PHONE_ID`) REFERENCES `blc_phone` (`PHONE_ID`),
  CONSTRAINT `FK4sg479sv9t1dj7b1pso158tr8` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_customer_role`
--

DROP TABLE IF EXISTS `blc_customer_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_customer_role` (
  `CUSTOMER_ROLE_ID` bigint(20) NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `ROLE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CUSTOMER_ROLE_ID`),
  KEY `CUSTROLE_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `CUSTROLE_ROLE_INDEX` (`ROLE_ID`),
  CONSTRAINT `FKqcnikrg70t86oju6xf6622f5x` FOREIGN KEY (`ROLE_ID`) REFERENCES `blc_role` (`ROLE_ID`),
  CONSTRAINT `FKsqxeay9un5o0l77mrtdgjxps4` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_data_drvn_enum`
--

DROP TABLE IF EXISTS `blc_data_drvn_enum`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_data_drvn_enum` (
  `ENUM_ID` bigint(20) NOT NULL,
  `ENUM_KEY` varchar(255) DEFAULT NULL,
  `MODIFIABLE` bit(1) DEFAULT NULL,
  PRIMARY KEY (`ENUM_ID`),
  KEY `ENUM_KEY_INDEX` (`ENUM_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_data_drvn_enum_val`
--

DROP TABLE IF EXISTS `blc_data_drvn_enum_val`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_data_drvn_enum_val` (
  `ENUM_VAL_ID` bigint(20) NOT NULL,
  `DISPLAY` varchar(255) DEFAULT NULL,
  `HIDDEN` bit(1) DEFAULT NULL,
  `ENUM_KEY` varchar(255) DEFAULT NULL,
  `ENUM_TYPE` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ENUM_VAL_ID`),
  KEY `HIDDEN_INDEX` (`HIDDEN`),
  KEY `ENUM_VAL_KEY_INDEX` (`ENUM_KEY`),
  KEY `FKq180xbmiqw22rrc9kf0qokaea` (`ENUM_TYPE`),
  CONSTRAINT `FKq180xbmiqw22rrc9kf0qokaea` FOREIGN KEY (`ENUM_TYPE`) REFERENCES `blc_data_drvn_enum` (`ENUM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_disc_item_fee_price`
--

DROP TABLE IF EXISTS `blc_disc_item_fee_price`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_disc_item_fee_price` (
  `DISC_ITEM_FEE_PRICE_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,5) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REPORTING_CODE` varchar(255) DEFAULT NULL,
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`DISC_ITEM_FEE_PRICE_ID`),
  KEY `FK70ocmaswx7p3xymfvildubx5` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK70ocmaswx7p3xymfvildubx5` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_discrete_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_discrete_order_item`
--

DROP TABLE IF EXISTS `blc_discrete_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_discrete_order_item` (
  `BASE_RETAIL_PRICE` decimal(19,5) DEFAULT NULL,
  `BASE_SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  `BUNDLE_ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  `SKU_ID` bigint(20) NOT NULL,
  `SKU_BUNDLE_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`),
  KEY `DISCRETE_PRODUCT_INDEX` (`PRODUCT_ID`),
  KEY `DISCRETE_SKU_INDEX` (`SKU_ID`),
  KEY `FKpu94j8xpk9uwpcgcy98ktle06` (`BUNDLE_ORDER_ITEM_ID`),
  KEY `FK1micyx881c06d24amsg3sk2he` (`SKU_BUNDLE_ITEM_ID`),
  CONSTRAINT `FK188b985egh16qfcjt8kv1asa4` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK1micyx881c06d24amsg3sk2he` FOREIGN KEY (`SKU_BUNDLE_ITEM_ID`) REFERENCES `blc_sku_bundle_item` (`SKU_BUNDLE_ITEM_ID`),
  CONSTRAINT `FK2moe4tjwke365lo2s5qgmacx7` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`),
  CONSTRAINT `FKmtcs7ax8jo2hy1ae4caafsfkp` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`),
  CONSTRAINT `FKpu94j8xpk9uwpcgcy98ktle06` FOREIGN KEY (`BUNDLE_ORDER_ITEM_ID`) REFERENCES `blc_bundle_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_dyn_discrete_order_item`
--

DROP TABLE IF EXISTS `blc_dyn_discrete_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_dyn_discrete_order_item` (
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`),
  CONSTRAINT `FKhv263skp3pgb4wcxg44umwcjs` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_discrete_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_email_tracking`
--

DROP TABLE IF EXISTS `blc_email_tracking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_email_tracking` (
  `EMAIL_TRACKING_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `DATE_SENT` datetime DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`EMAIL_TRACKING_ID`),
  KEY `EMAILTRACKING_INDEX` (`EMAIL_ADDRESS`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_email_tracking_clicks`
--

DROP TABLE IF EXISTS `blc_email_tracking_clicks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_email_tracking_clicks` (
  `CLICK_ID` bigint(20) NOT NULL,
  `CUSTOMER_ID` varchar(255) DEFAULT NULL,
  `DATE_CLICKED` datetime NOT NULL,
  `DESTINATION_URI` varchar(255) DEFAULT NULL,
  `QUERY_STRING` varchar(255) DEFAULT NULL,
  `EMAIL_TRACKING_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CLICK_ID`),
  KEY `TRACKINGCLICKS_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `TRACKINGCLICKS_TRACKING_INDEX` (`EMAIL_TRACKING_ID`),
  CONSTRAINT `FK3jed270645ahspuibr8wau0po` FOREIGN KEY (`EMAIL_TRACKING_ID`) REFERENCES `blc_email_tracking` (`EMAIL_TRACKING_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_email_tracking_opens`
--

DROP TABLE IF EXISTS `blc_email_tracking_opens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_email_tracking_opens` (
  `OPEN_ID` bigint(20) NOT NULL,
  `DATE_OPENED` datetime DEFAULT NULL,
  `USER_AGENT` varchar(255) DEFAULT NULL,
  `EMAIL_TRACKING_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`OPEN_ID`),
  KEY `TRACKINGOPEN_TRACKING` (`EMAIL_TRACKING_ID`),
  CONSTRAINT `FKt6fi06g4y7riiqeyuhb0t659n` FOREIGN KEY (`EMAIL_TRACKING_ID`) REFERENCES `blc_email_tracking` (`EMAIL_TRACKING_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fg_adjustment`
--

DROP TABLE IF EXISTS `blc_fg_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fg_adjustment` (
  `FG_ADJUSTMENT_ID` bigint(20) NOT NULL,
  `ADJUSTMENT_REASON` varchar(255) NOT NULL,
  `ADJUSTMENT_VALUE` decimal(19,5) NOT NULL,
  `FULFILLMENT_GROUP_ID` bigint(20) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`FG_ADJUSTMENT_ID`),
  KEY `FGADJUSTMENT_INDEX` (`FULFILLMENT_GROUP_ID`),
  KEY `FGADJUSTMENT_OFFER_INDEX` (`OFFER_ID`),
  CONSTRAINT `FK2ceuqqy88te84f61f0n7kvaw1` FOREIGN KEY (`FULFILLMENT_GROUP_ID`) REFERENCES `blc_fulfillment_group` (`FULFILLMENT_GROUP_ID`),
  CONSTRAINT `FKt0l1mgyccsuq76n8b0b6pc9a9` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fg_fee_tax_xref`
--

DROP TABLE IF EXISTS `blc_fg_fee_tax_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fg_fee_tax_xref` (
  `FULFILLMENT_GROUP_FEE_ID` bigint(20) NOT NULL,
  `TAX_DETAIL_ID` bigint(20) NOT NULL,
  UNIQUE KEY `UK_59ow3plvbkxjfs57k92ahf3eg` (`TAX_DETAIL_ID`),
  KEY `FK2t3oa9322dqgya6r27pb2bcsd` (`FULFILLMENT_GROUP_FEE_ID`),
  CONSTRAINT `FK1aueplsngm018mlqqq9yhgrn6` FOREIGN KEY (`TAX_DETAIL_ID`) REFERENCES `blc_tax_detail` (`TAX_DETAIL_ID`),
  CONSTRAINT `FK2t3oa9322dqgya6r27pb2bcsd` FOREIGN KEY (`FULFILLMENT_GROUP_FEE_ID`) REFERENCES `blc_fulfillment_group_fee` (`FULFILLMENT_GROUP_FEE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fg_fg_tax_xref`
--

DROP TABLE IF EXISTS `blc_fg_fg_tax_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fg_fg_tax_xref` (
  `FULFILLMENT_GROUP_ID` bigint(20) NOT NULL,
  `TAX_DETAIL_ID` bigint(20) NOT NULL,
  UNIQUE KEY `UK_57834q276cjrrnwjj1ilnj6ve` (`TAX_DETAIL_ID`),
  KEY `FKnah3gdurbtogb0s9sf3humt14` (`FULFILLMENT_GROUP_ID`),
  CONSTRAINT `FKla7cgvy244irmood3xt8rpsjb` FOREIGN KEY (`TAX_DETAIL_ID`) REFERENCES `blc_tax_detail` (`TAX_DETAIL_ID`),
  CONSTRAINT `FKnah3gdurbtogb0s9sf3humt14` FOREIGN KEY (`FULFILLMENT_GROUP_ID`) REFERENCES `blc_fulfillment_group` (`FULFILLMENT_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fg_item_tax_xref`
--

DROP TABLE IF EXISTS `blc_fg_item_tax_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fg_item_tax_xref` (
  `FULFILLMENT_GROUP_ITEM_ID` bigint(20) NOT NULL,
  `TAX_DETAIL_ID` bigint(20) NOT NULL,
  UNIQUE KEY `UK_hs9yvwvlwdy668hf186rgfyvq` (`TAX_DETAIL_ID`),
  KEY `FKl5kovj2ayfp7idroml0qjwan3` (`FULFILLMENT_GROUP_ITEM_ID`),
  CONSTRAINT `FKb5rnxtly8pr3ihvlrxlovnjkb` FOREIGN KEY (`TAX_DETAIL_ID`) REFERENCES `blc_tax_detail` (`TAX_DETAIL_ID`),
  CONSTRAINT `FKl5kovj2ayfp7idroml0qjwan3` FOREIGN KEY (`FULFILLMENT_GROUP_ITEM_ID`) REFERENCES `blc_fulfillment_group_item` (`FULFILLMENT_GROUP_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_field`
--

DROP TABLE IF EXISTS `blc_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_field` (
  `FIELD_ID` bigint(20) NOT NULL,
  `ABBREVIATION` varchar(255) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `ENTITY_TYPE` varchar(255) NOT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  `OVERRIDE_GENERATED_PROP_NAME` bit(1) DEFAULT NULL,
  `PROPERTY_NAME` varchar(255) NOT NULL,
  `TRANSLATABLE` bit(1) DEFAULT NULL,
  PRIMARY KEY (`FIELD_ID`),
  KEY `ENTITY_TYPE_INDEX` (`ENTITY_TYPE`),
  KEY `BLC_FIELD_abbreviation` (`ABBREVIATION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fld_def`
--

DROP TABLE IF EXISTS `blc_fld_def`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fld_def` (
  `FLD_DEF_ID` bigint(20) NOT NULL,
  `ALLOW_MULTIPLES` bit(1) DEFAULT NULL,
  `COLUMN_WIDTH` varchar(255) DEFAULT NULL,
  `FLD_ORDER` int(11) DEFAULT NULL,
  `FLD_TYPE` varchar(255) DEFAULT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  `HELP_TEXT` varchar(255) DEFAULT NULL,
  `HIDDEN_FLAG` bit(1) DEFAULT NULL,
  `HINT` varchar(255) DEFAULT NULL,
  `MAX_LENGTH` int(11) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REQUIRED_FLAG` bit(1) DEFAULT NULL,
  `SECURITY_LEVEL` varchar(255) DEFAULT NULL,
  `TEXT_AREA_FLAG` bit(1) DEFAULT NULL,
  `TOOLTIP` varchar(255) DEFAULT NULL,
  `VLDTN_ERROR_MSSG_KEY` varchar(255) DEFAULT NULL,
  `VLDTN_REGEX` varchar(255) DEFAULT NULL,
  `ENUM_ID` bigint(20) DEFAULT NULL,
  `FLD_GROUP_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FLD_DEF_ID`),
  KEY `FK3p9sauu111ycv4gbk6tymcj9e` (`ENUM_ID`),
  KEY `FKcqfi7hxwka5y9sqqoiolsnssr` (`FLD_GROUP_ID`),
  CONSTRAINT `FK3p9sauu111ycv4gbk6tymcj9e` FOREIGN KEY (`ENUM_ID`) REFERENCES `blc_data_drvn_enum` (`ENUM_ID`),
  CONSTRAINT `FKcqfi7hxwka5y9sqqoiolsnssr` FOREIGN KEY (`FLD_GROUP_ID`) REFERENCES `blc_fld_group` (`FLD_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fld_enum`
--

DROP TABLE IF EXISTS `blc_fld_enum`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fld_enum` (
  `FLD_ENUM_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FLD_ENUM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fld_enum_item`
--

DROP TABLE IF EXISTS `blc_fld_enum_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fld_enum_item` (
  `FLD_ENUM_ITEM_ID` bigint(20) NOT NULL,
  `FLD_ORDER` int(11) DEFAULT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `FLD_ENUM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FLD_ENUM_ITEM_ID`),
  KEY `FKjs87xrnu40vfq9uwdi9lv05lr` (`FLD_ENUM_ID`),
  CONSTRAINT `FKjs87xrnu40vfq9uwdi9lv05lr` FOREIGN KEY (`FLD_ENUM_ID`) REFERENCES `blc_fld_enum` (`FLD_ENUM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fld_group`
--

DROP TABLE IF EXISTS `blc_fld_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fld_group` (
  `FLD_GROUP_ID` bigint(20) NOT NULL,
  `INIT_COLLAPSED_FLAG` bit(1) DEFAULT NULL,
  `IS_MASTER_FIELD_GROUP` bit(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FLD_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_group`
--

DROP TABLE IF EXISTS `blc_fulfillment_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_group` (
  `FULFILLMENT_GROUP_ID` bigint(20) NOT NULL,
  `DELIVERY_INSTRUCTION` varchar(255) DEFAULT NULL,
  `PRICE` decimal(19,5) DEFAULT NULL,
  `SHIPPING_PRICE_TAXABLE` bit(1) DEFAULT NULL,
  `MERCHANDISE_TOTAL` decimal(19,5) DEFAULT NULL,
  `METHOD` varchar(255) DEFAULT NULL,
  `IS_PRIMARY` bit(1) DEFAULT NULL,
  `REFERENCE_NUMBER` varchar(255) DEFAULT NULL,
  `RETAIL_PRICE` decimal(19,5) DEFAULT NULL,
  `SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `FULFILLMENT_GROUP_SEQUNCE` int(11) DEFAULT NULL,
  `SERVICE` varchar(255) DEFAULT NULL,
  `SHIPPING_OVERRIDE` bit(1) DEFAULT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  `TOTAL` decimal(19,5) DEFAULT NULL,
  `TOTAL_FEE_TAX` decimal(19,5) DEFAULT NULL,
  `TOTAL_FG_TAX` decimal(19,5) DEFAULT NULL,
  `TOTAL_ITEM_TAX` decimal(19,5) DEFAULT NULL,
  `TOTAL_TAX` decimal(19,5) DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  `ADDRESS_ID` bigint(20) DEFAULT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) DEFAULT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  `PERSONAL_MESSAGE_ID` bigint(20) DEFAULT NULL,
  `PHONE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FULFILLMENT_GROUP_ID`),
  KEY `FG_ADDRESS_INDEX` (`ADDRESS_ID`),
  KEY `FG_METHOD_INDEX` (`METHOD`),
  KEY `FG_ORDER_INDEX` (`ORDER_ID`),
  KEY `FG_MESSAGE_INDEX` (`PERSONAL_MESSAGE_ID`),
  KEY `FG_PHONE_INDEX` (`PHONE_ID`),
  KEY `FG_PRIMARY_INDEX` (`IS_PRIMARY`),
  KEY `FG_REFERENCE_INDEX` (`REFERENCE_NUMBER`),
  KEY `FG_SERVICE_INDEX` (`SERVICE`),
  KEY `FG_STATUS_INDEX` (`STATUS`),
  KEY `FK336lsxych2j78fsd12dxacn7n` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FK336lsxych2j78fsd12dxacn7n` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FK3lis2v456tgcmagt1tkdummdi` FOREIGN KEY (`PHONE_ID`) REFERENCES `blc_phone` (`PHONE_ID`),
  CONSTRAINT `FK44mielsxkxtt1ndfiat2wj9po` FOREIGN KEY (`PERSONAL_MESSAGE_ID`) REFERENCES `blc_personal_message` (`PERSONAL_MESSAGE_ID`),
  CONSTRAINT `FKavpobeg9yjr9k3wtycirv5i8a` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`),
  CONSTRAINT `FKbtadc11h6ysb0fbyq2bsegum7` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_group_fee`
--

DROP TABLE IF EXISTS `blc_fulfillment_group_fee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_group_fee` (
  `FULFILLMENT_GROUP_FEE_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,5) DEFAULT NULL,
  `FEE_TAXABLE_FLAG` bit(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REPORTING_CODE` varchar(255) DEFAULT NULL,
  `TOTAL_FEE_TAX` decimal(19,5) DEFAULT NULL,
  `FULFILLMENT_GROUP_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`FULFILLMENT_GROUP_FEE_ID`),
  KEY `FKss79152pprx7xdwjkmelwf4xo` (`FULFILLMENT_GROUP_ID`),
  CONSTRAINT `FKss79152pprx7xdwjkmelwf4xo` FOREIGN KEY (`FULFILLMENT_GROUP_ID`) REFERENCES `blc_fulfillment_group` (`FULFILLMENT_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_group_item`
--

DROP TABLE IF EXISTS `blc_fulfillment_group_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_group_item` (
  `FULFILLMENT_GROUP_ITEM_ID` bigint(20) NOT NULL,
  `PRORATED_ORDER_ADJ` decimal(19,2) DEFAULT NULL,
  `QUANTITY` int(11) NOT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  `TOTAL_ITEM_AMOUNT` decimal(19,5) DEFAULT NULL,
  `TOTAL_ITEM_TAXABLE_AMOUNT` decimal(19,5) DEFAULT NULL,
  `TOTAL_ITEM_TAX` decimal(19,5) DEFAULT NULL,
  `FULFILLMENT_GROUP_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`FULFILLMENT_GROUP_ITEM_ID`),
  KEY `FGITEM_FG_INDEX` (`FULFILLMENT_GROUP_ID`),
  KEY `FGITEM_STATUS_INDEX` (`STATUS`),
  KEY `FKmra6tj092ugw58xhvvi43pdb2` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKmra6tj092ugw58xhvvi43pdb2` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKqfqxv2f0ita9ou48jpi7c3wi9` FOREIGN KEY (`FULFILLMENT_GROUP_ID`) REFERENCES `blc_fulfillment_group` (`FULFILLMENT_GROUP_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_opt_banded_prc`
--

DROP TABLE IF EXISTS `blc_fulfillment_opt_banded_prc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_opt_banded_prc` (
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FKsf9j5pdg9lo5e7xhasqn61m0y` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_opt_banded_wgt`
--

DROP TABLE IF EXISTS `blc_fulfillment_opt_banded_wgt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_opt_banded_wgt` (
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FKsarbwhn57dgx7kt1es3ny384n` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_option`
--

DROP TABLE IF EXISTS `blc_fulfillment_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_option` (
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  `FULFILLMENT_TYPE` varchar(255) NOT NULL,
  `LONG_DESCRIPTION` longtext,
  `NAME` varchar(255) DEFAULT NULL,
  `TAX_CODE` varchar(255) DEFAULT NULL,
  `TAXABLE` bit(1) DEFAULT NULL,
  `USE_FLAT_RATES` bit(1) DEFAULT NULL,
  PRIMARY KEY (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_option_fixed`
--

DROP TABLE IF EXISTS `blc_fulfillment_option_fixed`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_option_fixed` (
  `PRICE` decimal(19,5) NOT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`FULFILLMENT_OPTION_ID`),
  KEY `FKj5n6w6q7dk09k6ayif4g5t0t3` (`CURRENCY_CODE`),
  CONSTRAINT `FKj5n6w6q7dk09k6ayif4g5t0t3` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`),
  CONSTRAINT `FKl96yhpl4w0989nil2g6v2t3kq` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_price_band`
--

DROP TABLE IF EXISTS `blc_fulfillment_price_band`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_price_band` (
  `FULFILLMENT_PRICE_BAND_ID` bigint(20) NOT NULL,
  `RESULT_AMOUNT` decimal(19,5) NOT NULL,
  `RESULT_AMOUNT_TYPE` varchar(255) NOT NULL,
  `RETAIL_PRICE_MINIMUM_AMOUNT` decimal(19,5) NOT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FULFILLMENT_PRICE_BAND_ID`),
  KEY `FKh2i7xep5l3txpi65xpb3vxxdh` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FKh2i7xep5l3txpi65xpb3vxxdh` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_opt_banded_prc` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_fulfillment_weight_band`
--

DROP TABLE IF EXISTS `blc_fulfillment_weight_band`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_fulfillment_weight_band` (
  `FULFILLMENT_WEIGHT_BAND_ID` bigint(20) NOT NULL,
  `RESULT_AMOUNT` decimal(19,5) NOT NULL,
  `RESULT_AMOUNT_TYPE` varchar(255) NOT NULL,
  `MINIMUM_WEIGHT` decimal(19,5) DEFAULT NULL,
  `WEIGHT_UNIT_OF_MEASURE` varchar(255) DEFAULT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FULFILLMENT_WEIGHT_BAND_ID`),
  KEY `FKoij3p9iwe1856w6fd5283bpyl` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FKoij3p9iwe1856w6fd5283bpyl` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_opt_banded_wgt` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_gift_card_payment`
--

DROP TABLE IF EXISTS `blc_gift_card_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_gift_card_payment` (
  `PAYMENT_ID` bigint(20) NOT NULL,
  `PAN` varchar(255) NOT NULL,
  `PIN` varchar(255) DEFAULT NULL,
  `REFERENCE_NUMBER` varchar(255) NOT NULL,
  PRIMARY KEY (`PAYMENT_ID`),
  KEY `GIFTCARD_INDEX` (`REFERENCE_NUMBER`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_giftwrap_order_item`
--

DROP TABLE IF EXISTS `blc_giftwrap_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_giftwrap_order_item` (
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`),
  CONSTRAINT `FKtq6vr571td9a8ihss8os1wtr8` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_discrete_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_id_generation`
--

DROP TABLE IF EXISTS `blc_id_generation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_id_generation` (
  `ID_TYPE` varchar(255) NOT NULL,
  `BATCH_SIZE` bigint(20) NOT NULL,
  `BATCH_START` bigint(20) NOT NULL,
  `ID_MIN` bigint(20) DEFAULT NULL,
  `ID_MAX` bigint(20) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`ID_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_img_static_asset`
--

DROP TABLE IF EXISTS `blc_img_static_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_img_static_asset` (
  `HEIGHT` int(11) DEFAULT NULL,
  `WIDTH` int(11) DEFAULT NULL,
  `STATIC_ASSET_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`STATIC_ASSET_ID`),
  CONSTRAINT `FK6pugoo2mcm6irchv42bui3tm6` FOREIGN KEY (`STATIC_ASSET_ID`) REFERENCES `blc_static_asset` (`STATIC_ASSET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_index_field`
--

DROP TABLE IF EXISTS `blc_index_field`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_index_field` (
  `INDEX_FIELD_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `SEARCHABLE` bit(1) DEFAULT NULL,
  `FIELD_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`INDEX_FIELD_ID`),
  KEY `INDEX_FIELD_SEARCHABLE_INDEX` (`SEARCHABLE`),
  KEY `FKc1x5lu6romu8tsjrlpjmsqqxy` (`FIELD_ID`),
  CONSTRAINT `FKc1x5lu6romu8tsjrlpjmsqqxy` FOREIGN KEY (`FIELD_ID`) REFERENCES `blc_field` (`FIELD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_index_field_type`
--

DROP TABLE IF EXISTS `blc_index_field_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_index_field_type` (
  `INDEX_FIELD_TYPE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `FIELD_TYPE` varchar(255) DEFAULT NULL,
  `INDEX_FIELD_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`INDEX_FIELD_TYPE_ID`),
  KEY `FKmv0l0yt2099ffo2pjdrdbbj9h` (`INDEX_FIELD_ID`),
  CONSTRAINT `FKmv0l0yt2099ffo2pjdrdbbj9h` FOREIGN KEY (`INDEX_FIELD_ID`) REFERENCES `blc_index_field` (`INDEX_FIELD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_iso_country`
--

DROP TABLE IF EXISTS `blc_iso_country`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_iso_country` (
  `ALPHA_2` varchar(255) NOT NULL,
  `ALPHA_3` varchar(255) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `NUMERIC_CODE` int(11) DEFAULT NULL,
  `STATUS` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ALPHA_2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_item_offer_qualifier`
--

DROP TABLE IF EXISTS `blc_item_offer_qualifier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_item_offer_qualifier` (
  `ITEM_OFFER_QUALIFIER_ID` bigint(20) NOT NULL,
  `QUANTITY` bigint(20) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ITEM_OFFER_QUALIFIER_ID`),
  KEY `FKo9i9n1thqcqt9nu0fv2nlg1ec` (`OFFER_ID`),
  KEY `FK9fl5rced4g4u8sxh1j4mrwkto` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK9fl5rced4g4u8sxh1j4mrwkto` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKo9i9n1thqcqt9nu0fv2nlg1ec` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_locale`
--

DROP TABLE IF EXISTS `blc_locale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_locale` (
  `LOCALE_CODE` varchar(255) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DEFAULT_FLAG` bit(1) DEFAULT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  `USE_IN_SEARCH_INDEX` bit(1) DEFAULT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`LOCALE_CODE`),
  KEY `FK6gs37rhrtyd5ei2oqspxxrc3x` (`CURRENCY_CODE`),
  CONSTRAINT `FK6gs37rhrtyd5ei2oqspxxrc3x` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_media`
--

DROP TABLE IF EXISTS `blc_media`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_media` (
  `MEDIA_ID` bigint(20) NOT NULL,
  `ALT_TEXT` varchar(255) DEFAULT NULL,
  `TAGS` varchar(255) DEFAULT NULL,
  `TITLE` varchar(255) DEFAULT NULL,
  `URL` varchar(255) NOT NULL,
  PRIMARY KEY (`MEDIA_ID`),
  KEY `MEDIA_TITLE_INDEX` (`TITLE`),
  KEY `MEDIA_URL_INDEX` (`URL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_module_configuration`
--

DROP TABLE IF EXISTS `blc_module_configuration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_module_configuration` (
  `MODULE_CONFIG_ID` bigint(20) NOT NULL,
  `ACTIVE_END_DATE` datetime DEFAULT NULL,
  `ACTIVE_START_DATE` datetime DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `CONFIG_TYPE` varchar(255) NOT NULL,
  `IS_DEFAULT` bit(1) NOT NULL,
  `MODULE_NAME` varchar(255) NOT NULL,
  `MODULE_PRIORITY` int(11) NOT NULL,
  PRIMARY KEY (`MODULE_CONFIG_ID`),
  KEY `BLC_MODULE_CONFIGURATION_CONFIG_TYPE` (`CONFIG_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer`
--

DROP TABLE IF EXISTS `blc_offer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer` (
  `OFFER_ID` bigint(20) NOT NULL,
  `APPLY_TO_CHILD_ITEMS` bit(1) DEFAULT NULL,
  `APPLY_TO_SALE_PRICE` bit(1) DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `AUTOMATICALLY_ADDED` bit(1) DEFAULT NULL,
  `COMBINABLE_WITH_OTHER_OFFERS` bit(1) DEFAULT NULL,
  `OFFER_DESCRIPTION` varchar(255) DEFAULT NULL,
  `OFFER_DISCOUNT_TYPE` varchar(255) DEFAULT NULL,
  `END_DATE` datetime DEFAULT NULL,
  `MARKETING_MESSASGE` varchar(255) DEFAULT NULL,
  `MAX_USES_PER_CUSTOMER` bigint(20) DEFAULT NULL,
  `MAX_USES` int(11) DEFAULT NULL,
  `OFFER_NAME` varchar(255) NOT NULL,
  `OFFER_ITEM_QUALIFIER_RULE` varchar(255) DEFAULT NULL,
  `OFFER_ITEM_TARGET_RULE` varchar(255) DEFAULT NULL,
  `ORDER_MIN_TOTAL` decimal(19,5) DEFAULT NULL,
  `OFFER_PRIORITY` int(11) DEFAULT NULL,
  `QUALIFYING_ITEM_MIN_TOTAL` decimal(19,5) DEFAULT NULL,
  `REQUIRES_RELATED_TAR_QUAL` bit(1) DEFAULT NULL,
  `START_DATE` datetime DEFAULT NULL,
  `TARGET_MIN_TOTAL` decimal(19,5) DEFAULT NULL,
  `TARGET_SYSTEM` varchar(255) DEFAULT NULL,
  `TOTALITARIAN_OFFER` bit(1) DEFAULT NULL,
  `OFFER_TYPE` varchar(255) NOT NULL,
  `OFFER_VALUE` decimal(19,5) NOT NULL,
  PRIMARY KEY (`OFFER_ID`),
  KEY `OFFER_DISCOUNT_INDEX` (`OFFER_DISCOUNT_TYPE`),
  KEY `OFFER_MARKETING_MESSAGE_INDEX` (`MARKETING_MESSASGE`),
  KEY `OFFER_NAME_INDEX` (`OFFER_NAME`),
  KEY `OFFER_TYPE_INDEX` (`OFFER_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_audit`
--

DROP TABLE IF EXISTS `blc_offer_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_audit` (
  `OFFER_AUDIT_ID` bigint(20) NOT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  `OFFER_CODE_ID` bigint(20) DEFAULT NULL,
  `OFFER_ID` bigint(20) DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  `REDEEMED_DATE` datetime DEFAULT NULL,
  PRIMARY KEY (`OFFER_AUDIT_ID`),
  KEY `OFFERAUDIT_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `OFFERAUDIT_OFFER_CODE_INDEX` (`OFFER_CODE_ID`),
  KEY `OFFERAUDIT_OFFER_INDEX` (`OFFER_ID`),
  KEY `OFFERAUDIT_ORDER_INDEX` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_code`
--

DROP TABLE IF EXISTS `blc_offer_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_code` (
  `OFFER_CODE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `MAX_USES` int(11) DEFAULT NULL,
  `OFFER_CODE` varchar(255) NOT NULL,
  `END_DATE` datetime DEFAULT NULL,
  `START_DATE` datetime DEFAULT NULL,
  `USES` int(11) DEFAULT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`OFFER_CODE_ID`),
  KEY `OFFER_CODE_EMAIL_INDEX` (`EMAIL_ADDRESS`),
  KEY `OFFERCODE_OFFER_INDEX` (`OFFER_ID`),
  KEY `OFFERCODE_CODE_INDEX` (`OFFER_CODE`),
  CONSTRAINT `FK4rcfx31u6n9esw1ob98u8o87o` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_info`
--

DROP TABLE IF EXISTS `blc_offer_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_info` (
  `OFFER_INFO_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`OFFER_INFO_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_info_fields`
--

DROP TABLE IF EXISTS `blc_offer_info_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_info_fields` (
  `OFFER_INFO_FIELDS_ID` bigint(20) NOT NULL,
  `FIELD_VALUE` varchar(255) DEFAULT NULL,
  `FIELD_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`OFFER_INFO_FIELDS_ID`,`FIELD_NAME`),
  CONSTRAINT `FKohr0h2751uuxgawkbkakbptqn` FOREIGN KEY (`OFFER_INFO_FIELDS_ID`) REFERENCES `blc_offer_info` (`OFFER_INFO_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_item_criteria`
--

DROP TABLE IF EXISTS `blc_offer_item_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_item_criteria` (
  `OFFER_ITEM_CRITERIA_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_MATCH_RULE` longtext,
  `QUANTITY` int(11) NOT NULL,
  PRIMARY KEY (`OFFER_ITEM_CRITERIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_rule`
--

DROP TABLE IF EXISTS `blc_offer_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_rule` (
  `OFFER_RULE_ID` bigint(20) NOT NULL,
  `MATCH_RULE` longtext,
  PRIMARY KEY (`OFFER_RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_offer_rule_map`
--

DROP TABLE IF EXISTS `blc_offer_rule_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_offer_rule_map` (
  `OFFER_OFFER_RULE_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  `BLC_OFFER_OFFER_ID` bigint(20) NOT NULL,
  `OFFER_RULE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`OFFER_OFFER_RULE_ID`),
  KEY `FKmwr0lw44aa4hulm8c9qg39x9x` (`BLC_OFFER_OFFER_ID`),
  KEY `FK8ndq3dtgs1cr4ds9eil3sxcti` (`OFFER_RULE_ID`),
  CONSTRAINT `FK8ndq3dtgs1cr4ds9eil3sxcti` FOREIGN KEY (`OFFER_RULE_ID`) REFERENCES `blc_offer_rule` (`OFFER_RULE_ID`),
  CONSTRAINT `FKmwr0lw44aa4hulm8c9qg39x9x` FOREIGN KEY (`BLC_OFFER_OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order`
--

DROP TABLE IF EXISTS `blc_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order` (
  `ORDER_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `EMAIL_ADDRESS` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `ORDER_NUMBER` varchar(255) DEFAULT NULL,
  `IS_PREVIEW` bit(1) DEFAULT NULL,
  `ORDER_STATUS` varchar(255) DEFAULT NULL,
  `ORDER_SUBTOTAL` decimal(19,5) DEFAULT NULL,
  `SUBMIT_DATE` datetime DEFAULT NULL,
  `TAX_OVERRIDE` bit(1) DEFAULT NULL,
  `ORDER_TOTAL` decimal(19,5) DEFAULT NULL,
  `TOTAL_SHIPPING` decimal(19,5) DEFAULT NULL,
  `TOTAL_TAX` decimal(19,5) DEFAULT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `LOCALE_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ID`),
  KEY `ORDER_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `ORDER_EMAIL_INDEX` (`EMAIL_ADDRESS`),
  KEY `ORDER_NAME_INDEX` (`NAME`),
  KEY `ORDER_NUMBER_INDEX` (`ORDER_NUMBER`),
  KEY `ORDER_STATUS_INDEX` (`ORDER_STATUS`),
  KEY `FKl58agohje8ndhoow8s8hlday1` (`CURRENCY_CODE`),
  KEY `FKc90jmu6i66weyh7o0u5q94jtv` (`LOCALE_CODE`),
  CONSTRAINT `FKc90jmu6i66weyh7o0u5q94jtv` FOREIGN KEY (`LOCALE_CODE`) REFERENCES `blc_locale` (`LOCALE_CODE`),
  CONSTRAINT `FKh0ouiaamkm2k7qfgc6cjacukg` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKl58agohje8ndhoow8s8hlday1` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_adjustment`
--

DROP TABLE IF EXISTS `blc_order_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_adjustment` (
  `ORDER_ADJUSTMENT_ID` bigint(20) NOT NULL,
  `ADJUSTMENT_REASON` varchar(255) NOT NULL,
  `ADJUSTMENT_VALUE` decimal(19,5) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ADJUSTMENT_ID`),
  KEY `ORDERADJUST_OFFER_INDEX` (`OFFER_ID`),
  KEY `ORDERADJUST_ORDER_INDEX` (`ORDER_ID`),
  CONSTRAINT `FKh9agwlogxxgfxbxe7rcgrwv4u` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`),
  CONSTRAINT `FKmlymutb81ohtx11e2u64tjqyu` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_attribute`
--

DROP TABLE IF EXISTS `blc_order_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_attribute` (
  `ORDER_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ATTRIBUTE_ID`),
  UNIQUE KEY `ATTR_NAME_ORDER_ID` (`NAME`,`ORDER_ID`),
  KEY `FKa5k0dl8lmasauj4cmems5e16s` (`ORDER_ID`),
  CONSTRAINT `FKa5k0dl8lmasauj4cmems5e16s` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item`
--

DROP TABLE IF EXISTS `blc_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item` (
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DISCOUNTS_ALLOWED` bit(1) DEFAULT NULL,
  `HAS_VALIDATION_ERRORS` bit(1) DEFAULT NULL,
  `ITEM_TAXABLE_FLAG` bit(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `ORDER_ITEM_TYPE` varchar(255) DEFAULT NULL,
  `PRICE` decimal(19,5) DEFAULT NULL,
  `QUANTITY` int(11) NOT NULL,
  `RETAIL_PRICE` decimal(19,5) DEFAULT NULL,
  `RETAIL_PRICE_OVERRIDE` bit(1) DEFAULT NULL,
  `SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `SALE_PRICE_OVERRIDE` bit(1) DEFAULT NULL,
  `TOTAL_TAX` decimal(19,2) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `GIFT_WRAP_ITEM_ID` bigint(20) DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  `PARENT_ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  `PERSONAL_MESSAGE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`),
  KEY `ORDERITEM_CATEGORY_INDEX` (`CATEGORY_ID`),
  KEY `ORDERITEM_GIFT_INDEX` (`GIFT_WRAP_ITEM_ID`),
  KEY `ORDERITEM_ORDER_INDEX` (`ORDER_ID`),
  KEY `ORDERITEM_TYPE_INDEX` (`ORDER_ITEM_TYPE`),
  KEY `ORDERITEM_PARENT_INDEX` (`PARENT_ORDER_ITEM_ID`),
  KEY `ORDERITEM_MESSAGE_INDEX` (`PERSONAL_MESSAGE_ID`),
  CONSTRAINT `FK3553qqcmvw5i3durebksttod3` FOREIGN KEY (`GIFT_WRAP_ITEM_ID`) REFERENCES `blc_giftwrap_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK4vocoseu9tnln1vq4r2gygh3n` FOREIGN KEY (`PARENT_ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK737vx8aceqsa8wyb6hjt44x58` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FK8b71a8di9bu8jrssp98u8ka0s` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`),
  CONSTRAINT `FKccrkxx60l5x2q24dl97x9iu0a` FOREIGN KEY (`PERSONAL_MESSAGE_ID`) REFERENCES `blc_personal_message` (`PERSONAL_MESSAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_add_attr`
--

DROP TABLE IF EXISTS `blc_order_item_add_attr`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_add_attr` (
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ORDER_ITEM_ID`,`NAME`),
  CONSTRAINT `FK2xfsv1rmg5hy926njxrgrkxja` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_discrete_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_adjustment`
--

DROP TABLE IF EXISTS `blc_order_item_adjustment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_adjustment` (
  `ORDER_ITEM_ADJUSTMENT_ID` bigint(20) NOT NULL,
  `APPLIED_TO_SALE_PRICE` bit(1) DEFAULT NULL,
  `ADJUSTMENT_REASON` varchar(255) NOT NULL,
  `ADJUSTMENT_VALUE` decimal(19,5) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_ADJUSTMENT_ID`),
  KEY `OIADJUST_ITEM_INDEX` (`ORDER_ITEM_ID`),
  KEY `FKkw991n1so1bd0nvmtgky3d4rm` (`OFFER_ID`),
  CONSTRAINT `FKkw991n1so1bd0nvmtgky3d4rm` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`),
  CONSTRAINT `FKniw5eryl2ea895x5p3wh4sd0u` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_attribute`
--

DROP TABLE IF EXISTS `blc_order_item_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_attribute` (
  `ORDER_ITEM_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ORDER_ITEM_ATTRIBUTE_ID`),
  UNIQUE KEY `ATTR_NAME_ORDER_ITEM_ID` (`NAME`,`ORDER_ITEM_ID`),
  KEY `FK5f2l8atn9sh06yhbjx72i8tl1` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK5f2l8atn9sh06yhbjx72i8tl1` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_cart_message`
--

DROP TABLE IF EXISTS `blc_order_item_cart_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_cart_message` (
  `ORDER_ITEM_ID` bigint(20) NOT NULL,
  `CART_MESSAGE` varchar(255) DEFAULT NULL,
  KEY `FKpm9ip11x3m6rnkla1khhgqmfn` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKpm9ip11x3m6rnkla1khhgqmfn` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_dtl_adj`
--

DROP TABLE IF EXISTS `blc_order_item_dtl_adj`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_dtl_adj` (
  `ORDER_ITEM_DTL_ADJ_ID` bigint(20) NOT NULL,
  `APPLIED_TO_SALE_PRICE` bit(1) DEFAULT NULL,
  `OFFER_NAME` varchar(255) DEFAULT NULL,
  `ADJUSTMENT_REASON` varchar(255) NOT NULL,
  `ADJUSTMENT_VALUE` decimal(19,5) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_PRICE_DTL_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_DTL_ADJ_ID`),
  KEY `FKtaukfbw7rtre7kmvl6fla49t6` (`OFFER_ID`),
  KEY `FKtecvjagoubp6v2337mqm5gnmg` (`ORDER_ITEM_PRICE_DTL_ID`),
  CONSTRAINT `FKtaukfbw7rtre7kmvl6fla49t6` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`),
  CONSTRAINT `FKtecvjagoubp6v2337mqm5gnmg` FOREIGN KEY (`ORDER_ITEM_PRICE_DTL_ID`) REFERENCES `blc_order_item_price_dtl` (`ORDER_ITEM_PRICE_DTL_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_item_price_dtl`
--

DROP TABLE IF EXISTS `blc_order_item_price_dtl`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_item_price_dtl` (
  `ORDER_ITEM_PRICE_DTL_ID` bigint(20) NOT NULL,
  `QUANTITY` int(11) NOT NULL,
  `USE_SALE_PRICE` bit(1) DEFAULT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_ITEM_PRICE_DTL_ID`),
  KEY `FKerm8r2c1fj0vvd2rj0uxmavlj` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKerm8r2c1fj0vvd2rj0uxmavlj` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_lock`
--

DROP TABLE IF EXISTS `blc_order_lock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_lock` (
  `LOCK_KEY` varchar(255) NOT NULL,
  `ORDER_ID` bigint(20) NOT NULL,
  `LAST_UPDATED` bigint(20) DEFAULT NULL,
  `LOCKED` char(1) DEFAULT NULL,
  PRIMARY KEY (`LOCK_KEY`,`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_multiship_option`
--

DROP TABLE IF EXISTS `blc_order_multiship_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_multiship_option` (
  `ORDER_MULTISHIP_OPTION_ID` bigint(20) NOT NULL,
  `ADDRESS_ID` bigint(20) DEFAULT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_MULTISHIP_OPTION_ID`),
  KEY `MULTISHIP_OPTION_ORDER_INDEX` (`ORDER_ID`),
  KEY `FKt77nf9y3nokcclqibjhihjily` (`ADDRESS_ID`),
  KEY `FK1d1sd1fr2cdv0kvf2s5yclo55` (`FULFILLMENT_OPTION_ID`),
  KEY `FK8poefpppeoej296cr2g1otfki` (`ORDER_ITEM_ID`),
  CONSTRAINT `FK1d1sd1fr2cdv0kvf2s5yclo55` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FK8poefpppeoej296cr2g1otfki` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKrpsf6ltf21ohrgimnktmq5dn3` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`),
  CONSTRAINT `FKt77nf9y3nokcclqibjhihjily` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_offer_code_xref`
--

DROP TABLE IF EXISTS `blc_order_offer_code_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_offer_code_xref` (
  `ORDER_ID` bigint(20) NOT NULL,
  `OFFER_CODE_ID` bigint(20) NOT NULL,
  KEY `FKdtidg8l9a5wpcuwpnqbwwhuve` (`OFFER_CODE_ID`),
  KEY `FKljh9nh9lgxkgnebscn8u8sbgf` (`ORDER_ID`),
  CONSTRAINT `FKdtidg8l9a5wpcuwpnqbwwhuve` FOREIGN KEY (`OFFER_CODE_ID`) REFERENCES `blc_offer_code` (`OFFER_CODE_ID`),
  CONSTRAINT `FKljh9nh9lgxkgnebscn8u8sbgf` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_payment`
--

DROP TABLE IF EXISTS `blc_order_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_payment` (
  `ORDER_PAYMENT_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `AMOUNT` decimal(19,5) DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `GATEWAY_TYPE` varchar(255) DEFAULT NULL,
  `REFERENCE_NUMBER` varchar(255) DEFAULT NULL,
  `PAYMENT_TYPE` varchar(255) NOT NULL,
  `ADDRESS_ID` bigint(20) DEFAULT NULL,
  `ORDER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ORDER_PAYMENT_ID`),
  KEY `ORDERPAYMENT_ADDRESS_INDEX` (`ADDRESS_ID`),
  KEY `ORDERPAYMENT_ORDER_INDEX` (`ORDER_ID`),
  KEY `ORDERPAYMENT_REFERENCE_INDEX` (`REFERENCE_NUMBER`),
  KEY `ORDERPAYMENT_TYPE_INDEX` (`PAYMENT_TYPE`),
  CONSTRAINT `FK7k9dsqtdku90rongw4f2xsgg5` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`),
  CONSTRAINT `FKh0n8n75hk2l646hsxyyqrwgpx` FOREIGN KEY (`ORDER_ID`) REFERENCES `blc_order` (`ORDER_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_order_payment_transaction`
--

DROP TABLE IF EXISTS `blc_order_payment_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_order_payment_transaction` (
  `PAYMENT_TRANSACTION_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `TRANSACTION_AMOUNT` decimal(19,2) DEFAULT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CUSTOMER_IP_ADDRESS` varchar(255) DEFAULT NULL,
  `DATE_RECORDED` datetime DEFAULT NULL,
  `RAW_RESPONSE` longtext,
  `SAVE_TOKEN` bit(1) DEFAULT NULL,
  `SUCCESS` bit(1) DEFAULT NULL,
  `TRANSACTION_TYPE` varchar(255) DEFAULT NULL,
  `ORDER_PAYMENT` bigint(20) NOT NULL,
  `PARENT_TRANSACTION` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PAYMENT_TRANSACTION_ID`),
  KEY `FKq3hymseoakriel7rw57g3vh5n` (`ORDER_PAYMENT`),
  KEY `FKjg77vkh5u48omyy8uhagkswxs` (`PARENT_TRANSACTION`),
  CONSTRAINT `FKjg77vkh5u48omyy8uhagkswxs` FOREIGN KEY (`PARENT_TRANSACTION`) REFERENCES `blc_order_payment_transaction` (`PAYMENT_TRANSACTION_ID`),
  CONSTRAINT `FKq3hymseoakriel7rw57g3vh5n` FOREIGN KEY (`ORDER_PAYMENT`) REFERENCES `blc_order_payment` (`ORDER_PAYMENT_ID`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page`
--

DROP TABLE IF EXISTS `blc_page`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page` (
  `PAGE_ID` bigint(20) NOT NULL,
  `ACTIVE_END_DATE` datetime DEFAULT NULL,
  `ACTIVE_START_DATE` datetime DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `EXCLUDE_FROM_SITE_MAP` bit(1) DEFAULT NULL,
  `FULL_URL` varchar(255) DEFAULT NULL,
  `META_DESCRIPTION` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(255) DEFAULT NULL,
  `OFFLINE_FLAG` bit(1) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `PAGE_TMPLT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PAGE_ID`),
  KEY `PAGE_FULL_URL_INDEX` (`FULL_URL`),
  KEY `FKo95c7m41ycmhf4dwpebvemasl` (`PAGE_TMPLT_ID`),
  CONSTRAINT `FKo95c7m41ycmhf4dwpebvemasl` FOREIGN KEY (`PAGE_TMPLT_ID`) REFERENCES `blc_page_tmplt` (`PAGE_TMPLT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_attributes`
--

DROP TABLE IF EXISTS `blc_page_attributes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_attributes` (
  `ATTRIBUTE_ID` bigint(20) NOT NULL,
  `FIELD_NAME` varchar(255) NOT NULL,
  `FIELD_VALUE` varchar(255) DEFAULT NULL,
  `PAGE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ATTRIBUTE_ID`),
  KEY `PAGEATTRIBUTE_NAME_INDEX` (`FIELD_NAME`),
  KEY `PAGEATTRIBUTE_INDEX` (`PAGE_ID`),
  CONSTRAINT `FK94785hg4iuw1k22qh6b8hysxe` FOREIGN KEY (`PAGE_ID`) REFERENCES `blc_page` (`PAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_fld`
--

DROP TABLE IF EXISTS `blc_page_fld`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_fld` (
  `PAGE_FLD_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `FLD_KEY` varchar(255) DEFAULT NULL,
  `LOB_VALUE` longtext,
  `VALUE` varchar(255) DEFAULT NULL,
  `PAGE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PAGE_FLD_ID`),
  KEY `FK8t4im2p53x0mfufl92k87tsnx` (`PAGE_ID`),
  CONSTRAINT `FK8t4im2p53x0mfufl92k87tsnx` FOREIGN KEY (`PAGE_ID`) REFERENCES `blc_page` (`PAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_item_criteria`
--

DROP TABLE IF EXISTS `blc_page_item_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_item_criteria` (
  `PAGE_ITEM_CRITERIA_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_MATCH_RULE` longtext,
  `QUANTITY` int(11) NOT NULL,
  PRIMARY KEY (`PAGE_ITEM_CRITERIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_rule`
--

DROP TABLE IF EXISTS `blc_page_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_rule` (
  `PAGE_RULE_ID` bigint(20) NOT NULL,
  `MATCH_RULE` longtext,
  PRIMARY KEY (`PAGE_RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_rule_map`
--

DROP TABLE IF EXISTS `blc_page_rule_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_rule_map` (
  `BLC_PAGE_PAGE_ID` bigint(20) NOT NULL,
  `PAGE_RULE_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  PRIMARY KEY (`BLC_PAGE_PAGE_ID`,`MAP_KEY`),
  KEY `FKhj9uu6o7fb0n81g5yvk48skem` (`PAGE_RULE_ID`),
  CONSTRAINT `FKhj9uu6o7fb0n81g5yvk48skem` FOREIGN KEY (`PAGE_RULE_ID`) REFERENCES `blc_page_rule` (`PAGE_RULE_ID`),
  CONSTRAINT `FKtqx8xsmgx0hkrvery3ipqwwi0` FOREIGN KEY (`BLC_PAGE_PAGE_ID`) REFERENCES `blc_page` (`PAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_page_tmplt`
--

DROP TABLE IF EXISTS `blc_page_tmplt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_page_tmplt` (
  `PAGE_TMPLT_ID` bigint(20) NOT NULL,
  `TMPLT_DESCR` varchar(255) DEFAULT NULL,
  `TMPLT_NAME` varchar(255) DEFAULT NULL,
  `TMPLT_PATH` varchar(255) DEFAULT NULL,
  `LOCALE_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PAGE_TMPLT_ID`),
  KEY `FK19poavwqssando5ll1qid9kmf` (`LOCALE_CODE`),
  CONSTRAINT `FK19poavwqssando5ll1qid9kmf` FOREIGN KEY (`LOCALE_CODE`) REFERENCES `blc_locale` (`LOCALE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_payment_log`
--

DROP TABLE IF EXISTS `blc_payment_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_payment_log` (
  `PAYMENT_LOG_ID` bigint(20) NOT NULL,
  `AMOUNT_PAID` decimal(19,5) DEFAULT NULL,
  `EXCEPTION_MESSAGE` varchar(255) DEFAULT NULL,
  `LOG_TYPE` varchar(255) NOT NULL,
  `ORDER_PAYMENT_ID` bigint(20) DEFAULT NULL,
  `ORDER_PAYMENT_REF_NUM` varchar(255) DEFAULT NULL,
  `TRANSACTION_SUCCESS` bit(1) DEFAULT NULL,
  `TRANSACTION_TIMESTAMP` datetime NOT NULL,
  `TRANSACTION_TYPE` varchar(255) NOT NULL,
  `USER_NAME` varchar(255) NOT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  `CUSTOMER_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PAYMENT_LOG_ID`),
  KEY `PAYMENTLOG_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `PAYMENTLOG_LOGTYPE_INDEX` (`LOG_TYPE`),
  KEY `PAYMENTLOG_ORDERPAYMENT_INDEX` (`ORDER_PAYMENT_ID`),
  KEY `PAYMENTLOG_REFERENCE_INDEX` (`ORDER_PAYMENT_REF_NUM`),
  KEY `PAYMENTLOG_TRANTYPE_INDEX` (`TRANSACTION_TYPE`),
  KEY `PAYMENTLOG_USER_INDEX` (`USER_NAME`),
  KEY `FK1tulv4shboj3o32mjrbk2jh93` (`CURRENCY_CODE`),
  CONSTRAINT `FK1tulv4shboj3o32mjrbk2jh93` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`),
  CONSTRAINT `FK39ybfiu04bab30cqo45cap3ew` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_personal_message`
--

DROP TABLE IF EXISTS `blc_personal_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_personal_message` (
  `PERSONAL_MESSAGE_ID` bigint(20) NOT NULL,
  `MESSAGE` varchar(255) DEFAULT NULL,
  `MESSAGE_FROM` varchar(255) DEFAULT NULL,
  `MESSAGE_TO` varchar(255) DEFAULT NULL,
  `OCCASION` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PERSONAL_MESSAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_pgtmplt_fldgrp_xref`
--

DROP TABLE IF EXISTS `blc_pgtmplt_fldgrp_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_pgtmplt_fldgrp_xref` (
  `PG_TMPLT_FLD_GRP_ID` bigint(20) NOT NULL,
  `GROUP_ORDER` decimal(10,6) DEFAULT NULL,
  `FLD_GROUP_ID` bigint(20) DEFAULT NULL,
  `PAGE_TMPLT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PG_TMPLT_FLD_GRP_ID`),
  KEY `FKoy5hlxlq3pii0gj8yalskxv88` (`FLD_GROUP_ID`),
  KEY `FKr3xcn67le94r6oxnaebm5ebnk` (`PAGE_TMPLT_ID`),
  CONSTRAINT `FKoy5hlxlq3pii0gj8yalskxv88` FOREIGN KEY (`FLD_GROUP_ID`) REFERENCES `blc_fld_group` (`FLD_GROUP_ID`),
  CONSTRAINT `FKr3xcn67le94r6oxnaebm5ebnk` FOREIGN KEY (`PAGE_TMPLT_ID`) REFERENCES `blc_page_tmplt` (`PAGE_TMPLT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_phone`
--

DROP TABLE IF EXISTS `blc_phone`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_phone` (
  `PHONE_ID` bigint(20) NOT NULL,
  `COUNTRY_CODE` varchar(255) DEFAULT NULL,
  `EXTENSION` varchar(255) DEFAULT NULL,
  `IS_ACTIVE` bit(1) DEFAULT NULL,
  `IS_DEFAULT` bit(1) DEFAULT NULL,
  `PHONE_NUMBER` varchar(255) NOT NULL,
  PRIMARY KEY (`PHONE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product`
--

DROP TABLE IF EXISTS `blc_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product` (
  `PRODUCT_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CAN_SELL_WITHOUT_OPTIONS` bit(1) DEFAULT NULL,
  `CANONICAL_URL` varchar(255) DEFAULT NULL,
  `DISPLAY_TEMPLATE` varchar(255) DEFAULT NULL,
  `IS_FEATURED_PRODUCT` bit(1) NOT NULL,
  `MANUFACTURE` varchar(255) DEFAULT NULL,
  `META_DESC` varchar(255) DEFAULT NULL,
  `META_TITLE` varchar(255) DEFAULT NULL,
  `MODEL` varchar(255) DEFAULT NULL,
  `OVERRIDE_GENERATED_URL` bit(1) DEFAULT NULL,
  `URL` varchar(255) DEFAULT NULL,
  `URL_KEY` varchar(255) DEFAULT NULL,
  `DEFAULT_CATEGORY_ID` bigint(20) DEFAULT NULL,
  `DEFAULT_SKU_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_ID`),
  KEY `PRODUCT_CATEGORY_INDEX` (`DEFAULT_CATEGORY_ID`),
  KEY `PRODUCT_URL_INDEX` (`URL`,`URL_KEY`),
  KEY `FK9dljxk44kotarjr9ngebtxntx` (`DEFAULT_SKU_ID`),
  KEY `BLC_PRODUCT_url_key` (`URL_KEY`),
  CONSTRAINT `FK57aoxhpvwg389v7sx4m153mde` FOREIGN KEY (`DEFAULT_CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FK9dljxk44kotarjr9ngebtxntx` FOREIGN KEY (`DEFAULT_SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_attribute`
--

DROP TABLE IF EXISTS `blc_product_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_attribute` (
  `PRODUCT_ATTRIBUTE_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_ATTRIBUTE_ID`),
  KEY `PRODUCTATTRIBUTE_NAME_INDEX` (`NAME`),
  KEY `PRODUCTATTRIBUTE_INDEX` (`PRODUCT_ID`),
  CONSTRAINT `FK5rahmy0l6hsgbvgb37ojlkx09` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_bundle`
--

DROP TABLE IF EXISTS `blc_product_bundle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_bundle` (
  `AUTO_BUNDLE` bit(1) DEFAULT NULL,
  `BUNDLE_PROMOTABLE` bit(1) DEFAULT NULL,
  `ITEMS_PROMOTABLE` bit(1) DEFAULT NULL,
  `PRICING_MODEL` varchar(255) DEFAULT NULL,
  `BUNDLE_PRIORITY` int(11) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_ID`),
  CONSTRAINT `FK2hern8ie7vx4k6cawbryglb9g` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_cross_sale`
--

DROP TABLE IF EXISTS `blc_product_cross_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_cross_sale` (
  `CROSS_SALE_PRODUCT_ID` bigint(20) NOT NULL,
  `PROMOTION_MESSAGE` varchar(255) DEFAULT NULL,
  `SEQUENCE` decimal(10,6) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  `RELATED_SALE_PRODUCT_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`CROSS_SALE_PRODUCT_ID`),
  KEY `CROSSSALE_CATEGORY_INDEX` (`CATEGORY_ID`),
  KEY `CROSSSALE_INDEX` (`PRODUCT_ID`),
  KEY `CROSSSALE_RELATED_INDEX` (`RELATED_SALE_PRODUCT_ID`),
  KEY `BLC_PRODUCT_CROSS_SALE_product_id` (`PRODUCT_ID`),
  CONSTRAINT `FKak6hk19vp8ioy27lrt7x9be7w` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKeq0i4yj6td2qxh0tnekeomrxk` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`),
  CONSTRAINT `FKovg4s9i9ejesgcygfpyjq7eqa` FOREIGN KEY (`RELATED_SALE_PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_featured`
--

DROP TABLE IF EXISTS `blc_product_featured`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_featured` (
  `FEATURED_PRODUCT_ID` bigint(20) NOT NULL,
  `PROMOTION_MESSAGE` varchar(255) DEFAULT NULL,
  `SEQUENCE` decimal(10,6) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`FEATURED_PRODUCT_ID`),
  KEY `PRODFEATURED_CATEGORY_INDEX` (`CATEGORY_ID`),
  KEY `PRODFEATURED_PRODUCT_INDEX` (`PRODUCT_ID`),
  CONSTRAINT `FK83xhh0734p8wo8w64di8qwd9o` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKr4v6adrqqmd1qe09i6mb8fj8p` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_option`
--

DROP TABLE IF EXISTS `blc_product_option`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_option` (
  `PRODUCT_OPTION_ID` bigint(20) NOT NULL,
  `ATTRIBUTE_NAME` varchar(255) DEFAULT NULL,
  `DISPLAY_ORDER` int(11) DEFAULT NULL,
  `ERROR_CODE` varchar(255) DEFAULT NULL,
  `ERROR_MESSAGE` varchar(255) DEFAULT NULL,
  `LABEL` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `VALIDATION_STRATEGY_TYPE` varchar(255) DEFAULT NULL,
  `VALIDATION_TYPE` varchar(255) DEFAULT NULL,
  `REQUIRED` bit(1) DEFAULT NULL,
  `OPTION_TYPE` varchar(255) DEFAULT NULL,
  `USE_IN_SKU_GENERATION` bit(1) DEFAULT NULL,
  `VALIDATION_STRING` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_OPTION_ID`),
  KEY `PRODUCT_OPTION_NAME_INDEX` (`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_option_value`
--

DROP TABLE IF EXISTS `blc_product_option_value`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_option_value` (
  `PRODUCT_OPTION_VALUE_ID` bigint(20) NOT NULL,
  `ATTRIBUTE_VALUE` varchar(255) DEFAULT NULL,
  `DISPLAY_ORDER` bigint(20) DEFAULT NULL,
  `PRICE_ADJUSTMENT` decimal(19,5) DEFAULT NULL,
  `PRODUCT_OPTION_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PRODUCT_OPTION_VALUE_ID`),
  KEY `FK9ixc1gbymkin77d6krmc3mub7` (`PRODUCT_OPTION_ID`),
  CONSTRAINT `FK9ixc1gbymkin77d6krmc3mub7` FOREIGN KEY (`PRODUCT_OPTION_ID`) REFERENCES `blc_product_option` (`PRODUCT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_option_xref`
--

DROP TABLE IF EXISTS `blc_product_option_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_option_xref` (
  `PRODUCT_OPTION_XREF_ID` bigint(20) NOT NULL,
  `PRODUCT_ID` bigint(20) NOT NULL,
  `PRODUCT_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PRODUCT_OPTION_XREF_ID`),
  KEY `FKhqikdn2uw75plhcwn4cmjtt4m` (`PRODUCT_ID`),
  KEY `FKswm8iotfkm6a9iyj6ru3rmpv8` (`PRODUCT_OPTION_ID`),
  CONSTRAINT `FKhqikdn2uw75plhcwn4cmjtt4m` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`),
  CONSTRAINT `FKswm8iotfkm6a9iyj6ru3rmpv8` FOREIGN KEY (`PRODUCT_OPTION_ID`) REFERENCES `blc_product_option` (`PRODUCT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_product_up_sale`
--

DROP TABLE IF EXISTS `blc_product_up_sale`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_product_up_sale` (
  `UP_SALE_PRODUCT_ID` bigint(20) NOT NULL,
  `PROMOTION_MESSAGE` varchar(255) DEFAULT NULL,
  `SEQUENCE` decimal(10,6) DEFAULT NULL,
  `CATEGORY_ID` bigint(20) DEFAULT NULL,
  `PRODUCT_ID` bigint(20) DEFAULT NULL,
  `RELATED_SALE_PRODUCT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`UP_SALE_PRODUCT_ID`),
  KEY `UPSALE_CATEGORY_INDEX` (`CATEGORY_ID`),
  KEY `UPSALE_PRODUCT_INDEX` (`PRODUCT_ID`),
  KEY `UPSALE_RELATED_INDEX` (`RELATED_SALE_PRODUCT_ID`),
  CONSTRAINT `FKgefhcqet8553xhh9bdjb1jbjo` FOREIGN KEY (`PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`),
  CONSTRAINT `FKkcx4cl73kctdinewei1fk2vvl` FOREIGN KEY (`CATEGORY_ID`) REFERENCES `blc_category` (`CATEGORY_ID`),
  CONSTRAINT `FKm1r8s9j593gpcgluy5uyfa862` FOREIGN KEY (`RELATED_SALE_PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_promotion_message`
--

DROP TABLE IF EXISTS `blc_promotion_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_promotion_message` (
  `PROMOTION_MESSAGE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `END_DATE` datetime DEFAULT NULL,
  `PROMOTION_MESSASGE` varchar(255) DEFAULT NULL,
  `MESSAGE_PLACEMENT` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `PROMOTION_MESSAGE_PRIORITY` int(11) DEFAULT NULL,
  `START_DATE` datetime DEFAULT NULL,
  `LOCALE_CODE` varchar(255) DEFAULT NULL,
  `MEDIA_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PROMOTION_MESSAGE_ID`),
  KEY `PROMOTION_MESSAGE_NAME_INDEX` (`NAME`),
  KEY `FK3dgs3j2b8mshpafd25qvtftgv` (`LOCALE_CODE`),
  KEY `FK59dkr5skbs8ve2v27truld8kc` (`MEDIA_ID`),
  CONSTRAINT `FK3dgs3j2b8mshpafd25qvtftgv` FOREIGN KEY (`LOCALE_CODE`) REFERENCES `blc_locale` (`LOCALE_CODE`),
  CONSTRAINT `FK59dkr5skbs8ve2v27truld8kc` FOREIGN KEY (`MEDIA_ID`) REFERENCES `blc_media` (`MEDIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_prorated_order_item_adjust`
--

DROP TABLE IF EXISTS `blc_prorated_order_item_adjust`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_prorated_order_item_adjust` (
  `PRORATED_ORDER_ITEM_ADJUST_ID` bigint(20) NOT NULL,
  `PRORATED_QUANTITY` int(11) NOT NULL,
  `ADJUSTMENT_REASON` varchar(255) NOT NULL,
  `PRORATED_ADJUSTMENT_VALUE` decimal(19,5) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`PRORATED_ORDER_ITEM_ADJUST_ID`),
  KEY `POIADJUST_ITEM_INDEX` (`ORDER_ITEM_ID`),
  KEY `FKkud7s4f923plknu6u4041v3oa` (`OFFER_ID`),
  CONSTRAINT `FKfmq7jd0v7r11g8hlvyuw50u6q` FOREIGN KEY (`ORDER_ITEM_ID`) REFERENCES `blc_order_item` (`ORDER_ITEM_ID`),
  CONSTRAINT `FKkud7s4f923plknu6u4041v3oa` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_qual_crit_offer_xref`
--

DROP TABLE IF EXISTS `blc_qual_crit_offer_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_qual_crit_offer_xref` (
  `OFFER_QUAL_CRIT_ID` bigint(20) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `OFFER_ITEM_CRITERIA_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`OFFER_QUAL_CRIT_ID`),
  KEY `FKmmxl8rjhiuu6hc7qhyy85pjov` (`OFFER_ID`),
  KEY `FK6e8y3yk68wvw90gtsesduqbrb` (`OFFER_ITEM_CRITERIA_ID`),
  CONSTRAINT `FK6e8y3yk68wvw90gtsesduqbrb` FOREIGN KEY (`OFFER_ITEM_CRITERIA_ID`) REFERENCES `blc_offer_item_criteria` (`OFFER_ITEM_CRITERIA_ID`),
  CONSTRAINT `FKmmxl8rjhiuu6hc7qhyy85pjov` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_qual_crit_page_xref`
--

DROP TABLE IF EXISTS `blc_qual_crit_page_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_qual_crit_page_xref` (
  `PAGE_ID` bigint(20) NOT NULL,
  `PAGE_ITEM_CRITERIA_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`PAGE_ID`,`PAGE_ITEM_CRITERIA_ID`),
  UNIQUE KEY `UK_dg6txhn3dw4k680q2sjyeumml` (`PAGE_ITEM_CRITERIA_ID`),
  CONSTRAINT `FKm0ov6kstmsab8gy93m53c05tg` FOREIGN KEY (`PAGE_ID`) REFERENCES `blc_page` (`PAGE_ID`),
  CONSTRAINT `FKpe7oenmm4t3g8ypvo5j2yjrd7` FOREIGN KEY (`PAGE_ITEM_CRITERIA_ID`) REFERENCES `blc_page_item_criteria` (`PAGE_ITEM_CRITERIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_qual_crit_sc_xref`
--

DROP TABLE IF EXISTS `blc_qual_crit_sc_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_qual_crit_sc_xref` (
  `SC_ID` bigint(20) NOT NULL,
  `SC_ITEM_CRITERIA_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SC_ID`,`SC_ITEM_CRITERIA_ID`),
  UNIQUE KEY `UK_afqd4tvahqdouwkfb55xjuycm` (`SC_ITEM_CRITERIA_ID`),
  CONSTRAINT `FK6v9jfn06vkk5kpio9jdu08t30` FOREIGN KEY (`SC_ITEM_CRITERIA_ID`) REFERENCES `blc_sc_item_criteria` (`SC_ITEM_CRITERIA_ID`),
  CONSTRAINT `FKq0wnd7j8o8ss4umkpdjn38ota` FOREIGN KEY (`SC_ID`) REFERENCES `blc_sc` (`SC_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_rating_detail`
--

DROP TABLE IF EXISTS `blc_rating_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_rating_detail` (
  `RATING_DETAIL_ID` bigint(20) NOT NULL,
  `RATING` double NOT NULL,
  `RATING_SUBMITTED_DATE` datetime NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `RATING_SUMMARY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`RATING_DETAIL_ID`),
  KEY `RATING_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `FKorvii76lm0rnac92c67q1oles` (`RATING_SUMMARY_ID`),
  CONSTRAINT `FKjjjou706ellmb65wmy7gpv07s` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKorvii76lm0rnac92c67q1oles` FOREIGN KEY (`RATING_SUMMARY_ID`) REFERENCES `blc_rating_summary` (`RATING_SUMMARY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_rating_summary`
--

DROP TABLE IF EXISTS `blc_rating_summary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_rating_summary` (
  `RATING_SUMMARY_ID` bigint(20) NOT NULL,
  `AVERAGE_RATING` double NOT NULL,
  `ITEM_ID` varchar(255) NOT NULL,
  `RATING_TYPE` varchar(255) NOT NULL,
  PRIMARY KEY (`RATING_SUMMARY_ID`),
  KEY `RATINGSUMM_ITEM_INDEX` (`ITEM_ID`),
  KEY `RATINGSUMM_TYPE_INDEX` (`RATING_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_review_detail`
--

DROP TABLE IF EXISTS `blc_review_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_review_detail` (
  `REVIEW_DETAIL_ID` bigint(20) NOT NULL,
  `HELPFUL_COUNT` int(11) NOT NULL,
  `NOT_HELPFUL_COUNT` int(11) NOT NULL,
  `REVIEW_SUBMITTED_DATE` datetime NOT NULL,
  `REVIEW_STATUS` varchar(255) NOT NULL,
  `REVIEW_TEXT` varchar(255) NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `RATING_DETAIL_ID` bigint(20) DEFAULT NULL,
  `RATING_SUMMARY_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`REVIEW_DETAIL_ID`),
  KEY `REVIEWDETAIL_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `REVIEWDETAIL_RATING_INDEX` (`RATING_DETAIL_ID`),
  KEY `REVIEWDETAIL_SUMMARY_INDEX` (`RATING_SUMMARY_ID`),
  KEY `REVIEWDETAIL_STATUS_INDEX` (`REVIEW_STATUS`),
  CONSTRAINT `FKdc0r2t22u3ghe0ihma8dcd5y6` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`),
  CONSTRAINT `FKhp1g51hyv3y8gr2tedxm0pgyl` FOREIGN KEY (`RATING_DETAIL_ID`) REFERENCES `blc_rating_detail` (`RATING_DETAIL_ID`),
  CONSTRAINT `FKn9hvs3m8fhodmipm3fvmwhw74` FOREIGN KEY (`RATING_SUMMARY_ID`) REFERENCES `blc_rating_summary` (`RATING_SUMMARY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_review_feedback`
--

DROP TABLE IF EXISTS `blc_review_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_review_feedback` (
  `REVIEW_FEEDBACK_ID` bigint(20) NOT NULL,
  `IS_HELPFUL` bit(1) NOT NULL,
  `CUSTOMER_ID` bigint(20) NOT NULL,
  `REVIEW_DETAIL_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`REVIEW_FEEDBACK_ID`),
  KEY `REVIEWFEED_CUSTOMER_INDEX` (`CUSTOMER_ID`),
  KEY `REVIEWFEED_DETAIL_INDEX` (`REVIEW_DETAIL_ID`),
  CONSTRAINT `FKaledeh8wwn4ykopccqh3to8k5` FOREIGN KEY (`REVIEW_DETAIL_ID`) REFERENCES `blc_review_detail` (`REVIEW_DETAIL_ID`),
  CONSTRAINT `FKmppbg4bf4h8v1m9efgm10ty4b` FOREIGN KEY (`CUSTOMER_ID`) REFERENCES `blc_customer` (`CUSTOMER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_role`
--

DROP TABLE IF EXISTS `blc_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_role` (
  `ROLE_ID` bigint(20) NOT NULL,
  `ROLE_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`ROLE_ID`),
  KEY `ROLE_NAME_INDEX` (`ROLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sandbox`
--

DROP TABLE IF EXISTS `blc_sandbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sandbox` (
  `SANDBOX_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `AUTHOR` bigint(20) DEFAULT NULL,
  `COLOR` varchar(255) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `GO_LIVE_DATE` datetime DEFAULT NULL,
  `SANDBOX_NAME` varchar(255) DEFAULT NULL,
  `SANDBOX_TYPE` varchar(255) DEFAULT NULL,
  `PARENT_SANDBOX_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SANDBOX_ID`),
  KEY `SANDBOX_NAME_INDEX` (`SANDBOX_NAME`),
  KEY `FK5e7j7mfpr1en8q48yxkbjmduw` (`PARENT_SANDBOX_ID`),
  KEY `blc_sandbox_sandbox_type` (`SANDBOX_TYPE`),
  CONSTRAINT `FK5e7j7mfpr1en8q48yxkbjmduw` FOREIGN KEY (`PARENT_SANDBOX_ID`) REFERENCES `blc_sandbox` (`SANDBOX_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sandbox_mgmt`
--

DROP TABLE IF EXISTS `blc_sandbox_mgmt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sandbox_mgmt` (
  `SANDBOX_MGMT_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `SANDBOX_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SANDBOX_MGMT_ID`),
  UNIQUE KEY `UK_owins1o4pyal0j5pdlqfkd4b7` (`SANDBOX_ID`),
  CONSTRAINT `FKri581qivns8jshddbsl6m83hr` FOREIGN KEY (`SANDBOX_ID`) REFERENCES `blc_sandbox` (`SANDBOX_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc`
--

DROP TABLE IF EXISTS `blc_sc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc` (
  `SC_ID` bigint(20) NOT NULL,
  `CONTENT_NAME` varchar(255) NOT NULL,
  `OFFLINE_FLAG` bit(1) DEFAULT NULL,
  `PRIORITY` int(11) NOT NULL,
  `LOCALE_CODE` varchar(255) NOT NULL,
  `SC_TYPE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SC_ID`),
  KEY `CONTENT_NAME_INDEX` (`CONTENT_NAME`),
  KEY `SC_OFFLN_FLG_INDX` (`OFFLINE_FLAG`),
  KEY `CONTENT_PRIORITY_INDEX` (`PRIORITY`),
  KEY `FK13qnfvvq355l9cckfxkqqh59` (`LOCALE_CODE`),
  KEY `FKp9be5g25yydwn151wnwvbj9hu` (`SC_TYPE_ID`),
  CONSTRAINT `FK13qnfvvq355l9cckfxkqqh59` FOREIGN KEY (`LOCALE_CODE`) REFERENCES `blc_locale` (`LOCALE_CODE`),
  CONSTRAINT `FKp9be5g25yydwn151wnwvbj9hu` FOREIGN KEY (`SC_TYPE_ID`) REFERENCES `blc_sc_type` (`SC_TYPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_fld`
--

DROP TABLE IF EXISTS `blc_sc_fld`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_fld` (
  `SC_FLD_ID` bigint(20) NOT NULL,
  `FLD_KEY` varchar(255) DEFAULT NULL,
  `LOB_VALUE` longtext,
  `VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SC_FLD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_fld_map`
--

DROP TABLE IF EXISTS `blc_sc_fld_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_fld_map` (
  `BLC_SC_SC_FIELD_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  `SC_ID` bigint(20) NOT NULL,
  `SC_FLD_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`BLC_SC_SC_FIELD_ID`),
  KEY `FKh54fvkukkun10yu69gr7neyj9` (`SC_ID`),
  KEY `FKrwpb5a8l5uoeu4u046uihdx1g` (`SC_FLD_ID`),
  CONSTRAINT `FKh54fvkukkun10yu69gr7neyj9` FOREIGN KEY (`SC_ID`) REFERENCES `blc_sc` (`SC_ID`),
  CONSTRAINT `FKrwpb5a8l5uoeu4u046uihdx1g` FOREIGN KEY (`SC_FLD_ID`) REFERENCES `blc_sc_fld` (`SC_FLD_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_fld_tmplt`
--

DROP TABLE IF EXISTS `blc_sc_fld_tmplt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_fld_tmplt` (
  `SC_FLD_TMPLT_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SC_FLD_TMPLT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_fldgrp_xref`
--

DROP TABLE IF EXISTS `blc_sc_fldgrp_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_fldgrp_xref` (
  `BLC_SC_FLDGRP_XREF_ID` bigint(20) NOT NULL,
  `GROUP_ORDER` int(11) DEFAULT NULL,
  `FLD_GROUP_ID` bigint(20) DEFAULT NULL,
  `SC_FLD_TMPLT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`BLC_SC_FLDGRP_XREF_ID`),
  KEY `FKotfd5rhlje73tldskasabxd7k` (`FLD_GROUP_ID`),
  KEY `FKtqvhk2j6dxo8kruvflvpstgf` (`SC_FLD_TMPLT_ID`),
  CONSTRAINT `FKotfd5rhlje73tldskasabxd7k` FOREIGN KEY (`FLD_GROUP_ID`) REFERENCES `blc_fld_group` (`FLD_GROUP_ID`),
  CONSTRAINT `FKtqvhk2j6dxo8kruvflvpstgf` FOREIGN KEY (`SC_FLD_TMPLT_ID`) REFERENCES `blc_sc_fld_tmplt` (`SC_FLD_TMPLT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_item_criteria`
--

DROP TABLE IF EXISTS `blc_sc_item_criteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_item_criteria` (
  `SC_ITEM_CRITERIA_ID` bigint(20) NOT NULL,
  `ORDER_ITEM_MATCH_RULE` longtext,
  `QUANTITY` int(11) NOT NULL,
  `SC_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SC_ITEM_CRITERIA_ID`),
  KEY `FKi62rdb9fuxn6lfdo7d4q9haow` (`SC_ID`),
  CONSTRAINT `FKi62rdb9fuxn6lfdo7d4q9haow` FOREIGN KEY (`SC_ID`) REFERENCES `blc_sc` (`SC_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_rule`
--

DROP TABLE IF EXISTS `blc_sc_rule`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_rule` (
  `SC_RULE_ID` bigint(20) NOT NULL,
  `MATCH_RULE` longtext,
  PRIMARY KEY (`SC_RULE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_rule_map`
--

DROP TABLE IF EXISTS `blc_sc_rule_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_rule_map` (
  `BLC_SC_SC_ID` bigint(20) NOT NULL,
  `SC_RULE_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  PRIMARY KEY (`BLC_SC_SC_ID`,`MAP_KEY`),
  KEY `FK31d3qpemphv6qdbha0cl1x361` (`SC_RULE_ID`),
  CONSTRAINT `FK31d3qpemphv6qdbha0cl1x361` FOREIGN KEY (`SC_RULE_ID`) REFERENCES `blc_sc_rule` (`SC_RULE_ID`),
  CONSTRAINT `FKo4q8t9hx8iprk9bc9tllwhdhk` FOREIGN KEY (`BLC_SC_SC_ID`) REFERENCES `blc_sc` (`SC_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sc_type`
--

DROP TABLE IF EXISTS `blc_sc_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sc_type` (
  `SC_TYPE_ID` bigint(20) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `SC_FLD_TMPLT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SC_TYPE_ID`),
  KEY `SC_TYPE_NAME_INDEX` (`NAME`),
  KEY `FKh7idqa2kh7vusepjor3bc80b3` (`SC_FLD_TMPLT_ID`),
  CONSTRAINT `FKh7idqa2kh7vusepjor3bc80b3` FOREIGN KEY (`SC_FLD_TMPLT_ID`) REFERENCES `blc_sc_fld_tmplt` (`SC_FLD_TMPLT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_search_facet`
--

DROP TABLE IF EXISTS `blc_search_facet`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_search_facet` (
  `SEARCH_FACET_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `MULTISELECT` bit(1) DEFAULT NULL,
  `LABEL` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `REQUIRES_ALL_DEPENDENT` bit(1) DEFAULT NULL,
  `SEARCH_DISPLAY_PRIORITY` int(11) DEFAULT NULL,
  `SHOW_ON_SEARCH` bit(1) DEFAULT NULL,
  `USE_FACET_RANGES` bit(1) DEFAULT NULL,
  `INDEX_FIELD_TYPE_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SEARCH_FACET_ID`),
  KEY `FKrrhp7pwx3bjh2rhadrtv2ro81` (`INDEX_FIELD_TYPE_ID`),
  CONSTRAINT `FKrrhp7pwx3bjh2rhadrtv2ro81` FOREIGN KEY (`INDEX_FIELD_TYPE_ID`) REFERENCES `blc_index_field_type` (`INDEX_FIELD_TYPE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_search_facet_range`
--

DROP TABLE IF EXISTS `blc_search_facet_range`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_search_facet_range` (
  `SEARCH_FACET_RANGE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `MAX_VALUE` decimal(19,5) DEFAULT NULL,
  `MIN_VALUE` decimal(19,5) NOT NULL,
  `SEARCH_FACET_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SEARCH_FACET_RANGE_ID`),
  KEY `SEARCH_FACET_INDEX` (`SEARCH_FACET_ID`),
  CONSTRAINT `FKm1k6kfkc59c8jdx51qym3tcai` FOREIGN KEY (`SEARCH_FACET_ID`) REFERENCES `blc_search_facet` (`SEARCH_FACET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_search_facet_xref`
--

DROP TABLE IF EXISTS `blc_search_facet_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_search_facet_xref` (
  `ID` bigint(20) NOT NULL,
  `REQUIRED_FACET_ID` bigint(20) NOT NULL,
  `SEARCH_FACET_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `FK4xpicfgot9h1utp316cbi8268` (`REQUIRED_FACET_ID`),
  KEY `FKtdvsplwk8dl6mnb0p7fdyte13` (`SEARCH_FACET_ID`),
  CONSTRAINT `FK4xpicfgot9h1utp316cbi8268` FOREIGN KEY (`REQUIRED_FACET_ID`) REFERENCES `blc_search_facet` (`SEARCH_FACET_ID`),
  CONSTRAINT `FKtdvsplwk8dl6mnb0p7fdyte13` FOREIGN KEY (`SEARCH_FACET_ID`) REFERENCES `blc_search_facet` (`SEARCH_FACET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_search_intercept`
--

DROP TABLE IF EXISTS `blc_search_intercept`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_search_intercept` (
  `SEARCH_REDIRECT_ID` bigint(20) NOT NULL,
  `ACTIVE_END_DATE` datetime DEFAULT NULL,
  `ACTIVE_START_DATE` datetime DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `SEARCH_TERM` varchar(255) NOT NULL,
  `URL` varchar(255) NOT NULL,
  PRIMARY KEY (`SEARCH_REDIRECT_ID`),
  KEY `SEARCH_ACTIVE_INDEX` (`ACTIVE_END_DATE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_search_synonym`
--

DROP TABLE IF EXISTS `blc_search_synonym`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_search_synonym` (
  `SEARCH_SYNONYM_ID` bigint(20) NOT NULL,
  `SYNONYMS` varchar(255) DEFAULT NULL,
  `TERM` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SEARCH_SYNONYM_ID`),
  KEY `SEARCHSYNONYM_TERM_INDEX` (`TERM`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_shipping_rate`
--

DROP TABLE IF EXISTS `blc_shipping_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_shipping_rate` (
  `ID` bigint(20) NOT NULL,
  `BAND_RESULT_PCT` int(11) NOT NULL,
  `BAND_RESULT_QTY` decimal(19,2) NOT NULL,
  `BAND_UNIT_QTY` decimal(19,2) NOT NULL,
  `FEE_BAND` int(11) NOT NULL,
  `FEE_SUB_TYPE` varchar(255) DEFAULT NULL,
  `FEE_TYPE` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  KEY `SHIPPINGRATE_FEESUB_INDEX` (`FEE_SUB_TYPE`),
  KEY `SHIPPINGRATE_FEE_INDEX` (`FEE_TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_site`
--

DROP TABLE IF EXISTS `blc_site`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_site` (
  `SITE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `DEACTIVATED` bit(1) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `SITE_IDENTIFIER_TYPE` varchar(255) DEFAULT NULL,
  `SITE_IDENTIFIER_VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SITE_ID`),
  KEY `BLC_SITE_ID_VAL_INDEX` (`SITE_IDENTIFIER_VALUE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_site_catalog`
--

DROP TABLE IF EXISTS `blc_site_catalog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_site_catalog` (
  `SITE_CATALOG_XREF_ID` bigint(20) NOT NULL,
  `CATALOG_ID` bigint(20) NOT NULL,
  `SITE_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SITE_CATALOG_XREF_ID`),
  KEY `FKho5bxxfvt21ijan47er38vnyu` (`CATALOG_ID`),
  KEY `FKmktxeb1okchyhs2mxat1nk6s5` (`SITE_ID`),
  CONSTRAINT `FKho5bxxfvt21ijan47er38vnyu` FOREIGN KEY (`CATALOG_ID`) REFERENCES `blc_catalog` (`CATALOG_ID`),
  CONSTRAINT `FKmktxeb1okchyhs2mxat1nk6s5` FOREIGN KEY (`SITE_ID`) REFERENCES `blc_site` (`SITE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_site_map_cfg`
--

DROP TABLE IF EXISTS `blc_site_map_cfg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_site_map_cfg` (
  `INDEXED_SITE_MAP_FILE_NAME` varchar(255) DEFAULT NULL,
  `INDEXED_SITE_MAP_FILE_PATTERN` varchar(255) DEFAULT NULL,
  `MAX_URL_ENTRIES_PER_FILE` int(11) DEFAULT NULL,
  `SITE_MAP_FILE_NAME` varchar(255) DEFAULT NULL,
  `MODULE_CONFIG_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`MODULE_CONFIG_ID`),
  CONSTRAINT `FKdskgdyr42vk7c8g92bxir3wej` FOREIGN KEY (`MODULE_CONFIG_ID`) REFERENCES `blc_module_configuration` (`MODULE_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_site_map_gen_cfg`
--

DROP TABLE IF EXISTS `blc_site_map_gen_cfg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_site_map_gen_cfg` (
  `GEN_CONFIG_ID` bigint(20) NOT NULL,
  `CHANGE_FREQ` varchar(255) NOT NULL,
  `DISABLED` bit(1) NOT NULL,
  `GENERATOR_TYPE` varchar(255) NOT NULL,
  `PRIORITY` varchar(255) DEFAULT NULL,
  `MODULE_CONFIG_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`GEN_CONFIG_ID`),
  KEY `FKmw4fb38sdenx8kjrxg5v8mjei` (`MODULE_CONFIG_ID`),
  CONSTRAINT `FKmw4fb38sdenx8kjrxg5v8mjei` FOREIGN KEY (`MODULE_CONFIG_ID`) REFERENCES `blc_site_map_cfg` (`MODULE_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_site_map_url_entry`
--

DROP TABLE IF EXISTS `blc_site_map_url_entry`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_site_map_url_entry` (
  `URL_ENTRY_ID` bigint(20) NOT NULL,
  `CHANGE_FREQ` varchar(255) NOT NULL,
  `LAST_MODIFIED` datetime NOT NULL,
  `LOCATION` varchar(255) NOT NULL,
  `PRIORITY` varchar(255) NOT NULL,
  `GEN_CONFIG_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`URL_ENTRY_ID`),
  KEY `FKrvkeinfysjshg9ulmxno9rhla` (`GEN_CONFIG_ID`),
  CONSTRAINT `FKrvkeinfysjshg9ulmxno9rhla` FOREIGN KEY (`GEN_CONFIG_ID`) REFERENCES `blc_cust_site_map_gen_cfg` (`GEN_CONFIG_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku`
--

DROP TABLE IF EXISTS `blc_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku` (
  `SKU_ID` bigint(20) NOT NULL,
  `ACTIVE_END_DATE` datetime DEFAULT NULL,
  `ACTIVE_START_DATE` datetime DEFAULT NULL,
  `AVAILABLE_FLAG` char(1) DEFAULT NULL,
  `COST` decimal(19,5) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `CONTAINER_SHAPE` varchar(255) DEFAULT NULL,
  `DEPTH` decimal(19,2) DEFAULT NULL,
  `DIMENSION_UNIT_OF_MEASURE` varchar(255) DEFAULT NULL,
  `GIRTH` decimal(19,2) DEFAULT NULL,
  `HEIGHT` decimal(19,2) DEFAULT NULL,
  `CONTAINER_SIZE` varchar(255) DEFAULT NULL,
  `WIDTH` decimal(19,2) DEFAULT NULL,
  `DISCOUNTABLE_FLAG` char(1) DEFAULT NULL,
  `DISPLAY_TEMPLATE` varchar(255) DEFAULT NULL,
  `EXTERNAL_ID` varchar(255) DEFAULT NULL,
  `FULFILLMENT_TYPE` varchar(255) DEFAULT NULL,
  `INVENTORY_TYPE` varchar(255) DEFAULT NULL,
  `IS_MACHINE_SORTABLE` bit(1) DEFAULT NULL,
  `LONG_DESCRIPTION` longtext,
  `NAME` varchar(255) DEFAULT NULL,
  `QUANTITY_AVAILABLE` int(11) DEFAULT NULL,
  `RETAIL_PRICE` decimal(19,5) DEFAULT NULL,
  `SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `TAX_CODE` varchar(255) DEFAULT NULL,
  `TAXABLE_FLAG` char(1) DEFAULT NULL,
  `UPC` varchar(255) DEFAULT NULL,
  `URL_KEY` varchar(255) DEFAULT NULL,
  `WEIGHT` decimal(19,2) DEFAULT NULL,
  `WEIGHT_UNIT_OF_MEASURE` varchar(255) DEFAULT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  `DEFAULT_PRODUCT_ID` bigint(20) DEFAULT NULL,
  `ADDL_PRODUCT_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SKU_ID`),
  KEY `SKU_ACTIVE_END_INDEX` (`ACTIVE_END_DATE`),
  KEY `SKU_ACTIVE_START_INDEX` (`ACTIVE_START_DATE`),
  KEY `SKU_AVAILABLE_INDEX` (`AVAILABLE_FLAG`),
  KEY `SKU_DISCOUNTABLE_INDEX` (`DISCOUNTABLE_FLAG`),
  KEY `SKU_EXTERNAL_ID_INDEX` (`EXTERNAL_ID`),
  KEY `SKU_NAME_INDEX` (`NAME`),
  KEY `SKU_TAXABLE_INDEX` (`TAXABLE_FLAG`),
  KEY `SKU_UPC_INDEX` (`UPC`),
  KEY `SKU_URL_KEY_INDEX` (`URL_KEY`),
  KEY `FKdowfc15iv11csxhs4itbfbowh` (`CURRENCY_CODE`),
  KEY `FK9akww2wqlf0xt7rvupxtv193c` (`DEFAULT_PRODUCT_ID`),
  KEY `FKseqmjfh1kdphq3eeplsuf6nj4` (`ADDL_PRODUCT_ID`),
  CONSTRAINT `FK9akww2wqlf0xt7rvupxtv193c` FOREIGN KEY (`DEFAULT_PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`),
  CONSTRAINT `FKdowfc15iv11csxhs4itbfbowh` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`),
  CONSTRAINT `FKseqmjfh1kdphq3eeplsuf6nj4` FOREIGN KEY (`ADDL_PRODUCT_ID`) REFERENCES `blc_product` (`PRODUCT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_attribute`
--

DROP TABLE IF EXISTS `blc_sku_attribute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_attribute` (
  `SKU_ATTR_ID` bigint(20) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `VALUE` varchar(255) NOT NULL,
  `SKU_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SKU_ATTR_ID`),
  KEY `SKUATTR_NAME_INDEX` (`NAME`),
  KEY `SKUATTR_SKU_INDEX` (`SKU_ID`),
  CONSTRAINT `FK6w8gul2489hdbmxha9ftu6qbq` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_availability`
--

DROP TABLE IF EXISTS `blc_sku_availability`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_availability` (
  `SKU_AVAILABILITY_ID` bigint(20) NOT NULL,
  `AVAILABILITY_DATE` datetime DEFAULT NULL,
  `AVAILABILITY_STATUS` varchar(255) DEFAULT NULL,
  `LOCATION_ID` bigint(20) DEFAULT NULL,
  `QTY_ON_HAND` int(11) DEFAULT NULL,
  `RESERVE_QTY` int(11) DEFAULT NULL,
  `SKU_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`SKU_AVAILABILITY_ID`),
  KEY `SKUAVAIL_STATUS_INDEX` (`AVAILABILITY_STATUS`),
  KEY `SKUAVAIL_LOCATION_INDEX` (`LOCATION_ID`),
  KEY `SKUAVAIL_SKU_INDEX` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_bundle_item`
--

DROP TABLE IF EXISTS `blc_sku_bundle_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_bundle_item` (
  `SKU_BUNDLE_ITEM_ID` bigint(20) NOT NULL,
  `ITEM_SALE_PRICE` decimal(19,5) DEFAULT NULL,
  `QUANTITY` int(11) NOT NULL,
  `SEQUENCE` decimal(10,6) DEFAULT NULL,
  `PRODUCT_BUNDLE_ID` bigint(20) NOT NULL,
  `SKU_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SKU_BUNDLE_ITEM_ID`),
  KEY `FK78yrrdqjalrqrei5kfnybkrs8` (`PRODUCT_BUNDLE_ID`),
  KEY `FKbhe93esmsur5uyhl0v6dj392t` (`SKU_ID`),
  CONSTRAINT `FK78yrrdqjalrqrei5kfnybkrs8` FOREIGN KEY (`PRODUCT_BUNDLE_ID`) REFERENCES `blc_product_bundle` (`PRODUCT_ID`),
  CONSTRAINT `FKbhe93esmsur5uyhl0v6dj392t` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_fee`
--

DROP TABLE IF EXISTS `blc_sku_fee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_fee` (
  `SKU_FEE_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,5) NOT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `EXPRESSION` longtext,
  `FEE_TYPE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `TAXABLE` bit(1) DEFAULT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`SKU_FEE_ID`),
  KEY `FKm9vf5c5ktjqu4wilne2f6926k` (`CURRENCY_CODE`),
  CONSTRAINT `FKm9vf5c5ktjqu4wilne2f6926k` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_fee_xref`
--

DROP TABLE IF EXISTS `blc_sku_fee_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_fee_xref` (
  `SKU_FEE_ID` bigint(20) NOT NULL,
  `SKU_ID` bigint(20) NOT NULL,
  KEY `FK3vmkt7ojjlpk2fle4cp8eq55f` (`SKU_ID`),
  KEY `FKky8dxmeg4o49qyc7kiwojnuek` (`SKU_FEE_ID`),
  CONSTRAINT `FK3vmkt7ojjlpk2fle4cp8eq55f` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`),
  CONSTRAINT `FKky8dxmeg4o49qyc7kiwojnuek` FOREIGN KEY (`SKU_FEE_ID`) REFERENCES `blc_sku_fee` (`SKU_FEE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_fulfillment_excluded`
--

DROP TABLE IF EXISTS `blc_sku_fulfillment_excluded`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_fulfillment_excluded` (
  `SKU_ID` bigint(20) NOT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  KEY `FKbf81qj596ta3xs2erw4o7m1ft` (`FULFILLMENT_OPTION_ID`),
  KEY `FKs0toanodthismt1hugerli3e8` (`SKU_ID`),
  CONSTRAINT `FKbf81qj596ta3xs2erw4o7m1ft` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FKs0toanodthismt1hugerli3e8` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_fulfillment_flat_rates`
--

DROP TABLE IF EXISTS `blc_sku_fulfillment_flat_rates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_fulfillment_flat_rates` (
  `SKU_ID` bigint(20) NOT NULL,
  `RATE` decimal(19,5) DEFAULT NULL,
  `FULFILLMENT_OPTION_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SKU_ID`,`FULFILLMENT_OPTION_ID`),
  KEY `FKklcbu8knfitgnhlj1sa2vyd30` (`FULFILLMENT_OPTION_ID`),
  CONSTRAINT `FK1ruxosbsx27uicd9dny1ls9td` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`),
  CONSTRAINT `FKklcbu8knfitgnhlj1sa2vyd30` FOREIGN KEY (`FULFILLMENT_OPTION_ID`) REFERENCES `blc_fulfillment_option` (`FULFILLMENT_OPTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_media_map`
--

DROP TABLE IF EXISTS `blc_sku_media_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_media_map` (
  `SKU_MEDIA_ID` bigint(20) NOT NULL,
  `MAP_KEY` varchar(255) NOT NULL,
  `MEDIA_ID` bigint(20) DEFAULT NULL,
  `BLC_SKU_SKU_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SKU_MEDIA_ID`),
  KEY `FKl3netvy66i56mjj6bo43mjmn2` (`MEDIA_ID`),
  KEY `FKc3mu07614ovbqwbnd1lxdg2ac` (`BLC_SKU_SKU_ID`),
  CONSTRAINT `FKc3mu07614ovbqwbnd1lxdg2ac` FOREIGN KEY (`BLC_SKU_SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`),
  CONSTRAINT `FKl3netvy66i56mjj6bo43mjmn2` FOREIGN KEY (`MEDIA_ID`) REFERENCES `blc_media` (`MEDIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_sku_option_value_xref`
--

DROP TABLE IF EXISTS `blc_sku_option_value_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_sku_option_value_xref` (
  `SKU_OPTION_VALUE_XREF_ID` bigint(20) NOT NULL,
  `PRODUCT_OPTION_VALUE_ID` bigint(20) NOT NULL,
  `SKU_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`SKU_OPTION_VALUE_XREF_ID`),
  KEY `FKc9e8sa4v1mqlbhd9hjp6bxujh` (`PRODUCT_OPTION_VALUE_ID`),
  KEY `FKci6tv12pfsh2srrakx3ridy8v` (`SKU_ID`),
  CONSTRAINT `FKc9e8sa4v1mqlbhd9hjp6bxujh` FOREIGN KEY (`PRODUCT_OPTION_VALUE_ID`) REFERENCES `blc_product_option_value` (`PRODUCT_OPTION_VALUE_ID`),
  CONSTRAINT `FKci6tv12pfsh2srrakx3ridy8v` FOREIGN KEY (`SKU_ID`) REFERENCES `blc_sku` (`SKU_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_state`
--

DROP TABLE IF EXISTS `blc_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_state` (
  `ABBREVIATION` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `COUNTRY` varchar(255) NOT NULL,
  PRIMARY KEY (`ABBREVIATION`),
  KEY `STATE_NAME_INDEX` (`NAME`),
  KEY `FKr63uepn8p8qx02xcg43851gno` (`COUNTRY`),
  KEY `BLC_STATE_name` (`NAME`),
  CONSTRAINT `FKr63uepn8p8qx02xcg43851gno` FOREIGN KEY (`COUNTRY`) REFERENCES `blc_country` (`ABBREVIATION`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_static_asset`
--

DROP TABLE IF EXISTS `blc_static_asset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_static_asset` (
  `STATIC_ASSET_ID` bigint(20) NOT NULL,
  `ALT_TEXT` varchar(255) DEFAULT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `FILE_EXTENSION` varchar(255) DEFAULT NULL,
  `FILE_SIZE` bigint(20) DEFAULT NULL,
  `FULL_URL` varchar(255) NOT NULL,
  `MIME_TYPE` varchar(255) DEFAULT NULL,
  `NAME` varchar(255) NOT NULL,
  `STORAGE_TYPE` varchar(255) DEFAULT NULL,
  `TITLE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`STATIC_ASSET_ID`),
  KEY `ASST_FULL_URL_INDX` (`FULL_URL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_static_asset_desc`
--

DROP TABLE IF EXISTS `blc_static_asset_desc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_static_asset_desc` (
  `STATIC_ASSET_DESC_ID` bigint(20) NOT NULL,
  `CREATED_BY` bigint(20) DEFAULT NULL,
  `DATE_CREATED` datetime DEFAULT NULL,
  `DATE_UPDATED` datetime DEFAULT NULL,
  `UPDATED_BY` bigint(20) DEFAULT NULL,
  `DESCRIPTION` varchar(255) DEFAULT NULL,
  `LONG_DESCRIPTION` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`STATIC_ASSET_DESC_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_static_asset_strg`
--

DROP TABLE IF EXISTS `blc_static_asset_strg`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_static_asset_strg` (
  `STATIC_ASSET_STRG_ID` bigint(20) NOT NULL,
  `FILE_DATA` longblob,
  `STATIC_ASSET_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`STATIC_ASSET_STRG_ID`),
  KEY `STATIC_ASSET_ID_INDEX` (`STATIC_ASSET_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_store`
--

DROP TABLE IF EXISTS `blc_store`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_store` (
  `STORE_ID` bigint(20) NOT NULL,
  `ARCHIVED` char(1) DEFAULT NULL,
  `LATITUDE` double DEFAULT NULL,
  `LONGITUDE` double DEFAULT NULL,
  `STORE_NAME` varchar(255) NOT NULL,
  `STORE_OPEN` bit(1) DEFAULT NULL,
  `STORE_HOURS` varchar(255) DEFAULT NULL,
  `STORE_NUMBER` varchar(255) DEFAULT NULL,
  `ADDRESS_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`STORE_ID`),
  KEY `FKg65fln1wkn5rai85klf8ei1uy` (`ADDRESS_ID`),
  CONSTRAINT `FKg65fln1wkn5rai85klf8ei1uy` FOREIGN KEY (`ADDRESS_ID`) REFERENCES `blc_address` (`ADDRESS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_system_property`
--

DROP TABLE IF EXISTS `blc_system_property`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_system_property` (
  `BLC_SYSTEM_PROPERTY_ID` bigint(20) NOT NULL,
  `FRIENDLY_GROUP` varchar(255) DEFAULT NULL,
  `FRIENDLY_NAME` varchar(255) DEFAULT NULL,
  `FRIENDLY_TAB` varchar(255) DEFAULT NULL,
  `PROPERTY_NAME` varchar(255) NOT NULL,
  `OVERRIDE_GENERATED_PROP_NAME` bit(1) DEFAULT NULL,
  `PROPERTY_TYPE` varchar(255) DEFAULT NULL,
  `PROPERTY_VALUE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`BLC_SYSTEM_PROPERTY_ID`),
  KEY `BLC_SYSTEM_PROPERTY_property_name` (`PROPERTY_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_tar_crit_offer_xref`
--

DROP TABLE IF EXISTS `blc_tar_crit_offer_xref`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_tar_crit_offer_xref` (
  `OFFER_TAR_CRIT_ID` bigint(20) NOT NULL,
  `OFFER_ID` bigint(20) NOT NULL,
  `OFFER_ITEM_CRITERIA_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`OFFER_TAR_CRIT_ID`),
  KEY `FK5n28fyhs3hvqqn38rap5yns9i` (`OFFER_ID`),
  KEY `FKj44eau35bu6hfq5w53civq01y` (`OFFER_ITEM_CRITERIA_ID`),
  CONSTRAINT `FK5n28fyhs3hvqqn38rap5yns9i` FOREIGN KEY (`OFFER_ID`) REFERENCES `blc_offer` (`OFFER_ID`),
  CONSTRAINT `FKj44eau35bu6hfq5w53civq01y` FOREIGN KEY (`OFFER_ITEM_CRITERIA_ID`) REFERENCES `blc_offer_item_criteria` (`OFFER_ITEM_CRITERIA_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_tax_detail`
--

DROP TABLE IF EXISTS `blc_tax_detail`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_tax_detail` (
  `TAX_DETAIL_ID` bigint(20) NOT NULL,
  `AMOUNT` decimal(19,5) DEFAULT NULL,
  `TAX_COUNTRY` varchar(255) DEFAULT NULL,
  `JURISDICTION_NAME` varchar(255) DEFAULT NULL,
  `RATE` decimal(19,5) DEFAULT NULL,
  `TAX_REGION` varchar(255) DEFAULT NULL,
  `TAX_NAME` varchar(255) DEFAULT NULL,
  `TYPE` varchar(255) DEFAULT NULL,
  `CURRENCY_CODE` varchar(255) DEFAULT NULL,
  `MODULE_CONFIG_ID` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`TAX_DETAIL_ID`),
  KEY `FK7rwcm52210yymslbjj8m25cvi` (`CURRENCY_CODE`),
  KEY `FK53heksajqlpbnfd8yrbudum8a` (`MODULE_CONFIG_ID`),
  CONSTRAINT `FK53heksajqlpbnfd8yrbudum8a` FOREIGN KEY (`MODULE_CONFIG_ID`) REFERENCES `blc_module_configuration` (`MODULE_CONFIG_ID`),
  CONSTRAINT `FK7rwcm52210yymslbjj8m25cvi` FOREIGN KEY (`CURRENCY_CODE`) REFERENCES `blc_currency` (`CURRENCY_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_trans_additnl_fields`
--

DROP TABLE IF EXISTS `blc_trans_additnl_fields`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_trans_additnl_fields` (
  `PAYMENT_TRANSACTION_ID` bigint(20) NOT NULL,
  `FIELD_VALUE` longtext,
  `FIELD_NAME` varchar(255) NOT NULL,
  PRIMARY KEY (`PAYMENT_TRANSACTION_ID`,`FIELD_NAME`),
  CONSTRAINT `FKdmq1toto9pwrhw5uife2ssq45` FOREIGN KEY (`PAYMENT_TRANSACTION_ID`) REFERENCES `blc_order_payment_transaction` (`PAYMENT_TRANSACTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_translation`
--

DROP TABLE IF EXISTS `blc_translation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_translation` (
  `TRANSLATION_ID` bigint(20) NOT NULL,
  `ENTITY_ID` varchar(255) DEFAULT NULL,
  `ENTITY_TYPE` varchar(255) DEFAULT NULL,
  `FIELD_NAME` varchar(255) DEFAULT NULL,
  `LOCALE_CODE` varchar(255) DEFAULT NULL,
  `TRANSLATED_VALUE` longtext,
  PRIMARY KEY (`TRANSLATION_ID`),
  KEY `TRANSLATION_INDEX` (`ENTITY_TYPE`,`ENTITY_ID`,`FIELD_NAME`,`LOCALE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_url_handler`
--

DROP TABLE IF EXISTS `blc_url_handler`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_url_handler` (
  `URL_HANDLER_ID` bigint(20) NOT NULL,
  `INCOMING_URL` varchar(255) NOT NULL,
  `IS_REGEX` bit(1) DEFAULT NULL,
  `NEW_URL` varchar(255) NOT NULL,
  `URL_REDIRECT_TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`URL_HANDLER_ID`),
  KEY `INCOMING_URL_INDEX` (`INCOMING_URL`),
  KEY `IS_REGEX_HANDLER_INDEX` (`IS_REGEX`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_userconnection`
--

DROP TABLE IF EXISTS `blc_userconnection`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_userconnection` (
  `providerId` varchar(255) NOT NULL,
  `providerUserId` varchar(255) NOT NULL,
  `userId` varchar(255) NOT NULL,
  `accessToken` varchar(255) NOT NULL,
  `displayName` varchar(255) DEFAULT NULL,
  `expireTime` bigint(20) DEFAULT NULL,
  `imageUrl` varchar(255) DEFAULT NULL,
  `profileUrl` varchar(255) DEFAULT NULL,
  `rank` int(11) NOT NULL,
  `refreshToken` varchar(255) DEFAULT NULL,
  `secret` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`providerId`,`providerUserId`,`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `blc_zip_code`
--

DROP TABLE IF EXISTS `blc_zip_code`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `blc_zip_code` (
  `ZIP_CODE_ID` varchar(255) NOT NULL,
  `ZIP_CITY` varchar(255) DEFAULT NULL,
  `ZIP_LATITUDE` double DEFAULT NULL,
  `ZIP_LONGITUDE` double DEFAULT NULL,
  `ZIP_STATE` varchar(255) DEFAULT NULL,
  `ZIPCODE` int(11) DEFAULT NULL,
  PRIMARY KEY (`ZIP_CODE_ID`),
  KEY `ZIPCODE_CITY_INDEX` (`ZIP_CITY`),
  KEY `ZIPCODE_LATITUDE_INDEX` (`ZIP_LATITUDE`),
  KEY `ZIPCODE_LONGITUDE_INDEX` (`ZIP_LONGITUDE`),
  KEY `ZIPCODE_STATE_INDEX` (`ZIP_STATE`),
  KEY `ZIPCODE_ZIP_INDEX` (`ZIPCODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sequence_generator`
--

DROP TABLE IF EXISTS `sequence_generator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sequence_generator` (
  `ID_NAME` varchar(255) NOT NULL,
  `ID_VAL` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`ID_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2020-04-01 15:19:27

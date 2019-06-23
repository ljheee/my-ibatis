/*
 Navicat Premium Data Transfer

 Source Server         : local-demo
 Source Server Type    : MySQL
 Source Server Version : 80011
 Source Host           : localhost
 Source Database       : test

 Target Server Type    : MySQL
 Target Server Version : 80011
 File Encoding         : utf-8

 Date: 06/23/2019 12:03:59 PM
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `user`
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `appid` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `passwd` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;

-- ----------------------------
--  Records of `user`
-- ----------------------------
BEGIN;
INSERT INTO `user` VALUES ('10', '95955542783', 'abc', '123'), ('11', '95955542780', 'ljh', '111'), ('12', '95955542781', 'qqq', 'qqq'), ('13', '959', 'aa', 'aa'), ('14', '2121', 'rr', 'rr'), ('15', '3443', '434', '434'), ('16', '434', '434', '434'), ('17', '2122', '222', '222');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;

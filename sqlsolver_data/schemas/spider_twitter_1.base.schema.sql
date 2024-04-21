CREATE TABLE `follows`
(
    `f1` int(11) NOT NULL,
    `f2` int(11) NOT NULL,
    PRIMARY KEY (`f1`, `f2`),
    FOREIGN KEY (`f1`) REFERENCES `user_profiles` (`uid`),
    FOREIGN KEY (`f2`) REFERENCES `user_profiles` (`uid`)
);

CREATE TABLE `tweets`
(
    `id`         bigint(20) NOT NULL,
    `uid`        int(11)    NOT NULL,
    `text`       char(140)  NOT NULL,
    `createdate` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`uid`) REFERENCES `user_profiles` (`uid`)
);

CREATE TABLE IF NOT EXISTS `user_profiles`
(
    `uid`         int(11) NOT NULL,
    `name`        varchar(255) DEFAULT NULL,
    `email`       varchar(255) DEFAULT NULL,
    `partitionid` int(11)      DEFAULT NULL,
    `followers`   int(11)      DEFAULT NULL,
    PRIMARY KEY (`uid`)
);
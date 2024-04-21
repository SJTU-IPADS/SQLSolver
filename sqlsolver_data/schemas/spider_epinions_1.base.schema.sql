CREATE TABLE `item` (
                        `i_id` int NOT NULL,
                        `title` varchar(20) DEFAULT NULL,
                        PRIMARY KEY (`i_id`)
);

CREATE TABLE `review` (
                          `a_id` int NOT NULL PRIMARY KEY,
                          `u_id` int NOT NULL,
                          `i_id` int NOT NULL,
                          `rating` int DEFAULT NULL,
                          `rank` int DEFAULT NULL,
                          FOREIGN KEY (`u_id`) REFERENCES `useracct`(`u_id`),
                          FOREIGN KEY (`i_id`) REFERENCES `item`(`i_id`)
);

CREATE TABLE `useracct` (
                            `u_id` int NOT NULL,
                            `name` varchar(128) DEFAULT NULL,
                            PRIMARY KEY (`u_id`)
);

CREATE INDEX `idx_review_IDX_RATING_UID` ON `review` (`u_id`);
CREATE INDEX `idx_review_IDX_RATING_AID` ON `review` (`a_id`);
CREATE INDEX `idx_review_IDX_RATING_IID` ON `review` (`i_id`);

CREATE TABLE IF NOT EXISTS `trust` (
                                       `source_u_id` int NOT NULL,
                                       `target_u_id` int NOT NULL,
                                       `trust` int NOT NULL,
                                       FOREIGN KEY (`source_u_id`) REFERENCES `useracct`(`u_id`),
                                       FOREIGN KEY (`target_u_id`) REFERENCES `useracct`(`u_id`)
);

CREATE INDEX `idx_trust_IDX_TRUST_SID` ON `trust` (`source_u_id`);
CREATE INDEX `idx_trust_IDX_TRUST_TID` ON `trust` (`target_u_id`);
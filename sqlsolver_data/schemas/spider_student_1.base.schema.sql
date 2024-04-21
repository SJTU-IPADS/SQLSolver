CREATE TABLE IF NOT EXISTS `list`
(
    `LastName`  TEXT,
    `FirstName` TEXT,
    `Grade`     INTEGER,
    `Classroom` INTEGER,
    PRIMARY KEY (`LastName`, `FirstName`)
);

CREATE TABLE IF NOT EXISTS `teachers`
(
    `LastName`  TEXT,
    `FirstName` TEXT,
    `Classroom` INTEGER,
    PRIMARY KEY (`LastName`, `FirstName`)
);

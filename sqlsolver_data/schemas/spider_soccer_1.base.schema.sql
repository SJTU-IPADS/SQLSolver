CREATE TABLE IF NOT EXISTS `Player_Attributes` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `player_fifa_api_id` INT,
    `player_api_id` INT,
    `date` TEXT,
    `overall_rating` INT,
    `potential` INT,
    `preferred_foot` TEXT,
    `attacking_work_rate` TEXT,
    `defensive_work_rate` TEXT,
    `crossing` INT,
    `finishing` INT,
    `heading_accuracy` INT,
    `short_passing` INT,
    `volleys` INT,
    `dribbling` INT,
    `curve` INT,
    `free_kick_accuracy` INT,
    `long_passing` INT,
    `ball_control` INT,
    `acceleration` INT,
    `sprint_speed` INT,
    `agility` INT,
    `reactions` INT,
    `balance` INT,
    `shot_power` INT,
    `jumping` INT,
    `stamina` INT,
    `strength` INT,
    `long_shots` INT,
    `aggression` INT,
    `interceptions` INT,
    `positioning` INT,
    `vision` INT,
    `penalties` INT,
    `marking` INT,
    `standing_tackle` INT,
    `sliding_tackle` INT,
    `gk_diving` INT,
    `gk_handling` INT,
    `gk_kicking` INT,
    `gk_positioning` INT,
    `gk_reflexes` INT,
    FOREIGN KEY(`player_fifa_api_id`) REFERENCES `Player`(`player_fifa_api_id`),
    FOREIGN KEY(`player_api_id`) REFERENCES `Player`(`player_api_id`)
);

CREATE TABLE `Player` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `player_api_id` INT UNIQUE,
    `player_name` TEXT,
    `player_fifa_api_id` INT UNIQUE,
    `birthday` TEXT,
    `height` INT,
    `weight` INT
);

CREATE TABLE `League` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `country_id` INT,
    `name` TEXT UNIQUE,
    FOREIGN KEY(`country_id`) REFERENCES `Country`(`id`)
);

CREATE TABLE `Country` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` TEXT UNIQUE
);

CREATE TABLE IF NOT EXISTS `Team` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `team_api_id` INT UNIQUE,
    `team_fifa_api_id` INT,
    `team_long_name` TEXT,
    `team_short_name` TEXT
);

CREATE TABLE `Team_Attributes` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `team_fifa_api_id` INT,
    `team_api_id` INT,
    `date` TEXT,
    `buildUpPlaySpeed` INT,
    `buildUpPlaySpeedClass` TEXT,
    `buildUpPlayDribbling` INT,
    `buildUpPlayDribblingClass` TEXT,
    `buildUpPlayPassing` INT,
    `buildUpPlayPassingClass` TEXT,
    `buildUpPlayPositioningClass` TEXT,
    `chanceCreationPassing` INT,
    `chanceCreationPassingClass` TEXT,
    `chanceCreationCrossing` INT,
    `chanceCreationCrossingClass` TEXT,
    `chanceCreationShooting` INT,
    `chanceCreationShootingClass` TEXT,
    `chanceCreationPositioningClass` TEXT,
    `defencePressure` INT,
    `defencePressureClass` TEXT,
    `defenceAggression` INT,
    `defenceAggressionClass` TEXT,
    `defenceTeamWidth` INT,
    `defenceTeamWidthClass` TEXT,
    `defenceDefenderLineClass` TEXT,
    FOREIGN KEY(`team_fifa_api_id`) REFERENCES `Team`(`team_fifa_api_id`),
    FOREIGN KEY(`team_api_id`) REFERENCES `Team`(`team_api_id`)
);
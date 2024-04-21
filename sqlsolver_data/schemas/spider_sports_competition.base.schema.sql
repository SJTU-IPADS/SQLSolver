


DROP TABLE IF EXISTS club;
CREATE TABLE IF NOT EXISTS club (
Club_ID int,
name text,
Region text,
Start_year text,
PRIMARY KEY (Club_ID)
);









DROP TABLE IF EXISTS club_rank;
CREATE TABLE IF NOT EXISTS club_rank (
Rank real,
Club_ID int,
Gold real,
Silver real,
Bronze real,
Total real,
PRIMARY KEY (Rank,Club_ID)
FOREIGN KEY (Club_ID) REFERENCES club(Club_ID)
);

DROP TABLE IF EXISTS player;
CREATE TABLE IF NOT EXISTS player (
Player_ID int,
name text,
Position text,
Club_ID int,
Apps real,
Tries real,
Goals text,
Points real,
PRIMARY KEY (Player_ID),
FOREIGN KEY (Club_ID) REFERENCES club(Club_ID)
);

DROP TABLE IF EXISTS competition;
CREATE TABLE IF NOT EXISTS competition (
Competition_ID int,
Year real,
Competition_type text,
Country text,
PRIMARY KEY (Competition_ID)
);




































DROP TABLE IF EXISTS competition_result;
CREATE TABLE IF NOT EXISTS competition_result (
Competition_ID int,
Club_ID_1 int,
Club_ID_2 int,
Score text,
PRIMARY KEY (Competition_ID,Club_ID_1,Club_ID_2),
FOREIGN KEY (Club_ID_1) REFERENCES club(Club_ID),
FOREIGN KEY (Club_ID_2) REFERENCES club(Club_ID),
FOREIGN KEY (Competition_ID) REFERENCES competition(Competition_ID)
);









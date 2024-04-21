DROP TABLE IF EXISTS basketball_match;
CREATE TABLE IF NOT EXISTS basketball_match (
Team_ID int,
School_ID int,
Team_Name text,
ACC_Regular_Season text,
ACC_Percent text,
ACC_Home text,
ACC_Road text,
All_Games text,
All_Games_Percent int,
All_Home text,
All_Road text,
All_Neutral text,
PRIMARY KEY (Team_ID),
FOREIGN KEY (School_ID) REFERENCES university(School_ID)
);

DROP TABLE IF EXISTS university;
CREATE TABLE IF NOT EXISTS university (
School_ID int,
School text,
Location text,
Founded real,
Affiliation text,
Enrollment real,
Nickname text,
Primary_conference text,
PRIMARY KEY (School_ID)
);













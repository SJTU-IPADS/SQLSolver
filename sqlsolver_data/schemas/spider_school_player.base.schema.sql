


DROP TABLE IF EXISTS school;
CREATE TABLE IF NOT EXISTS school (
School_ID int,
School text,
Location text,
Enrollment real,
Founded real,
Denomination text,
Boys_or_Girls text,
Day_or_Boarding text,
Year_Entered_Competition real,
School_Colors text,
PRIMARY KEY (School_Id)
);










DROP TABLE IF EXISTS school_details;
CREATE TABLE IF NOT EXISTS school_details (
School_ID int,
Nickname text,
Colors text,
League text,
Class text,
Division text,
PRIMARY KEY (School_Id),
FOREIGN KEY (School_ID) REFERENCES school(School_ID)
);










DROP TABLE IF EXISTS school_performance;
CREATE TABLE IF NOT EXISTS school_performance (
School_Id int,
School_Year text,
Class_A text,
Class_AA text,
PRIMARY KEY (School_Id,School_Year),
FOREIGN KEY (School_ID) REFERENCES school(School_ID)
);

























DROP TABLE IF EXISTS player;
CREATE TABLE IF NOT EXISTS player (
Player_ID int,
Player text,
Team text,
Age int,
Position text,
School_ID int,
PRIMARY KEY (Player_ID),
FOREIGN KEY (School_ID) REFERENCES school(School_ID)
);

























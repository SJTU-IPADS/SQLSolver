

DROP TABLE IF EXISTS college;
CREATE TABLE IF NOT EXISTS college (
College_ID int,
Name text,
Leader_Name text,
College_Location text,
PRIMARY KEY (College_ID)
);










DROP TABLE IF EXISTS member;
CREATE TABLE IF NOT EXISTS member (
Member_ID int,
Name text,
Country text,
College_ID int,
PRIMARY KEY (Member_ID),
FOREIGN KEY (College_ID) REFERENCES college(College_ID)
);














DROP TABLE IF EXISTS round;
CREATE TABLE IF NOT EXISTS round (
Round_ID int,
Member_ID int,
Decoration_Theme text,
Rank_in_Round int,
PRIMARY KEY (Member_ID,Round_ID),
FOREIGN KEY (Member_ID) REFERENCES member(Member_ID)
);










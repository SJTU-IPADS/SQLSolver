DROP TABLE IF EXISTS entrepreneur;
CREATE TABLE IF NOT EXISTS entrepreneur (
Entrepreneur_ID int,
People_ID int,
Company text,
Money_Requested real,
Investor text,
PRIMARY KEY (Entrepreneur_ID),
FOREIGN KEY (People_ID) REFERENCES people(People_ID)
);

DROP TABLE IF EXISTS people;
CREATE TABLE IF NOT EXISTS people (
People_ID int,
Name text,
Height real,
Weight real,
Date_of_Birth text,
PRIMARY KEY (People_ID)
);


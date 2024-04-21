DROP TABLE IF EXISTS city;
CREATE TABLE IF NOT EXISTS city (
City_ID int,
City text,
Hanzi text,
Hanyu_Pinyin text,
Regional_Population int,
GDP real,
PRIMARY KEY (City_ID)
);

DROP TABLE IF EXISTS matchh;
CREATE TABLE IF NOT EXISTS matchh (
matchh_ID int,
Date text,
Venue text,
Score text,
Result text,
Competition text,
PRIMARY KEY (matchh_ID)
);

DROP TABLE IF EXISTS temperature;
CREATE TABLE IF NOT EXISTS temperature (
City_ID int,
Jan real,
Feb real,
Mar real,
Apr real,
Jun real,
Jul real,
Aug real,
Sep real,
Oct real,
Nov real,
Dec real,
PRIMARY KEY (City_ID),
FOREIGN KEY (City_ID) REFERENCES city(City_ID)
);

DROP TABLE IF EXISTS hosting_city;
CREATE TABLE IF NOT EXISTS hosting_city (
  Year int,
  matchh_ID int,
  Host_City text,
  PRIMARY KEY (Year),
  FOREIGN KEY (Host_City) REFERENCES city(City_ID),
  FOREIGN KEY (matchh_ID) REFERENCES matchh(matchh_ID)
);


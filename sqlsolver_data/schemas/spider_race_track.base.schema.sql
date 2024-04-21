

DROP TABLE IF EXISTS race;
CREATE TABLE IF NOT EXISTS race (
Race_ID int,
Name text,
Class text,
Date text,
Track_ID text,
PRIMARY KEY (Race_ID),
FOREIGN KEY (Track_ID) REFERENCES track(Track_ID)
);

DROP TABLE IF EXISTS track;
CREATE TABLE IF NOT EXISTS track (
Track_ID int,
Name text,
Location text,
Seating real,
Year_Opened real,
PRIMARY KEY (Track_ID)
);




















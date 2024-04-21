

DROP TABLE IF EXISTS captain;
CREATE TABLE IF NOT EXISTS captain (
Captain_ID int,
Name text,
Ship_ID int,
age text,
Class text,
Rank text,
PRIMARY KEY (Captain_ID),
FOREIGN KEY (Ship_ID) REFERENCES Ship(Ship_ID)
);

DROP TABLE IF EXISTS Ship;
CREATE TABLE IF NOT EXISTS Ship (
Ship_ID int,
Name text,
Type text,
Built_Year real,
Class text,
Flag text,
PRIMARY KEY (Ship_ID)
);




















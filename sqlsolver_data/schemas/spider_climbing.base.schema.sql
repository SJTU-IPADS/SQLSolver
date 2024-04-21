

DROP TABLE IF EXISTS mountain;
CREATE TABLE IF NOT EXISTS mountain (
Mountain_ID int,
Name text,
Height real,
Prominence real,
Range text,
Country text,
PRIMARY KEY (Mountain_ID)
);

DROP TABLE IF EXISTS climber;
CREATE TABLE IF NOT EXISTS climber (
Climber_ID int,
Name text,
Country text,
Time text,
Points real,
Mountain_ID int,
PRIMARY KEY (Climber_ID),
FOREIGN KEY (Mountain_ID) REFERENCES mountain(Mountain_ID)
);






















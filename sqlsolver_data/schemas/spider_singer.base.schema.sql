CREATE TABLE singer (
Singer_ID INT,
Name VARCHAR(255),
Birth_Year REAL,
Net_Worth_Millions REAL,
Citizenship VARCHAR(255),
PRIMARY KEY (Singer_ID)
);

CREATE TABLE song (
Song_ID INT,
Title VARCHAR(255),
Singer_ID INT,
Sales REAL,
Highest_Position REAL,
PRIMARY KEY (Song_ID),
FOREIGN KEY (Singer_ID) REFERENCES singer(Singer_ID)
);
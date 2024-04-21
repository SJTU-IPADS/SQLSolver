CREATE TABLE stadium (
Stadium_ID INT,
Location VARCHAR(255),
Name VARCHAR(255),
Capacity INT,
Highest INT,
Lowest INT,
Average INT,
PRIMARY KEY (Stadium_ID)
);

CREATE TABLE singer (
Singer_ID INT,
Name VARCHAR(255),
Country VARCHAR(255),
Song_Name VARCHAR(255),
Song_release_year VARCHAR(255),
Age INT,
Is_male BOOLEAN,
PRIMARY KEY (Singer_ID)
);

CREATE TABLE concert (
concert_ID INT,
concert_Name VARCHAR(255),
Theme VARCHAR(255),
Stadium_ID INT,
Year VARCHAR(255),
PRIMARY KEY (concert_ID),
FOREIGN KEY (Stadium_ID) REFERENCES stadium(Stadium_ID)
);

CREATE TABLE singer_in_concert (
concert_ID INT,
Singer_ID INT,
PRIMARY KEY (concert_ID,Singer_ID),
FOREIGN KEY (concert_ID) REFERENCES concert(concert_ID),
FOREIGN KEY (Singer_ID) REFERENCES singer(Singer_ID)
);

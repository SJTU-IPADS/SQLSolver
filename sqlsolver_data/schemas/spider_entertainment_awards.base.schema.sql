


DROP TABLE IF EXISTS festival_detail;
CREATE TABLE IF NOT EXISTS festival_detail (
Festival_ID int,
Festival_Name text,
Chair_Name text,
Location text,
Year int,
Num_of_Audience int,
PRIMARY KEY (Festival_ID)
);








CREATE TABLE artwork (
Artwork_ID int,
Type text,
Name text,
PRIMARY KEY (Artwork_ID)
);












CREATE TABLE nomination (
Artwork_ID int,
Festival_ID int,
Result text,
PRIMARY KEY (Artwork_ID,Festival_ID),
FOREIGN KEY (Artwork_ID) REFERENCES artwork(Artwork_ID),
FOREIGN KEY (Festival_ID) REFERENCES festival_detail(Festival_ID)
);









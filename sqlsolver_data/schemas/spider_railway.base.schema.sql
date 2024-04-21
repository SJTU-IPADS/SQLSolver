


DROP TABLE IF EXISTS railway;
CREATE TABLE IF NOT EXISTS railway (
Railway_ID int,
Railway text,
Builder text,
Built text,
Wheels text,
Location text,
ObjectNumber text,
PRIMARY KEY (Railway_ID)
);
















DROP TABLE IF EXISTS train;
CREATE TABLE IF NOT EXISTS train (
Train_ID int,
Train_Num text,
Name text,
From text,
Arrival text,
Railway_ID int,
PRIMARY KEY (Train_ID),
FOREIGN KEY (Railway_ID) REFERENCES railway(Railway_ID)
);













DROP TABLE IF EXISTS manager;
CREATE TABLE IF NOT EXISTS manager (
Manager_ID int,
Name text,
Country text,
Working_year_starts text,
Age int,
Level int,
PRIMARY KEY (Manager_ID)
);











DROP TABLE IF EXISTS railway_manage;
CREATE TABLE IF NOT EXISTS railway_manage (
Railway_ID int,
Manager_ID int,
From_Year text,
PRIMARY KEY (Railway_ID,Manager_ID),
FOREIGN KEY (Manager_ID) REFERENCES manager(Manager_ID),
FOREIGN KEY (Railway_ID) REFERENCES railway(Railway_ID)
);







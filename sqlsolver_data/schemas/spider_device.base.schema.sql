

DROP TABLE IF EXISTS device;
CREATE TABLE IF NOT EXISTS device (
Device_ID int,
Device text,
Carrier text,
Package_Version text,
Applications text,
Software_Platform text,
PRIMARY KEY (Device_ID)
);

DROP TABLE IF EXISTS shop;
CREATE TABLE IF NOT EXISTS shop (
Shop_ID int,
Shop_Name text,
Location text,
Open_Date text,
Open_Year int,
PRIMARY KEY (Shop_ID)
);























DROP TABLE IF EXISTS stock;
CREATE TABLE IF NOT EXISTS stock (
Shop_ID int,
Device_ID int,
Quantity int,
PRIMARY KEY (Shop_ID,Device_ID),
FOREIGN KEY (Shop_ID) REFERENCES shop(Shop_ID),
FOREIGN KEY (Device_ID) REFERENCES device(Device_ID)
);











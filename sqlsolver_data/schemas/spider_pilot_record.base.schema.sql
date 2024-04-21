

DROP TABLE IF EXISTS aircraft;
CREATE TABLE IF NOT EXISTS aircraft (
Aircraft_ID int,
Order_Year int,
Manufacturer text,
Model text,
Fleet_Series text,
Powertrain text,
Fuel_Propulsion text,
PRIMARY KEY (Aircraft_ID)
);


DROP TABLE IF EXISTS pilot;
CREATE TABLE IF NOT EXISTS pilot (
Pilot_ID int,
Pilot_name text,
Rank int,
Age int,
Nationality text,
Position text,
Join_Year int,
Team text,
PRIMARY KEY (Pilot_ID)
);

















DROP TABLE IF EXISTS pilot_record;
CREATE TABLE IF NOT EXISTS pilot_record (
Record_ID int,
Pilot_ID int,
Aircraft_ID int,
Date text,
PRIMARY KEY (Pilot_ID, Aircraft_ID, Date),
FOREIGN KEY (Pilot_ID) REFERENCES pilot(Pilot_ID),
FOREIGN KEY (Aircraft_ID) REFERENCES aircraft(Aircraft_ID)
);









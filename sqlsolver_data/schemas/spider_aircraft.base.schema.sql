



CREATE TABLE pilot (
  Pilot_Id int(11) NOT NULL,
  Name varchar(50) NOT NULL,
  Age int(11) NOT NULL,
  PRIMARY KEY (Pilot_Id)
);















CREATE TABLE aircraft (
  Aircraft_ID int(11) NOT NULL,
  Aircraft varchar(50) NOT NULL,
  Description varchar(50) NOT NULL,
  Max_Gross_Weight varchar(50) NOT NULL,
  Total_disk_area varchar(50) NOT NULL,
  Max_disk_Loading varchar(50) NOT NULL,
  PRIMARY KEY (Aircraft_ID)
);


CREATE TABLE match (
Round real,
Location text,
Country text,
Date text,
Fastest_Qualifying text,
Winning_Pilot text,
Winning_Aircraft text,
PRIMARY KEY (Round),
FOREIGN KEY (Winning_Aircraft) REFERENCES aircraft(Aircraft_ID),
FOREIGN KEY (Winning_Pilot) REFERENCES pilot(Pilot_Id)
);

CREATE TABLE airport (
Airport_ID int,
Airport_Name text,
Total_Passengers real,
%_Change_2007 text,
International_Passengers real,
Domestic_Passengers real,
Transit_Passengers real,
Aircraft_Movements real,
Freight_Metric_Tonnes real,
PRIMARY KEY (Airport_ID)
);

CREATE TABLE airport_aircraft (
ID int,
Airport_ID int,
Aircraft_ID int,
PRIMARY KEY (Airport_ID,Aircraft_ID),
FOREIGN KEY (Airport_ID) REFERENCES airport(Airport_ID),
FOREIGN KEY (Aircraft_ID) REFERENCES aircraft(Aircraft_ID)
);







































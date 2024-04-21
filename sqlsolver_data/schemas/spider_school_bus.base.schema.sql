


DROP TABLE IF EXISTS driver;
CREATE TABLE IF NOT EXISTS driver (
Driver_ID int,
Name text,
Party text,
Home_city text,
Age int,
PRIMARY KEY (Driver_ID)
);

DROP TABLE IF EXISTS school;
CREATE TABLE IF NOT EXISTS school (
School_ID int,
Grade text,
School text,
Location text,
Type text,
PRIMARY KEY (School_ID)
);
























DROP TABLE IF EXISTS school_bus;
CREATE TABLE IF NOT EXISTS school_bus (
School_ID int,
Driver_ID int,
Years_Working int,
If_full_time bool,
PRIMARY KEY (School_ID,Driver_ID),
FOREIGN KEY (School_ID) REFERENCES school(School_ID),
FOREIGN KEY (Driver_ID) REFERENCES driver(Driver_ID)
);









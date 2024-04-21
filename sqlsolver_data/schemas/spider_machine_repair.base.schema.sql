


DROP TABLE IF EXISTS repair;
CREATE TABLE IF NOT EXISTS repair (
repair_ID int,
name text,
Launch_Date text,
Notes text,
PRIMARY KEY (repair_ID)
);











DROP TABLE IF EXISTS machine;
CREATE TABLE IF NOT EXISTS machine (
Machine_ID int,
Making_Year int,
Class text,
Team text,
Machine_series text,
value_points real,
quality_rank int,
PRIMARY KEY (Machine_ID)
);












DROP TABLE IF EXISTS technician;
CREATE TABLE IF NOT EXISTS technician (
technician_id real,
Name text,
Team text,
Starting_Year real,
Age int,
PRIMARY Key (technician_id)
);















DROP TABLE IF EXISTS repair_assignment;
CREATE TABLE IF NOT EXISTS repair_assignment (
technician_id int,
repair_ID int,
Machine_ID int,
PRIMARY Key (technician_id,repair_ID,Machine_ID),
FOREIGN KEY (technician_id) REFERENCES technician(technician_id),
FOREIGN KEY (repair_ID) REFERENCES repair(repair_ID),
FOREIGN KEY (Machine_ID) REFERENCES machine(Machine_ID)
);












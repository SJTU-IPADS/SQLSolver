CREATE TABLE conductor (
Conductor_ID INT,
Name VARCHAR(255),
Age INT,
Nationality VARCHAR(255),
Year_of_Work INT,
PRIMARY KEY (Conductor_ID)
);

CREATE TABLE orchestra (
Orchestra_ID INT,
Orchestra VARCHAR(255),
Conductor_ID INT,
Record_Company VARCHAR(255),
Year_of_Founded REAL,
Major_Record_Format VARCHAR(255),
PRIMARY KEY (Orchestra_ID),
FOREIGN KEY (Conductor_ID) REFERENCES conductor(Conductor_ID)
);

CREATE TABLE performance (
Performance_ID INT,
Orchestra_ID INT,
Type VARCHAR(255),
Date VARCHAR(255),
Official_ratings_millions REAL,
Weekly_rank VARCHAR(255),
Share VARCHAR(255),
PRIMARY KEY (Performance_ID),
FOREIGN KEY (Orchestra_ID) REFERENCES orchestra(Orchestra_ID)
);

CREATE TABLE show (
Show_ID INT,
Performance_ID INT,
If_first_show BOOLEAN,
Result VARCHAR(255),
Attendance REAL,
FOREIGN KEY (Performance_ID) REFERENCES performance(Performance_ID)
);
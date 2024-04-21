




DROP TABLE IF EXISTS conductor;
CREATE TABLE IF NOT EXISTS conductor (Conductor_ID int,Name text,birthday TIMESTAMP,Nationality text,Year_of_Work int,PRIMARY KEY (Conductor_ID));



















DROP TABLE IF EXISTS orchestra;
CREATE TABLE IF NOT EXISTS orchestra (Orchestra_ID int,Orchestra text,Conductor_ID int,Record_Company text,Year_of_Founded real,Major_Record_Format text,PRIMARY KEY (Orchestra_ID),FOREIGN KEY (Conductor_ID) REFERENCES conductor(Conductor_ID));

DROP TABLE IF EXISTS performance;
CREATE TABLE IF NOT EXISTS performance (Performance_ID int,Orchestra_ID int,Type text,Date text,Official_ratings_(millions) real,Weekly_rank text,Share text,PRIMARY KEY (Performance_ID),FOREIGN KEY (Orchestra_ID) REFERENCES orchestra(Orchestra_ID));

DROP TABLE IF EXISTS show;
CREATE TABLE IF NOT EXISTS show (Show_ID int,Performance_ID int,Result text,If_first_show bool,Attendance real,FOREIGN KEY (Performance_ID) REFERENCES performance(Performance_ID));






































DROP TABLE IF EXISTS film;
CREATE TABLE IF NOT EXISTS film (
Film_ID int,
Rank_in_series int,
Number_in_season int,
Title text,
Directed_by text,
Original_air_date text,
Production_code text,
PRIMARY KEY (Film_ID)
);

DROP TABLE IF EXISTS cinema;
CREATE TABLE IF NOT EXISTS cinema (
Cinema_ID int,
Name text,
Openning_year int,
Capacity int,
Location text,
PRIMARY KEY (Cinema_ID));


















DROP TABLE IF EXISTS schedule;
CREATE TABLE IF NOT EXISTS schedule (
Cinema_ID int,
Film_ID int,
Date text,
Show_times_per_day int,
Price float,
PRIMARY KEY (Cinema_ID,Film_ID),
FOREIGN KEY (Film_ID) REFERENCES film(Film_ID),
FOREIGN KEY (Cinema_ID) REFERENCES cinema(Cinema_ID)
);











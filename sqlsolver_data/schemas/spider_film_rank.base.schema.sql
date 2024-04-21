




DROP TABLE IF EXISTS film;
CREATE TABLE IF NOT EXISTS film (
Film_ID int,
Title text,
Studio text,
Director text,
Gross_in_dollar int,
PRIMARY KEY (Film_ID)
);

















DROP TABLE IF EXISTS market;
CREATE TABLE IF NOT EXISTS market (
Market_ID int,
Country text,
Number_cities int,
PRIMARY KEY (Market_ID)
);









DROP TABLE IF EXISTS film_market_estimation;
CREATE TABLE IF NOT EXISTS film_market_estimation (
Estimation_ID int,
Low_Estimate real,
High_Estimate real,
Film_ID int,
Type text,
Market_ID int,
Year int,
PRIMARY KEY (Estimation_ID),
FOREIGN KEY (Film_ID) REFERENCES film(Film_ID),
FOREIGN KEY (Market_ID) REFERENCES market(Market_ID)
);
















DROP TABLE IF EXISTS county_public_safety;
CREATE TABLE IF NOT EXISTS county_public_safety (
County_ID int,
Name text,
Population int,
Police_officers int,
Residents_per_officer int,
Case_burden int,
Crime_rate real,
Police_force text,
Location text,
PRIMARY KEY (County_ID)
);

DROP TABLE IF EXISTS city;
CREATE TABLE IF NOT EXISTS city (
City_ID int,
County_ID int,
Name text,
White real,
Black real,
Amerindian real,
Asian real,
Multiracial real,
Hispanic real,
PRIMARY KEY (City_ID),
FOREIGN KEY (County_ID) REFERENCES county_public_safety(County_ID)
);





























DROP TABLE IF EXISTS storm;
CREATE TABLE IF NOT EXISTS storm (
Storm_ID int,
Name text,
Dates_active text,
Max_speed int,
Damage_millions_USD real,
Number_Deaths int,
PRIMARY KEY (Storm_ID)
);

DROP TABLE IF EXISTS region;
CREATE TABLE IF NOT EXISTS region (
Region_id int,
Region_code text,
Region_name text,
PRIMARY KEY (Region_id)
);

CREATE TABLE affected_region (
Region_id int,
Storm_ID int,
Number_city_affected real,
PRIMARY KEY (Region_id,Storm_ID),
FOREIGN KEY (Region_id) REFERENCES region(Region_id),
FOREIGN KEY (Storm_ID) REFERENCES storm(Storm_ID)
);



DROP TABLE IF EXISTS body_builder;
CREATE TABLE IF NOT EXISTS body_builder (
Body_Builder_ID int,
People_ID int,
Snatch real,
Clean_Jerk real,
Total real,
PRIMARY KEY (Body_Builder_ID),
FOREIGN KEY (People_ID) REFERENCES people(People_ID)
);

DROP TABLE IF EXISTS people;
CREATE TABLE IF NOT EXISTS people (
People_ID int,
Name text,
Height real,
Weight real,
Birth_Date text,
Birth_Place text,
PRIMARY KEY (People_ID)
);

















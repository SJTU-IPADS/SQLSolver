DROP TABLE IF EXISTS candidate;
CREATE TABLE IF NOT EXISTS candidate (
Candidate_ID int,
People_ID int,
Poll_Source text,
Date text,
Support_rate real,
Consider_rate real,
Oppose_rate real,
Unsure_rate real,
PRIMARY KEY (Candidate_ID),
FOREIGN KEY (People_ID) REFERENCES people(People_ID)
);

DROP TABLE IF EXISTS people;
CREATE TABLE IF NOT EXISTS people (
People_ID int,
Sex text,
Name text,
Date_of_Birth text,
Height real,
Weight real,
PRIMARY KEY (People_ID)
);

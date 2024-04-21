


DROP TABLE IF EXISTS people;
CREATE TABLE IF NOT EXISTS people (
People_ID int,
District text,
Name text,
Party text,
Age int,
PRIMARY KEY (People_ID)
);

DROP TABLE IF EXISTS debate;
CREATE TABLE IF NOT EXISTS debate (
Debate_ID int,
Date text,
Venue text,
Num_of_Audience int,
PRIMARY KEY (Debate_ID)
);
























DROP TABLE IF EXISTS debate_people;
CREATE TABLE IF NOT EXISTS debate_people (
Debate_ID int,
Affirmative int,
Negative int,
If_Affirmative_Win bool,
PRIMARY KEY (Debate_ID,Affirmative,Negative),
FOREIGN KEY (Debate_ID) REFERENCES debate(Debate_ID),
FOREIGN KEY (Affirmative) REFERENCES people(People_ID),
FOREIGN KEY (Negative) REFERENCES people(People_ID)
);








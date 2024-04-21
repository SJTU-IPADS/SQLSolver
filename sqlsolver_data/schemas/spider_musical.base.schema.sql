

DROP TABLE IF EXISTS musical;
CREATE TABLE IF NOT EXISTS musical (
Musical_ID int,
Name text,
Year int,
Award text,
Category text,
Nominee text,
Result text,
PRIMARY KEY (Musical_ID)
);

DROP TABLE IF EXISTS actor;
CREATE TABLE IF NOT EXISTS actor (
Actor_ID int,
Name text,
Musical_ID int,
Character text,
Duration text,
age int,
PRIMARY KEY (Actor_ID),
FOREIGN KEY (Musical_ID) REFERENCES actor(Actor_ID)
);




















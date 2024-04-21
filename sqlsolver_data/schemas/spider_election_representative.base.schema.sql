

DROP TABLE IF EXISTS election;
CREATE TABLE IF NOT EXISTS election (
Election_ID int,
Representative_ID int,
Date text,
Votes real,
Vote_Percent real,
Seats real,
Place real,
PRIMARY KEY (Election_ID),
FOREIGN KEY (Representative_ID) REFERENCES representative(Representative_ID)
);

DROP TABLE IF EXISTS representative;
CREATE TABLE IF NOT EXISTS representative (
Representative_ID int,
Name text,
State text,
Party text,
Lifespan text,
PRIMARY KEY (Representative_ID)
);
















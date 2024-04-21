CREATE TABLE poker_player (
Poker_Player_ID INT,
People_ID INT,
Final_Table_Made REAL,
Best_Finish REAL,
Money_Rank REAL,
Earnings REAL,
PRIMARY KEY (Poker_Player_ID),
FOREIGN KEY (People_ID) REFERENCES people(People_ID)
);

CREATE TABLE people (
People_ID INT,
Nationality VARCHAR(255),
Name VARCHAR(255),
Birth_Date VARCHAR(255),
Height REAL,
PRIMARY KEY (People_ID)
);
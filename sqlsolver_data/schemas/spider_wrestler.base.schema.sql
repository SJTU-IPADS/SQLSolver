

DROP TABLE IF EXISTS wrestler;
CREATE TABLE IF NOT EXISTS wrestler (
Wrestler_ID int,
Name text,
Reign text,
Days_held text,
Location text,
Event text,
PRIMARY KEY (Wrestler_ID)
);

DROP TABLE IF EXISTS Elimination;
CREATE TABLE IF NOT EXISTS Elimination (
Elimination_ID text,
Wrestler_ID text,
Team text,
Eliminated_By text,
Elimination_Move text,
Time text,
PRIMARY KEY (Elimination_ID),
FOREIGN KEY (Wrestler_ID) REFERENCES wrestler(Wrestler_ID)
);






















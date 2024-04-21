
CREATE TABLE IF NOT EXISTS player (
Player_ID int,
Sponsor_name text,
Player_name text,
Gender text,
Residence text,
Occupation text,
Votes int,
Rank text,
PRIMARY KEY (Player_ID)
);














CREATE TABLE IF NOT EXISTS club (
Club_ID int,
Club_name text,
Region text,
Start_year int,
PRIMARY KEY (Club_ID)
);






CREATE TABLE IF NOT EXISTS coach (
Coach_ID int,
Coach_name text,
Gender text,
Club_ID int,
Rank int,
PRIMARY KEY (Coach_ID),
FOREIGN KEY (Club_ID) REFERENCES club(Club_ID)
);





CREATE TABLE IF NOT EXISTS player_coach (
Player_ID int,
Coach_ID int,
Starting_year int,
PRIMARY KEY (Player_ID,Coach_ID),
FOREIGN KEY (Player_ID) REFERENCES player(Player_ID),
FOREIGN KEY (Coach_ID) REFERENCES coach(Coach_ID)
);







CREATE TABLE IF NOT EXISTS match_result (
Rank int,
Club_ID int,
Gold int,
Big_Silver int,
Small_Silver int,
Bronze int,
Points int,
PRIMARY KEY (Rank,Club_ID),
FOREIGN KEY (Club_ID) REFERENCES club(Club_ID)
);








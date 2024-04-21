


DROP TABLE IF EXISTS party;
CREATE TABLE IF NOT EXISTS party (
Party_ID int,
Party_Theme text,
Location text,
First_year text,
Last_year text,
Number_of_hosts int,
PRIMARY KEY (Party_ID)
);

DROP TABLE IF EXISTS host;
CREATE TABLE IF NOT EXISTS host (
Host_ID int,
Name text,
Nationality text,
Age text,
PRIMARY KEY (Host_ID)
);
























DROP TABLE IF EXISTS party_host;
CREATE TABLE IF NOT EXISTS party_host (
Party_ID int,
Host_ID int,
Is_Main_in_Charge bool,
PRIMARY KEY (Party_ID,Host_ID),
FOREIGN KEY (Host_ID) REFERENCES host(Host_ID),
FOREIGN KEY (Party_ID) REFERENCES party(Party_ID)
);









/* 
 * SQL scripts for CS61 Intro to SQL lectures
 * FILENAME SOCCER2.SQL
 */

DROP TABLE  IF EXISTS Player;
DROP TABLE  IF EXISTS Tryout;
DROP TABLE  IF EXISTS College;

CREATE TABLE 	College 
  ( cName   	varchar(20) NOT NULL,
    state   	varchar(2),
    enr     	numeric(5,0),
    PRIMARY KEY (cName)
  );

CREATE TABLE 	Player
  ( pID			numeric(5,0) NOT NULL,
  	pName   	varchar(20),
    yCard   	varchar(3),
    HS      	numeric(5,0),
    PRIMARY KEY (pID)
  );

CREATE TABLE 	Tryout
  ( pID			numeric(5,0),
  	cName   	varchar(20),
    pPos    	varchar(8),
    decision    varchar(3),
    PRIMARY KEY (pID, cName),
    FOREIGN KEY (pID) REFERENCES Player(pID),
    FOREIGN KEY (cName) REFERENCES College(cName)
  );

/* note that left and right are reserved words in SQL */




















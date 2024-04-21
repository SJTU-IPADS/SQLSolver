-- LINK: https://en.wikibooks.org/wiki/SQL_Exercises/The_computer_store

CREATE TABLE Manufacturers (
  Code INTEGER,
  Name VARCHAR(255) NOT NULL,
  Headquarter VARCHAR(255) NOT NULL,
  Founder VARCHAR(255) NOT NULL,
  Revenue REAL,
  PRIMARY KEY (Code)   
);

CREATE TABLE Products (
  Code INTEGER,
  Name VARCHAR(255) NOT NULL ,
  Price DECIMAL NOT NULL ,
  Manufacturer INTEGER NOT NULL,
  PRIMARY KEY (Code), 
  FOREIGN KEY (Manufacturer) REFERENCES Manufacturers(Code)
);




















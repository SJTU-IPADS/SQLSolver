CREATE TABLE Person (
  name varchar(20) PRIMARY KEY,
  age INTEGER,
  city TEXT,
  gender TEXT,
  job TEXT
);

CREATE TABLE PersonFriend (
  name varchar(20),
  friend varchar(20),
  year INTEGER,
  FOREIGN KEY (name) REFERENCES Person(name),
  FOREIGN KEY (friend) REFERENCES Person(name)
);




 






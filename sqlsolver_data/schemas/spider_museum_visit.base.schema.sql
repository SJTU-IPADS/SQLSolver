CREATE TABLE museum (
Museum_ID INT,
Name VARCHAR(255),
Num_of_Staff INT,
Open_Year VARCHAR(255),
PRIMARY KEY (Museum_ID)
);

CREATE TABLE visitor (
ID INT,
Name VARCHAR(255),
Level_of_membership INT,
Age INT,
PRIMARY KEY (ID)
);

CREATE TABLE visit (
Museum_ID INT,
visitor_ID INT,
Num_of_Ticket INT,
Total_spent REAL,
PRIMARY KEY (Museum_ID,visitor_ID),
FOREIGN KEY (Museum_ID) REFERENCES museum(Museum_ID),
FOREIGN KEY (visitor_ID) REFERENCES visitor(ID)
);
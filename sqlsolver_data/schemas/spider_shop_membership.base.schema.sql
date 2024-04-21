

DROP TABLE IF EXISTS member;
CREATE TABLE IF NOT EXISTS member (
Member_ID int,
Card_Number text,
Name text,
Hometown text,
Level int,
PRIMARY KEY (Member_ID)
);














DROP TABLE IF EXISTS branch;
CREATE TABLE IF NOT EXISTS branch (
Branch_ID int,
Name text,
Open_year text,
Address_road text,
City text,
membership_amount text,
PRIMARY KEY (Branch_ID)
);












DROP TABLE IF EXISTS membership_register_branch;
CREATE TABLE IF NOT EXISTS membership_register_branch (
Member_ID int,
Branch_ID text,
Register_Year text,
PRIMARY KEY (Member_ID),
FOREIGN KEY (Member_ID) REFERENCES member(Member_ID),
FOREIGN KEY (Branch_ID) REFERENCES branch(Branch_ID)
);












DROP TABLE IF EXISTS purchase;
CREATE TABLE IF NOT EXISTS purchase (
Member_ID int,
Branch_ID text,
Year text,
Total_pounds real,
PRIMARY KEY (Member_ID,Branch_ID,Year),
FOREIGN KEY (Member_ID) REFERENCES member(Member_ID),
FOREIGN KEY (Branch_ID) REFERENCES branch(Branch_ID)
);











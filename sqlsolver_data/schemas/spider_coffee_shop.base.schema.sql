

DROP TABLE IF EXISTS shop;
CREATE TABLE IF NOT EXISTS shop (
Shop_ID int,
Address text,
Num_of_staff text,
Score real,
Open_Year text,
PRIMARY KEY (Shop_ID)
);















DROP TABLE IF EXISTS member;
CREATE TABLE IF NOT EXISTS member (
Member_ID int,
Name text,
Membership_card text,
Age int,
Time_of_purchase int,
Level_of_membership int,
Address text,
PRIMARY KEY (Member_ID)
);













DROP TABLE IF EXISTS happy_hour;
CREATE TABLE IF NOT EXISTS happy_hour (
HH_ID int,
Shop_ID int,
Month text,
Num_of_shaff_in_charge int,
PRIMARY KEY (HH_ID,Shop_ID,Month),
FOREIGN KEY (Shop_ID) REFERENCES shop(Shop_ID)
);













DROP TABLE IF EXISTS happy_hour_member;
CREATE TABLE IF NOT EXISTS happy_hour_member (
HH_ID int,
Member_ID int,
Total_amount real,
PRIMARY KEY (HH_ID,Member_ID),
FOREIGN KEY (Member_ID) REFERENCES member(Member_ID)
);










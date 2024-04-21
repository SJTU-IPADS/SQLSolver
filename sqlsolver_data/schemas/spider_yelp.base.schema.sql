
DROP TABLE IF EXISTS business;
CREATE TABLE IF NOT EXISTS business (
bid int,
business_id text,
name text,
full_address text,
city text,
latitude text,
longitude text,
review_count int,
is_open int,
rating real,
state text,
primary key(bid)
);
DROP TABLE IF EXISTS category;
CREATE TABLE IF NOT EXISTS category (
id int,
business_id text,
category_name text,
primary key(id),
foreign key(business_id) references business(business_id)
);
DROP TABLE IF EXISTS user;
CREATE TABLE IF NOT EXISTS user (
uid int,
user_id text,
name text,
primary key(uid)
);
DROP TABLE IF EXISTS checkin;
CREATE TABLE IF NOT EXISTS checkin (
cid int,
business_id text,
count int,
day text,
primary key(cid),
foreign key(business_id) references business(business_id)
);

DROP TABLE IF EXISTS neighbourhood;
CREATE TABLE IF NOT EXISTS neighbourhood (
id int,
business_id text,
neighbourhood_name text,
primary key(id),
foreign key(business_id) references business(business_id)
);

DROP TABLE IF EXISTS review;
CREATE TABLE IF NOT EXISTS review (
rid int,
business_id text,
user_id text,
rating real,
text text,
year int,
month text,
primary key(rid),
foreign key(business_id) references business(business_id),
foreign key(user_id) references user(user_id)
);
DROP TABLE IF EXISTS tip;
CREATE TABLE IF NOT EXISTS tip (
tip_id int,
business_id text,
text text,
user_id text,
likes int,
year int,
month text,
primary key(tip_id)
foreign key(business_id) references business(business_id),
foreign key(user_id) references user(user_id)

);



DROP TABLE IF EXISTS airport;
CREATE TABLE IF NOT EXISTS airport (
id int,
City text,
Country text,
IATA text,
ICAO text,
name text,
primary key(id)
);


DROP TABLE IF EXISTS operate_company;
CREATE TABLE IF NOT EXISTS operate_company (
id int,
name text,
Type text,
Principal_activities text,
Incorporated_in text,
Group_Equity_Shareholding real,
primary key (id)
);

DROP TABLE IF EXISTS flight;
CREATE TABLE IF NOT EXISTS flight (
id int,
Vehicle_Flight_number text,
Date text,
Pilot text,
Velocity real,
Altitude real,
airport_id int,
company_id int,
primary key (id),
foreign key (airport_id) references airport(id),
foreign key (company_id) references operate_company(id)
);















































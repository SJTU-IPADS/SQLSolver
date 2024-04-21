



DROP TABLE IF EXISTS company;
CREATE TABLE IF NOT EXISTS company (
Company_ID int,
Rank int,
Company text,
Headquarters text,
Main_Industry text,
Sales_billion real,
Profits_billion real,
Assets_billion real,
Market_Value real,
PRIMARY KEY (Company_ID)
);

DROP TABLE IF EXISTS gas_station;
CREATE TABLE IF NOT EXISTS gas_station (
Station_ID int,
Open_Year int,
Location text,
Manager_Name text,
Vice_Manager_Name text,
Representative_Name text,
PRIMARY KEY (Station_ID)
);

























DROP TABLE IF EXISTS station_company;
CREATE TABLE IF NOT EXISTS station_company (
Station_ID int,
Company_ID int,
Rank_of_the_Year int,
PRIMARY KEY (Station_ID,Company_ID),
FOREIGN KEY (Station_ID) REFERENCES gas_station(Station_ID),
FOREIGN KEY (Company_ID) REFERENCES company(Company_ID)
);









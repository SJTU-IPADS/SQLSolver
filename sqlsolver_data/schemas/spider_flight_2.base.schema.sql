CREATE TABLE airlines (
uid INT(11) PRIMARY KEY,
Airline VARCHAR(255),
Abbreviation VARCHAR(255),
Country VARCHAR(255)
);
CREATE TABLE airports (
City VARCHAR(255),
AirportCode VARCHAR(255) PRIMARY KEY,
AirportName VARCHAR(255),
Country VARCHAR(255),
CountryAbbrev VARCHAR(255)
);
CREATE TABLE flights (
Airline INT(11),
FlightNo INT(11),
SourceAirport VARCHAR(255),
DestAirport VARCHAR(255),
PRIMARY KEY(Airline, FlightNo),
FOREIGN KEY (SourceAirport) REFERENCES airports(AirportCode),
FOREIGN KEY (DestAirport) REFERENCES airports(AirportCode)
);

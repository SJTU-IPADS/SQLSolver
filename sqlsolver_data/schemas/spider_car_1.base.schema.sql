CREATE TABLE continents (
ContId INT PRIMARY KEY,
Continent VARCHAR(255)
);

CREATE TABLE countries (
CountryId INT PRIMARY KEY,
CountryName VARCHAR(255),
Continent INT,
FOREIGN KEY (Continent) REFERENCES continents(ContId)
);

CREATE TABLE car_makers (
Id INT PRIMARY KEY,
Maker VARCHAR(255),
FullName VARCHAR(255),
Country INT,
FOREIGN KEY (Country) REFERENCES countries(CountryId)
);

CREATE TABLE model_list (
ModelId INT PRIMARY KEY,
Maker INT,
Model VARCHAR(255) UNIQUE,
FOREIGN KEY (Maker) REFERENCES car_makers (Id)

);

CREATE TABLE car_names (
MakeId INT PRIMARY KEY,
Model VARCHAR(255),
Make VARCHAR(255),
FOREIGN KEY (Model) REFERENCES model_list (Model)
);

CREATE TABLE cars_data (
Id INT PRIMARY KEY,
MPG VARCHAR(255),
Cylinders INT,
Edispl REAL,
Horsepower VARCHAR(255),
Weight INT,
Accelerate REAL,
Year INT,
FOREIGN KEY (Id) REFERENCES car_names (MakeId)
);

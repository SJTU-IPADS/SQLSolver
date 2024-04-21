


DROP TABLE IF EXISTS manufacturer;
CREATE TABLE IF NOT EXISTS manufacturer (
    Manufacturer_ID int,
    Open_Year real,
    Name text,
    Num_of_Factories int,
    Num_of_Shops int,
    PRIMARY KEY (Manufacturer_ID)
);












DROP TABLE IF EXISTS furniture;
CREATE TABLE IF NOT EXISTS furniture (
    Furniture_ID int,
    Name text,
    Num_of_Component int,
    Market_Rate real,
    PRIMARY KEY (Furniture_ID)
);










DROP TABLE IF EXISTS furniture_manufacte;
CREATE TABLE IF NOT EXISTS furniture_manufacte (
    Manufacturer_ID int,
    Furniture_ID int,
    Price_in_Dollar real,
    PRIMARY KEY (Manufacturer_ID,Furniture_ID),
    FOREIGN KEY (Manufacturer_ID) REFERENCES manufacturer(Manufacturer_ID),
    FOREIGN KEY (Furniture_ID) REFERENCES furniture(Furniture_ID)
);








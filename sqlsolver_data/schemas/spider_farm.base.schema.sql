CREATE TABLE city
(
    City_ID        int,
    Official_Name  text,
    Status         text,
    Area_km_2      float,
    Population     float,
    Census_Ranking text,
    PRIMARY KEY (City_ID)
);

CREATE TABLE farm
(
    Farm_ID         int,
    Year            int,
    Total_Horses    float,
    Working_Horses  float,
    Total_Cattle    float,
    Oxen            float,
    Bulls           float,
    Cows            float,
    Pigs            float,
    Sheep_and_Goats float,
    PRIMARY KEY (Farm_ID)
);

CREATE TABLE farm_competition
(
    Competition_ID int,
    Year           int,
    Theme          text,
    Host_city_ID   int,
    Hosts          text,
    PRIMARY KEY (Competition_ID),
    FOREIGN KEY (Host_city_ID) REFERENCES city (City_ID)
);


CREATE TABLE competition_record
(
    Competition_ID int,
    Farm_ID        int,
    Rank           int,
    PRIMARY KEY (Competition_ID, Farm_ID),
    FOREIGN KEY (Competition_ID) REFERENCES farm_competition (Competition_ID),
    FOREIGN KEY (Farm_ID) REFERENCES farm (Farm_ID)
);
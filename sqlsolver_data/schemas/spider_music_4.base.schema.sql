

DROP TABLE IF EXISTS artist;
CREATE TABLE IF NOT EXISTS artist (
    Artist_ID int,
    Artist text,
    Age int,
    Famous_Title text,
    Famous_Release_date text,
    PRIMARY KEY (Artist_ID)
);



DROP TABLE IF EXISTS volume;
CREATE TABLE IF NOT EXISTS volume (
    Volume_ID int,
    Volume_Issue text,
    Issue_Date text,
    Weeks_on_Top real,
    Song text,
    Artist_ID int,
    PRIMARY KEY (Volume_ID),
    FOREIGN KEY (Artist_ID) REFERENCES artist(Artist_ID)
);


























DROP TABLE IF EXISTS music_festival;
CREATE TABLE IF NOT EXISTS music_festival (
    ID int,
    Music_Festival text,
    Date_of_ceremony text,
    Category text,
    Volume int,
    Result text,
    PRIMARY KEY (ID),
    FOREIGN KEY (Volume) REFERENCES volume(Volume_ID)
);











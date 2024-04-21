

DROP TABLE IF EXISTS publication;
CREATE TABLE IF NOT EXISTS publication (
Publication_ID int,
Book_ID int,
Publisher text,
Publication_Date text,
Price real,
PRIMARY KEY (Publication_ID),
FOREIGN KEY (Book_ID) REFERENCES book(Book_ID)
);

DROP TABLE IF EXISTS book;
CREATE TABLE IF NOT EXISTS book (
Book_ID int,
Title text,
Issues real,
Writer text,
PRIMARY KEY (Book_ID)
);






















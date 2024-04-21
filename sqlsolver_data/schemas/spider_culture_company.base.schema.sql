

DROP TABLE IF EXISTS book_club;
CREATE TABLE IF NOT EXISTS book_club (
book_club_id int,
Year int,
Author_or_Editor text,
Book_Title text,
Publisher text,
Category text,
Result text,
PRIMARY KEY (book_club_id)
);















DROP TABLE IF EXISTS movie;
CREATE TABLE IF NOT EXISTS movie (
movie_id int,
Title text,
Year int,
Director text,
Budget_million real,
Gross_worldwide int,
PRIMARY KEY(movie_id)
);














DROP TABLE IF EXISTS culture_company;
CREATE TABLE IF NOT EXISTS culture_company (
Company_name text,
Type text,
Incorporated_in text,
Group_Equity_Shareholding real,
book_club_id text,
movie_id text,
PRIMARY KEY(Company_name),
FOREIGN KEY (book_club_id) REFERENCES book_club(book_club_id),
FOREIGN KEY (movie_id) REFERENCES movie(movie_id)
);









CREATE TABLE IF NOT EXISTS journal (
Journal_ID int,
Date text,
Theme text,
Sales int,
PRIMARY KEY (Journal_ID)
);
















CREATE TABLE IF NOT EXISTS editor (
Editor_ID int,
Name text,
Age real,
PRIMARY KEY (Editor_ID)
);









CREATE TABLE IF NOT EXISTS journal_committee (
Editor_ID int,
Journal_ID int,
Work_Type text,
PRIMARY KEY (Editor_ID,Journal_ID),
FOREIGN KEY (Editor_ID) REFERENCES editor(Editor_ID),
FOREIGN KEY (Journal_ID) REFERENCES journal(Journal_ID)
);









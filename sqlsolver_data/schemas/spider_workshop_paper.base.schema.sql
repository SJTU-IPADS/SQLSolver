


DROP TABLE IF EXISTS workshop;
CREATE TABLE IF NOT EXISTS workshop (
Workshop_ID int,
Date text,
Venue text,
Name text,
PRIMARY KEY (Workshop_ID)
);

DROP TABLE IF EXISTS submission;
CREATE TABLE IF NOT EXISTS submission (
Submission_ID int,
Scores real,
Author text,
College text,
PRIMARY KEY (Submission_ID)
);





















DROP TABLE IF EXISTS Acceptance;
CREATE TABLE IF NOT EXISTS Acceptance (
Submission_ID int,
Workshop_ID int,
Result text,
PRIMARY KEY (Submission_ID,Workshop_ID),
FOREIGN KEY (Submission_ID) REFERENCES submission(Submission_ID),
FOREIGN KEY (Workshop_ID) REFERENCES workshop(Workshop_ID)
);









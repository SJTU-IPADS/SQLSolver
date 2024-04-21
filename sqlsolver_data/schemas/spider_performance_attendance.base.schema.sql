


DROP TABLE IF EXISTS member;
CREATE TABLE IF NOT EXISTS member (
    Member_ID text,
    Name text,
    Nationality text,
    Role text,
    PRIMARY KEY (Member_ID)
);















DROP TABLE IF EXISTS performance;
CREATE TABLE IF NOT EXISTS performance (
    Performance_ID real,
    Date text,
    Host text,
    Location text,
    Attendance int,
    PRIMARY KEY (Performance_ID)
);









DROP TABLE IF EXISTS member_attendance;
CREATE TABLE IF NOT EXISTS member_attendance (
    Member_ID int,
    Performance_ID int,
    Num_of_Pieces int,
    PRIMARY KEY (Member_ID,Performance_ID),
    FOREIGN KEY (Member_ID) REFERENCES member(Member_ID),
    FOREIGN KEY (Performance_ID) REFERENCES performance(Performance_ID)
);











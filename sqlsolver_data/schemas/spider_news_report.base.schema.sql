


DROP TABLE IF EXISTS event;
CREATE TABLE IF NOT EXISTS event (
    Event_ID int,
    Date text,
    Venue text,
    Name text,
    Event_Attendance int,
    PRIMARY KEY (Event_ID)
);

DROP TABLE IF EXISTS journalist;
CREATE TABLE IF NOT EXISTS journalist (
    journalist_ID int,
    Name text,
    Nationality text,
    Age text,
    Years_working int,
    PRIMARY KEY (journalist_ID)
);






















DROP TABLE IF EXISTS news_report;
CREATE TABLE IF NOT EXISTS news_report (
    journalist_ID int,
    Event_ID int,
    Work_Type text,
    PRIMARY KEY (journalist_ID,Event_ID),
    FOREIGN KEY (journalist_ID) REFERENCES journalist(journalist_ID),
    FOREIGN KEY (Event_ID) REFERENCES event(Event_ID)
);










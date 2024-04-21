CREATE TABLE Inst
(
    instID  INTEGER,
    name    TEXT,
    country TEXT,
    PRIMARY KEY (instID)
);

CREATE TABLE Authors
(
    authID INTEGER,
    lname  TEXT,
    fname  TEXT,
    PRIMARY KEY (authID)
);

CREATE TABLE Papers
(
    paperID INTEGER,
    title   TEXT,
    PRIMARY KEY (paperID)
);

CREATE TABLE Authorship
(
    authID    INTEGER,
    instID    INTEGER,
    paperID   INTEGER,
    authOrder INTEGER,
    PRIMARY KEY (authID, instID, paperID),
    FOREIGN KEY (authID) REFERENCES Authors (authID),
    FOREIGN KEY (instID) REFERENCES Inst (instID),
    FOREIGN KEY (paperID) REFERENCES Papers (paperID)
);

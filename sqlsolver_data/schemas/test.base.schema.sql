CREATE TABLE a
(
    i INT PRIMARY KEY,
    j INT,
    k INT
);

CREATE TABLE b
(
    x INT PRIMARY KEY,
    y INT,
    z INT
);

CREATE TABLE c
(
    u INT PRIMARY KEY,
    v CHAR(10),
    w DECIMAL(1, 10)
);

CREATE TABLE d
(
    p INT,
    q CHAR(10),
    r DECIMAL(1, 10),
    UNIQUE KEY (p),
    FOREIGN KEY (p) REFERENCES c (u)
);

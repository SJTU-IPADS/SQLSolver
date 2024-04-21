CREATE TABLE routes
(
    rid       INTEGER PRIMARY KEY,
    dst_apid  INTEGER,
    dst_ap    VARCHAR(4),
    src_apid  BIGINT,
    src_ap    VARCHAR(4),
    alid      BIGINT,
    airline   VARCHAR(4),
    codeshare TEXT,
    FOREIGN KEY (dst_apid) REFERENCES airports (apid),
    FOREIGN KEY (src_apid) REFERENCES airports (apid),
    FOREIGN KEY (alid) REFERENCES airlines (alid)
);

CREATE TABLE airports
(
    apid      INTEGER PRIMARY KEY,
    name      TEXT NOT NULL,
    city      TEXT,
    country   TEXT,
    x         REAL,
    y         REAL,
    elevation BIGINT,
    iata      VARCHAR(3),
    icao      VARCHAR(4)
);

CREATE TABLE airlines
(
    alid     INTEGER PRIMARY KEY,
    name     TEXT,
    iata     VARCHAR(2),
    icao     VARCHAR(3),
    callsign TEXT,
    country  TEXT,
    active   VARCHAR(2)
);

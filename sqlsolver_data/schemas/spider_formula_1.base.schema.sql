CREATE TABLE IF NOT EXISTS circuits
(
    circuitId  INTEGER PRIMARY KEY,
    circuitRef TEXT,
    name       TEXT,
    location   TEXT,
    country    TEXT,
    lat        REAL,
    lng        REAL,
    alt        TEXT,
    url        TEXT
);

CREATE TABLE IF NOT EXISTS races
(
    raceId    INTEGER PRIMARY KEY,
    year      INTEGER,
    round     INTEGER,
    circuitId INTEGER,
    name      TEXT,
    date      TEXT,
    time      TEXT,
    url       TEXT,
    FOREIGN KEY (circuitId) REFERENCES circuits (circuitId)
);

CREATE TABLE IF NOT EXISTS drivers
(
    driverId    INTEGER PRIMARY KEY,
    driverRef   TEXT,
    number      TEXT,
    code        TEXT,
    forename    TEXT,
    surname     TEXT,
    dob         TEXT,
    nationality TEXT,
    url         TEXT
);

CREATE TABLE IF NOT EXISTS status
(
    statusId INTEGER PRIMARY KEY,
    status   TEXT
);

CREATE TABLE IF NOT EXISTS seasons
(
    year INTEGER PRIMARY KEY,
    url  TEXT
);

CREATE TABLE IF NOT EXISTS constructors
(
    constructorId  INTEGER PRIMARY KEY,
    constructorRef TEXT,
    name           TEXT,
    nationality    TEXT,
    url            TEXT
);

CREATE TABLE IF NOT EXISTS constructorStandings
(
    constructorStandingsId INTEGER PRIMARY KEY,
    raceId                 INTEGER,
    constructorId          INTEGER,
    points                 REAL,
    position               INTEGER,
    positionText           TEXT,
    wins                   INTEGER,
    FOREIGN KEY (constructorId) REFERENCES constructors (constructorId),
    FOREIGN KEY (raceId) REFERENCES races (raceId)
);

CREATE TABLE IF NOT EXISTS results
(
    resultId        INTEGER PRIMARY KEY,
    raceId          INTEGER,
    driverId        INTEGER,
    constructorId   INTEGER,
    number          INTEGER,
    grid            INTEGER,
    position        TEXT,
    positionText    TEXT,
    positionOrder   INTEGER,
    points          REAL,
    laps            TEXT,
    time            TEXT,
    milliseconds    TEXT,
    fastestLap      TEXT,
    rank            TEXT,
    fastestLapTime  TEXT,
    fastestLapSpeed TEXT,
    statusId        INTEGER,
    FOREIGN KEY (constructorId) REFERENCES constructors (constructorId),
    FOREIGN KEY (raceId) REFERENCES races (raceId),
    FOREIGN KEY (driverId) REFERENCES drivers (driverId)
);

CREATE TABLE IF NOT EXISTS driverStandings
(
    driverStandingsId INTEGER PRIMARY KEY,
    raceId            INTEGER,
    driverId          INTEGER,
    points            REAL,
    position          INTEGER,
    positionText      TEXT,
    wins              INTEGER,
    FOREIGN KEY (raceId) REFERENCES races (raceId),
    FOREIGN KEY (driverId) REFERENCES drivers (driverId)
);

CREATE TABLE IF NOT EXISTS constructorResults
(
    constructorResultsId INTEGER PRIMARY KEY,
    raceId               INTEGER,
    constructorId        INTEGER,
    points               REAL,
    status               TEXT,
    FOREIGN KEY (constructorId) REFERENCES constructors (constructorId),
    FOREIGN KEY (raceId) REFERENCES races (raceId)
);

CREATE TABLE IF NOT EXISTS qualifying
(
    qualifyId     INTEGER PRIMARY KEY,
    raceId        INTEGER,
    driverId      INTEGER,
    constructorId INTEGER,
    number        INTEGER,
    position      INTEGER,
    q1            TEXT,
    q2            TEXT,
    q3            TEXT,
    FOREIGN KEY (constructorId) REFERENCES constructors (constructorId),
    FOREIGN KEY (raceId) REFERENCES races (raceId),
    FOREIGN KEY (driverId) REFERENCES drivers (driverId)
);

CREATE TABLE IF NOT EXISTS pitStops
(
    raceId       INTEGER,
    driverId     INTEGER,
    stop         INTEGER,
    lap          INTEGER,
    time         TEXT,
    duration     TEXT,
    milliseconds INTEGER,
    PRIMARY KEY (raceId, driverId, stop),
    FOREIGN KEY (raceId) REFERENCES races (raceId),
    FOREIGN KEY (driverId) REFERENCES drivers (driverId)
);

CREATE TABLE IF NOT EXISTS lapTimes
(
    raceId       INTEGER,
    driverId     INTEGER,
    lap          INTEGER,
    position     INTEGER,
    time         TEXT,
    milliseconds INTEGER,
    PRIMARY KEY (raceId, driverId, lap),
    FOREIGN KEY (raceId) REFERENCES races (raceId),
    FOREIGN KEY (driverId) REFERENCES drivers (driverId)
);

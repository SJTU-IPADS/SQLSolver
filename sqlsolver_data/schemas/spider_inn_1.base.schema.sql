CREATE TABLE IF NOT EXISTS Rooms
(
    RoomId       TEXT PRIMARY KEY,
    roomName     TEXT,
    beds         INTEGER,
    bedType      TEXT,
    maxOccupancy INTEGER,
    basePrice    INTEGER,
    decor        TEXT
);

CREATE TABLE IF NOT EXISTS Reservations
(
    Code      INTEGER PRIMARY KEY,
    Room      TEXT,
    CheckIn   TEXT,
    CheckOut  TEXT,
    Rate      REAL,
    LastName  TEXT,
    FirstName TEXT,
    Adults    INTEGER,
    Kids      INTEGER,
    FOREIGN KEY (Room) REFERENCES Rooms (RoomId)
);

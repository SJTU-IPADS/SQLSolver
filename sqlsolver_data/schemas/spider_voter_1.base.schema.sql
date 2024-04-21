CREATE TABLE AREA_CODE_STATE (
area_code INTEGER NOT NULL,
state VARCHAR(2) NOT NULL,
PRIMARY KEY (area_code)
);

CREATE TABLE CONTESTANTS (
contestant_number INTEGER,
contestant_name VARCHAR(50) NOT NULL,
PRIMARY KEY (contestant_number)
);

CREATE TABLE VOTES (
vote_id INTEGER NOT NULL PRIMARY KEY,
phone_number INTEGER NOT NULL,
state VARCHAR(2) NOT NULL,
contestant_number INTEGER NOT NULL,
created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
FOREIGN KEY (state) REFERENCES AREA_CODE_STATE(state),
FOREIGN KEY (contestant_number) REFERENCES CONTESTANTS(contestant_number)
);

CREATE INDEX idx_VOTES_idx_votes_phone_number ON VOTES (phone_number);
CREATE TABLE IF NOT EXISTS TV_Channel (
id TEXT PRIMARY KEY,
series_name TEXT,
Country TEXT,
Language TEXT,
Content TEXT,
Pixel_aspect_ratio_PAR TEXT,
Hight_definition_TV TEXT,
Pay_per_view_PPV TEXT,
Package_Option TEXT
);

CREATE TABLE IF NOT EXISTS TV_series (
id REAL,
Episode TEXT,
Air_Date TEXT,
Rating TEXT,
Share REAL,
18_49_Rating_Share TEXT,
Viewers_m TEXT,
Weekly_Rank REAL,
Channel TEXT,
PRIMARY KEY (id),
FOREIGN KEY (Channel) REFERENCES TV_Channel(id)
);

CREATE TABLE IF NOT EXISTS Cartoon (
id REAL,
Title TEXT,
Directed_by TEXT,
Written_by TEXT,
Original_air_date TEXT,
Production_code REAL,
Channel TEXT,
PRIMARY KEY (id),
FOREIGN KEY (Channel) REFERENCES TV_Channel(id)
);

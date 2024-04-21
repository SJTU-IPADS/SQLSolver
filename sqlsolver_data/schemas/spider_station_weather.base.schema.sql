

DROP TABLE IF EXISTS train;
CREATE TABLE IF NOT EXISTS train (
    id int,
    train_number int,
    name text,
    origin text,
    destination text,
    time text,
    interval text,
    primary key (id)
);

DROP TABLE IF EXISTS station;
CREATE TABLE IF NOT EXISTS station (
    id int,
    network_name text,
    services text,
    local_authority text,
    primary key (id)
);

DROP TABLE IF EXISTS route;
CREATE TABLE IF NOT EXISTS route (
    train_id int,
    station_id int,
    primary key (train_id, station_id),
    foreign key (train_id) references train(id),
    foreign key (station_id) references station(id)
);

DROP TABLE IF EXISTS weekly_weather;
CREATE TABLE IF NOT EXISTS weekly_weather (
    station_id int,
    day_of_week text,
    high_temperature int,
    low_temperature int,
    precipitation real,
    wind_speed_mph int,
    primary key (station_id, day_of_week),
    foreign key (station_id) references station(id)
);






























































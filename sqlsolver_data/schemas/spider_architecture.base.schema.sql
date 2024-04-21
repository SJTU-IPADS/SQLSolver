DROP TABLE IF EXISTS architect;
CREATE TABLE IF NOT EXISTS architect (
id int,
name text,
nationality text,
gender text,
primary key(id)
);

DROP TABLE IF EXISTS bridge;
CREATE TABLE IF NOT EXISTS bridge (
architect_id int,
id int,
name text,
location text,
length_meters real,
length_feet real,
primary key(id),
foreign key (architect_id ) references architect(id)
);

DROP TABLE IF EXISTS mill;
CREATE TABLE IF NOT EXISTS mill (
architect_id int,
id int,
location text,
name text,
type text,
built_year int,
notes text,
primary key (id),
foreign key (architect_id ) references architect(id)
);
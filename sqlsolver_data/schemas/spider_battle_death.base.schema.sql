CREATE TABLE battle (
id int,
name text,
date text,
bulgarian_commander text,
latin_commander text,
result text,
primary key(id)
);

CREATE TABLE ship (
lost_in_battle int,
id int,
name text,
tonnage text,
ship_type text,
location text,
disposition_of_ship text,
primary key(id),
foreign key (`lost_in_battle`) references `battle`(id) 
);


CREATE TABLE death (
caused_by_ship_id int,
id int,
note text,
killed int,
injured int,
primary key(id),
foreign key (caused_by_ship_id) references `ship`(id) 
);
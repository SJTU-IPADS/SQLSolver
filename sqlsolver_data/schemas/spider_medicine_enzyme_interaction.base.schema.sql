
DROP TABLE IF EXISTS medicine;
CREATE TABLE IF NOT EXISTS medicine (
id int,
name text,
Trade_Name text,
FDA_approved text,
primary key (id)
);

DROP TABLE IF EXISTS enzyme;
CREATE TABLE IF NOT EXISTS enzyme (
id int,
name text,
Location text,
Product text,
Chromosome text,
OMIM int,
Porphyria text,
primary key (id)
);


DROP TABLE IF EXISTS medicine_enzyme_interaction;
CREATE TABLE IF NOT EXISTS medicine_enzyme_interaction (
enzyme_id int,
medicine_id int,
interaction_type text,
primary key (enzyme_id, medicine_id),
foreign key (enzyme_id) references enzyme(id),
foreign key (medicine_id) references medicine(id)
);





























































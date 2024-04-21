

DROP TABLE IF EXISTS building;
CREATE TABLE IF NOT EXISTS building (
building_id text,
Name text,
Street_address text,
Years_as_tallest text,
Height_feet int,
Floors int,
PRIMARY KEY(building_id)
);

DROP TABLE IF EXISTS Institution;
CREATE TABLE IF NOT EXISTS Institution (
Institution_id  text,
Institution text,
Location text,
Founded real,
Type text,
Enrollment int,
Team text,
Primary_Conference text,
building_id text,
PRIMARY KEY(Institution_id),
FOREIGN  KEY (building_id) REFERENCES building(building_id)
);

DROP TABLE IF EXISTS protein;
CREATE TABLE IF NOT EXISTS protein (
common_name text,
protein_name text,
divergence_from_human_lineage real,
accession_number text,
sequence_length real,
sequence_identity_to_human_protein text,
Institution_id text,
PRIMARY KEY(common_name),
FOREIGN KEY(Institution_id) REFERENCES Institution(Institution_id)
);































CREATE TABLE IF NOT EXISTS chip_model (
Model_name text,
Launch_year real,
RAM_MiB real,
ROM_MiB real,
Slots text,
WiFi text,
Bluetooth text,
PRIMARY KEY (Model_name)
);














CREATE TABLE IF NOT EXISTS screen_mode (
Graphics_mode real,
Char_cells text,
Pixels text,
Hardware_colours real,
used_kb real,
map text,
Type text,
PRIMARY KEY (Graphics_mode)
);








CREATE TABLE IF NOT EXISTS phone (
Company_name text,
Hardware_Model_name text,
Accreditation_type text,
Accreditation_level text,
Date text,
chip_model text,
screen_mode text,
PRIMARY KEY(Hardware_Model_name),
FOREIGN KEY (screen_mode) REFERENCES screen_mode(Graphics_mode),
FOREIGN KEY (chip_model) REFERENCES chip_model(Model_name)
);









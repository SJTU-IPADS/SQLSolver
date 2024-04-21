
CREATE TABLE Addresses (
address_id INTEGER PRIMARY KEY,
line_1_number_building VARCHAR(80),
town_city VARCHAR(50),
zip_postcode VARCHAR(20),
state_province_county VARCHAR(50),
country VARCHAR(50)
);

















CREATE TABLE Services (
service_id INTEGER PRIMARY KEY,
service_type_code VARCHAR(15) NOT NULL,
service_name VARCHAR(80),
service_descriptio VARCHAR(255)
);

















CREATE TABLE Forms (
form_id INTEGER PRIMARY KEY,
form_type_code VARCHAR(15) NOT NULL,
service_id INTEGER,
form_number VARCHAR(50),
form_name VARCHAR(80),
form_description VARCHAR(255),
FOREIGN KEY (service_id ) REFERENCES Services(service_id )
);










CREATE TABLE Individuals (
individual_id INTEGER PRIMARY KEY,
individual_first_name VARCHAR(80),
individual_middle_name VARCHAR(80),
inidividual_phone VARCHAR(80),
individual_email VARCHAR(80),
individual_address VARCHAR(255),
individual_last_name VARCHAR(80)
);

















CREATE TABLE Organizations (
organization_id INTEGER PRIMARY KEY,
date_formed DATETIME,
organization_name VARCHAR(255),
uk_vat_number VARCHAR(20)
);







CREATE TABLE Parties (
party_id INTEGER PRIMARY KEY,
payment_method_code VARCHAR(15) NOT NULL,
party_phone VARCHAR(80),
party_email VARCHAR(80)
);

















CREATE TABLE Organization_Contact_Individuals (
individual_id INTEGER NOT NULL,
organization_id INTEGER NOT NULL,
date_contact_from DATETIME NOT NULL,
date_contact_to DATETIME,
PRIMARY KEY (individual_id,organization_id ),
FOREIGN KEY (organization_id ) REFERENCES Organizations(organization_id ),
FOREIGN KEY (individual_id ) REFERENCES Individuals(individual_id )
);


















CREATE TABLE Party_Addresses (
party_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_address_from DATETIME NOT NULL,
address_type_code VARCHAR(15) NOT NULL,
date_address_to DATETIME,
PRIMARY KEY (party_id, address_id),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id ),
FOREIGN KEY (party_id ) REFERENCES Parties(party_id )
);

















CREATE TABLE Party_Forms (
party_id INTEGER NOT NULL,
form_id INTEGER NOT NULL,
date_completion_started DATETIME NOT NULL,
form_status_code VARCHAR(15) NOT NULL,
date_fully_completed DATETIME,
PRIMARY KEY (party_id, form_id),
FOREIGN KEY (party_id ) REFERENCES Parties(party_id ),
FOREIGN KEY (form_id ) REFERENCES Forms(form_id )
);
CREATE TABLE Party_Services (
booking_id INTEGER NOT NULL ,
customer_id INTEGER NOT NULL,
service_id INTEGER NOT NULL,
service_datetime DATETIME NOT NULL,
booking_made_date DATETIME,
FOREIGN KEY (service_id ) REFERENCES Services(service_id ),
FOREIGN KEY (customer_id ) REFERENCES Parties(party_id )
);





























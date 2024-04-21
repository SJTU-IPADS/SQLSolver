

CREATE TABLE Roles (
role_code VARCHAR(15) PRIMARY KEY,
role_description VARCHAR(80)
);

CREATE TABLE Users (
user_id INTEGER PRIMARY KEY,
role_code VARCHAR(15) NOT NULL,
user_name VARCHAR(40),
user_login VARCHAR(40),
password VARCHAR(40),
FOREIGN KEY (role_code ) REFERENCES Roles(role_code )
);


















CREATE TABLE Document_Structures (
document_structure_code VARCHAR(15) PRIMARY KEY,
parent_document_structure_code VARCHAR(15),
document_structure_description VARCHAR(80)
);







CREATE TABLE Functional_Areas (
functional_area_code VARCHAR(15) PRIMARY KEY,
parent_functional_area_code VARCHAR(15),
functional_area_description VARCHAR(80) NOT NULL
);






CREATE TABLE Images (
image_id INTEGER PRIMARY KEY,
image_alt_text VARCHAR(80),
image_name VARCHAR(40),
image_url VARCHAR(255)
);

















CREATE TABLE Documents (
document_code VARCHAR(15) PRIMARY KEY,
document_structure_code VARCHAR(15) NOT NULL,
document_type_code VARCHAR(15) NOT NULL,
access_count INTEGER,
document_name VARCHAR(80),
FOREIGN KEY (document_structure_code ) REFERENCES Document_Structures(document_structure_code )
);




















CREATE TABLE Document_Functional_Areas (
document_code VARCHAR(15) NOT NULL,
functional_area_code VARCHAR(15) NOT NULL,
FOREIGN KEY (document_code ) REFERENCES Documents(document_code ),
FOREIGN KEY (functional_area_code ) REFERENCES Functional_Areas(functional_area_code )
);

















CREATE TABLE Document_Sections (
section_id INTEGER PRIMARY KEY,
document_code VARCHAR(15) NOT NULL,
section_sequence INTEGER,
section_code VARCHAR(20),
section_title VARCHAR(80),
FOREIGN KEY (document_code ) REFERENCES Documents(document_code )
);
















CREATE TABLE Document_Sections_Images (
section_id INTEGER NOT NULL,
image_id INTEGER NOT NULL,
PRIMARY KEY (section_id,image_id),
FOREIGN KEY (section_id ) REFERENCES Document_Sections(section_id ),
FOREIGN KEY (image_id ) REFERENCES Images(image_id )
);






















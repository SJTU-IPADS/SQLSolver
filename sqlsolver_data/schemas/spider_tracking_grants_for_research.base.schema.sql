
CREATE TABLE Document_Types (
document_type_code VARCHAR(10) PRIMARY KEY,
document_description VARCHAR(255) NOT NULL
);
CREATE TABLE Documents (
document_id INTEGER PRIMARY KEY,
document_type_code VARCHAR(10),
grant_id INTEGER NOT NULL,
sent_date DATETIME NOT NULL,
response_received_date DATETIME NOT NULL,
other_details VARCHAR(255) NOT NULL,
FOREIGN KEY (document_type_code ) REFERENCES Document_Types(document_type_code ),
FOREIGN KEY (grant_id ) REFERENCES Grants(grant_id )
);
CREATE TABLE Grants (
grant_id INTEGER PRIMARY KEY,
organisation_id INTEGER NOT NULL,
grant_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
grant_start_date DATETIME NOT NULL,
grant_end_date DATETIME NOT NULL,
other_details VARCHAR(255) NOT NULL,
FOREIGN KEY (organisation_id ) REFERENCES Organisations(organisation_id )
);
CREATE TABLE Organisation_Types (
organisation_type VARCHAR(10) PRIMARY KEY,
organisation_type_description VARCHAR(255) NOT NULL
);
CREATE TABLE Organisations (
organisation_id INTEGER PRIMARY KEY,
organisation_type VARCHAR(10) NOT NULL,
organisation_details VARCHAR(255) NOT NULL,
FOREIGN KEY (organisation_type ) REFERENCES Organisation_Types(organisation_type )
);
CREATE TABLE Project_Outcomes (
project_id INTEGER NOT NULL,
outcome_code VARCHAR(10) NOT NULL,
outcome_details VARCHAR(255),
FOREIGN KEY (project_id ) REFERENCES Projects(project_id ),FOREIGN KEY (outcome_code ) REFERENCES Research_Outcomes(outcome_code )
);
CREATE TABLE Project_Staff (
staff_id DOUBLE PRIMARY KEY,
project_id INTEGER NOT NULL,
role_code VARCHAR(10) NOT NULL,
date_from DATETIME,
date_to DATETIME,
other_details VARCHAR(255),
FOREIGN KEY (project_id ) REFERENCES Projects(project_id ),FOREIGN KEY (role_code ) REFERENCES Staff_Roles(role_code )
);
CREATE TABLE Projects (
project_id INTEGER PRIMARY KEY,
organisation_id INTEGER NOT NULL,
project_details VARCHAR(255) NOT NULL,
FOREIGN KEY (organisation_id ) REFERENCES Organisations(organisation_id )
);
CREATE TABLE Research_Outcomes (
outcome_code VARCHAR(10) PRIMARY KEY,
outcome_description VARCHAR(255) NOT NULL
);
CREATE TABLE Research_Staff (
staff_id INTEGER PRIMARY KEY,
employer_organisation_id INTEGER NOT NULL,
staff_details VARCHAR(255) NOT NULL,
FOREIGN KEY (employer_organisation_id ) REFERENCES Organisations(organisation_id )
);
CREATE TABLE Staff_Roles (
role_code VARCHAR(10) PRIMARY KEY,
role_description VARCHAR(255) NOT NULL
);
CREATE TABLE Tasks (
task_id INTEGER PRIMARY KEY,
project_id INTEGER NOT NULL,
task_details VARCHAR(255) NOT NULL,
eg Agree Objectives VARCHAR(1),
FOREIGN KEY (project_id ) REFERENCES Projects(project_id )
);




































































































































CREATE TABLE Ref_Address_Types (
address_type_code VARCHAR(15) PRIMARY KEY,
address_type_description VARCHAR(80)
);
CREATE TABLE Ref_Detention_Type (
detention_type_code VARCHAR(10) PRIMARY KEY,
detention_type_description VARCHAR(80)
);
CREATE TABLE Ref_Incident_Type (
incident_type_code VARCHAR(10) PRIMARY KEY,
incident_type_description VARCHAR(80)
);













CREATE TABLE Addresses (
address_id INTEGER PRIMARY KEY,
line_1 VARCHAR(120),
line_2 VARCHAR(120),
line_3 VARCHAR(120),
city VARCHAR(80),
zip_postcode VARCHAR(20),
state_province_county VARCHAR(50),
country VARCHAR(50),
other_address_details VARCHAR(255)
);























CREATE TABLE Students (
student_id INTEGER PRIMARY KEY,
address_id INTEGER NOT NULL,
first_name VARCHAR(80),
middle_name VARCHAR(40),
last_name VARCHAR(40),
cell_mobile_number VARCHAR(40),
email_address VARCHAR(40),
date_first_rental DATETIME,
date_left_university DATETIME,
other_student_details VARCHAR(255),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id )
);


















CREATE TABLE Teachers (
teacher_id INTEGER PRIMARY KEY,
address_id INTEGER NOT NULL,
first_name VARCHAR(80),
middle_name VARCHAR(80),
last_name VARCHAR(80),
gender VARCHAR(1),
cell_mobile_number VARCHAR(40),
email_address VARCHAR(40),
other_details VARCHAR(255),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id )
);

















CREATE TABLE Assessment_Notes (
notes_id INTEGER NOT NULL ,
student_id INTEGER,
teacher_id INTEGER NOT NULL,
date_of_notes DATETIME,
text_of_notes VARCHAR(255),
other_details VARCHAR(255),
FOREIGN KEY (student_id ) REFERENCES Students(student_id ),
FOREIGN KEY (teacher_id ) REFERENCES Teachers(teacher_id )
);

















CREATE TABLE Behavior_Incident (
incident_id INTEGER PRIMARY KEY,
incident_type_code VARCHAR(10) NOT NULL,
student_id INTEGER NOT NULL,
date_incident_start DATETIME,
date_incident_end DATETIME,
incident_summary VARCHAR(255),
recommendations VARCHAR(255),
other_details VARCHAR(255),
FOREIGN KEY (incident_type_code ) REFERENCES Ref_Incident_Type(incident_type_code ),
FOREIGN KEY (student_id ) REFERENCES Students(student_id )
);

















CREATE TABLE Detention (
detention_id INTEGER PRIMARY KEY,
detention_type_code VARCHAR(10) NOT NULL,
teacher_id INTEGER,
datetime_detention_start DATETIME,
datetime_detention_end DATETIME,
detention_summary VARCHAR(255),
other_details VARCHAR(255),
FOREIGN KEY (detention_type_code ) REFERENCES Ref_Detention_Type(detention_type_code ),
FOREIGN KEY (teacher_id ) REFERENCES Teachers(teacher_id )
);

CREATE TABLE Student_Addresses (
student_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_address_from DATETIME NOT NULL,
date_address_to DATETIME,
monthly_rental DECIMAL(19,4),
other_details VARCHAR(255),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id ),
FOREIGN KEY (student_id ) REFERENCES Students(student_id )
);

CREATE TABLE Students_in_Detention (
student_id INTEGER NOT NULL,
detention_id INTEGER NOT NULL,
incident_id INTEGER NOT NULL,
FOREIGN KEY (incident_id ) REFERENCES Behavior_Incident(incident_id ),
FOREIGN KEY (detention_id ) REFERENCES Detention(detention_id ),
FOREIGN KEY (student_id ) REFERENCES Students(student_id )
);




















































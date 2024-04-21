CREATE TABLE Breeds (
breed_code VARCHAR(10) PRIMARY KEY ,
breed_name VARCHAR(80)
);

CREATE TABLE Charges (
charge_id INT PRIMARY KEY ,
charge_type VARCHAR(10),
charge_amount DECIMAL(19,4)
);

CREATE TABLE Sizes (
size_code VARCHAR(10) PRIMARY KEY ,
size_description VARCHAR(80)
);

CREATE TABLE Treatment_Types (
treatment_type_code VARCHAR(10) PRIMARY KEY ,
treatment_type_description VARCHAR(80)
);

CREATE TABLE Owners (
owner_id INT PRIMARY KEY ,
first_name VARCHAR(50),
last_name VARCHAR(50),
street VARCHAR(50),
city VARCHAR(50),
state VARCHAR(20),
zip_code VARCHAR(20),
email_address VARCHAR(50),
home_phone VARCHAR(20),
cell_number VARCHAR(20)
);

CREATE TABLE Dogs (
dog_id INT PRIMARY KEY ,
owner_id INT NOT NULL,
abandoned_yn VARCHAR(1),
breed_code VARCHAR(10) NOT NULL,
size_code VARCHAR(10) NOT NULL,
name VARCHAR(50),
age VARCHAR(20),
date_of_birth DATETIME,
gender VARCHAR(1),
weight VARCHAR(20),
date_arrived DATETIME,
date_adopted DATETIME,
date_departed DATETIME,
FOREIGN KEY (breed_code) REFERENCES Breeds(breed_code),
FOREIGN KEY (size_code) REFERENCES Sizes(size_code),
FOREIGN KEY (owner_id) REFERENCES Owners(owner_id)
);

CREATE TABLE Professionals (
professional_id INT PRIMARY KEY ,
role_code VARCHAR(10) NOT NULL,
first_name VARCHAR(50),
street VARCHAR(50),
city VARCHAR(50),
state VARCHAR(20),
zip_code VARCHAR(20),
last_name VARCHAR(50),
email_address VARCHAR(50),
home_phone VARCHAR(20),
cell_number VARCHAR(20)
);

CREATE TABLE Treatments (
treatment_id INT PRIMARY KEY ,
dog_id INT NOT NULL,
professional_id INT NOT NULL,
treatment_type_code VARCHAR(10) NOT NULL,
date_of_treatment DATETIME,
cost_of_treatment DECIMAL(19, 4),
FOREIGN KEY (treatment_type_code) REFERENCES Treatment_Types(treatment_type_code),
FOREIGN KEY (professional_id) REFERENCES Professionals(professional_id),
FOREIGN KEY (dog_id) REFERENCES Dogs(dog_id)
);
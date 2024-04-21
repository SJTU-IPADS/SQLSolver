
CREATE TABLE Ref_Document_Types (
document_type_code CHAR(15) NOT NULL,
document_type_description VARCHAR(255) NOT NULL,
PRIMARY KEY (document_type_code)
);




CREATE TABLE Roles (
role_code CHAR(15) NOT NULL,
role_description VARCHAR(255),
PRIMARY KEY (role_code)
);





CREATE TABLE Addresses (
address_id INTEGER NOT NULL,
address_details VARCHAR(255),
PRIMARY KEY (address_id)
);











CREATE TABLE Ref_Document_Status (
document_status_code CHAR(15) NOT NULL,
document_status_description VARCHAR(255) NOT NULL,
PRIMARY KEY (document_status_code)
);




CREATE TABLE Ref_Shipping_Agents (
shipping_agent_code CHAR(15) NOT NULL,
shipping_agent_name VARCHAR(255) NOT NULL,
shipping_agent_description VARCHAR(255) NOT NULL,
PRIMARY KEY (shipping_agent_code)
);






CREATE TABLE Documents (
document_id INTEGER NOT NULL,
document_status_code CHAR(15) NOT NULL,
document_type_code CHAR(15) NOT NULL,
shipping_agent_code CHAR(15),
receipt_date DATETIME,
receipt_number VARCHAR(255),
other_details VARCHAR(255),
PRIMARY KEY (document_id),
FOREIGN KEY (document_type_code) REFERENCES Ref_Document_Types (document_type_code),
FOREIGN KEY (document_status_code) REFERENCES Ref_Document_Status (document_status_code),
FOREIGN KEY (shipping_agent_code) REFERENCES Ref_Shipping_Agents (shipping_agent_code)
);
















CREATE TABLE Employees (
employee_id INTEGER NOT NULL,
role_code CHAR(15) NOT NULL,
employee_name VARCHAR(255),
other_details VARCHAR(255),
PRIMARY KEY (employee_id),
FOREIGN KEY (role_code) REFERENCES Roles (role_code)
);







CREATE TABLE Document_Drafts (
document_id INTEGER NOT NULL,
draft_number INTEGER NOT NULL,
draft_details VARCHAR(255),
PRIMARY KEY (document_id, draft_number),
FOREIGN KEY (document_id) REFERENCES Documents (document_id)
);
















CREATE TABLE Draft_Copies (
document_id INTEGER NOT NULL,
draft_number INTEGER NOT NULL,
copy_number INTEGER NOT NULL,
PRIMARY KEY (document_id, draft_number, copy_number),
FOREIGN KEY (document_id, draft_number) REFERENCES Document_Drafts (document_id,draft_number)
);









CREATE TABLE Circulation_History (
document_id INTEGER NOT NULL,
draft_number INTEGER NOT NULL,
copy_number INTEGER NOT NULL,
employee_id INTEGER NOT NULL,
PRIMARY KEY (document_id, draft_number, copy_number, employee_id),
FOREIGN KEY (document_id, draft_number, copy_number) REFERENCES Draft_Copies (document_id,draft_number,copy_number),
FOREIGN KEY (employee_id) REFERENCES Employees (employee_id)
);





CREATE TABLE Documents_Mailed (
document_id INTEGER NOT NULL,
mailed_to_address_id INTEGER NOT NULL,
mailing_date DATETIME,
PRIMARY KEY (document_id, mailed_to_address_id),
FOREIGN KEY (document_id) REFERENCES Documents (document_id),
FOREIGN KEY (mailed_to_address_id) REFERENCES Addresses (address_id)
);
















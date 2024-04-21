

CREATE TABLE Third_Party_Companies (
company_id INTEGER PRIMARY KEY ,
company_type VARCHAR(5) NOT NULL,
company_name VARCHAR(255),
company_address VARCHAR(255),
other_company_details VARCHAR(255)
);


















CREATE TABLE Maintenance_Contracts (
maintenance_contract_id INTEGER PRIMARY KEY,
maintenance_contract_company_id INTEGER NOT NULL,
contract_start_date DATETIME,
contract_end_date DATETIME,
other_contract_details VARCHAR(255),
FOREIGN KEY (maintenance_contract_company_id ) REFERENCES Third_Party_Companies(company_id )
);


















CREATE TABLE Parts (
part_id INTEGER PRIMARY KEY,
part_name VARCHAR(255),
chargeable_yn VARCHAR(1),
chargeable_amount VARCHAR(20),
other_part_details VARCHAR(255)
);





CREATE TABLE Skills (
skill_id INTEGER PRIMARY KEY,
skill_code VARCHAR(20),
skill_description VARCHAR(255)
);






CREATE TABLE Staff (
staff_id INTEGER PRIMARY KEY,
staff_name VARCHAR(255),
gender VARCHAR(1),
other_staff_details VARCHAR(255)
);


















CREATE TABLE Assets (
asset_id INTEGER PRIMARY KEY,
maintenance_contract_id INTEGER NOT NULL,
supplier_company_id INTEGER NOT NULL,
asset_details VARCHAR(255),
asset_make VARCHAR(20),
asset_model VARCHAR(20),
asset_acquired_date DATETIME,
asset_disposed_date DATETIME,
other_asset_details VARCHAR(255),
FOREIGN KEY (maintenance_contract_id )
REFERENCES Maintenance_Contracts(maintenance_contract_id ),
FOREIGN KEY (supplier_company_id ) REFERENCES Third_Party_Companies(company_id )
);





















CREATE TABLE Asset_Parts (
asset_id INTEGER NOT NULL,
part_id INTEGER NOT NULL,
FOREIGN KEY (part_id ) REFERENCES Parts(part_id ),
FOREIGN KEY (asset_id ) REFERENCES Assets(asset_id )
);



















CREATE TABLE Maintenance_Engineers (
engineer_id INTEGER PRIMARY KEY,
company_id INTEGER NOT NULL,
first_name VARCHAR(50),
last_name VARCHAR(50),
other_details VARCHAR(255),
FOREIGN KEY (company_id ) REFERENCES Third_Party_Companies(company_id )
);




















CREATE TABLE Engineer_Skills (
engineer_id INTEGER NOT NULL,
skill_id INTEGER NOT NULL,
FOREIGN KEY (engineer_id ) REFERENCES Maintenance_Engineers(engineer_id ),
FOREIGN KEY (skill_id ) REFERENCES Skills(skill_id )
);






















CREATE TABLE Fault_Log (
fault_log_entry_id INTEGER PRIMARY KEY,
asset_id INTEGER NOT NULL,
recorded_by_staff_id INTEGER NOT NULL,
fault_log_entry_datetime DATETIME,
fault_description VARCHAR(255),
other_fault_details VARCHAR(255),
FOREIGN KEY (asset_id ) REFERENCES Assets(asset_id ),
FOREIGN KEY (recorded_by_staff_id ) REFERENCES Staff(staff_id )
);

















CREATE TABLE Engineer_Visits (
engineer_visit_id INTEGER PRIMARY KEY,
contact_staff_id INTEGER,
engineer_id INTEGER NOT NULL,
fault_log_entry_id INTEGER NOT NULL,
fault_status VARCHAR(10) NOT NULL,
visit_start_datetime DATETIME,
visit_end_datetime DATETIME,
other_visit_details VARCHAR(255),
FOREIGN KEY (fault_log_entry_id ) REFERENCES Fault_Log(fault_log_entry_id ),
FOREIGN KEY (engineer_id ) REFERENCES Maintenance_Engineers(engineer_id ),
FOREIGN KEY (contact_staff_id ) REFERENCES Staff(staff_id )
);





















CREATE TABLE Part_Faults (
part_fault_id INTEGER PRIMARY KEY,
part_id INTEGER NOT NULL,
fault_short_name VARCHAR(20),
fault_description VARCHAR(255),
other_fault_details VARCHAR(255),
FOREIGN KEY (part_id ) REFERENCES Parts(part_id )
);


CREATE TABLE Fault_Log_Parts (
fault_log_entry_id INTEGER NOT NULL,
part_fault_id INTEGER NOT NULL,
fault_status VARCHAR(10) NOT NULL,
FOREIGN KEY (part_fault_id ) REFERENCES Part_Faults(part_fault_id ),
FOREIGN KEY (fault_log_entry_id ) REFERENCES Fault_Log(fault_log_entry_id )
);




































CREATE TABLE Skills_Required_To_Fix (
part_fault_id INTEGER NOT NULL,
skill_id INTEGER NOT NULL,
FOREIGN KEY (part_fault_id ) REFERENCES Part_Faults(part_fault_id ),
FOREIGN KEY (skill_id ) REFERENCES Skills(skill_id )
);





















CREATE TABLE Problem_Category_Codes (
problem_category_code VARCHAR(20) PRIMARY KEY,
problem_category_description VARCHAR(80)
);



CREATE TABLE Problem_Log (
problem_log_id INTEGER PRIMARY KEY,
assigned_to_staff_id INTEGER NOT NULL,
problem_id INTEGER NOT NULL,
problem_category_code VARCHAR(20) NOT NULL,
problem_status_code VARCHAR(20) NOT NULL,
log_entry_date DATETIME,
log_entry_description VARCHAR(255),
log_entry_fix VARCHAR(255),
other_log_details VARCHAR(255),
FOREIGN KEY (problem_category_code ) REFERENCES Problem_Category_Codes(problem_category_code ),FOREIGN KEY (assigned_to_staff_id ) REFERENCES Staff(staff_id ),FOREIGN KEY (problem_id ) REFERENCES Problems(problem_id ),FOREIGN KEY (problem_status_code ) REFERENCES Problem_Status_Codes(problem_status_code )
);
CREATE TABLE Problem_Status_Codes (
problem_status_code VARCHAR(20) PRIMARY KEY,
problem_status_description VARCHAR(80)
);



CREATE TABLE Product (
product_id INTEGER PRIMARY KEY,
product_name VARCHAR(80),
product_details VARCHAR(255)
);
CREATE TABLE Staff (
staff_id INTEGER PRIMARY KEY,
staff_first_name VARCHAR(80),
staff_last_name VARCHAR(80),
other_staff_details VARCHAR(255)
);

CREATE TABLE Problems (
problem_id INTEGER PRIMARY KEY,
product_id INTEGER NOT NULL,
closure_authorised_by_staff_id INTEGER NOT NULL,
reported_by_staff_id INTEGER NOT NULL,
date_problem_reported DATETIME NOT NULL,
date_problem_closed DATETIME,
problem_description VARCHAR(255),
other_problem_details VARCHAR(255),
FOREIGN KEY (closure_authorised_by_staff_id ) REFERENCES Staff(staff_id ),
FOREIGN KEY (product_id ) REFERENCES Product(product_id ),
FOREIGN KEY (reported_by_staff_id ) REFERENCES Staff(staff_id )
);




































































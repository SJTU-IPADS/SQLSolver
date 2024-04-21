


CREATE TABLE Staff (
staff_id INTEGER PRIMARY KEY,
gender VARCHAR(1),
first_name VARCHAR(80),
last_name VARCHAR(80),
email_address VARCHAR(255),
phone_number VARCHAR(80)
);









CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
customer_type_code VARCHAR(20) NOT NULL,
address_line_1 VARCHAR(80),
address_line_2 VARCHAR(80),
town_city VARCHAR(80),
state VARCHAR(80),
email_address VARCHAR(255),
phone_number VARCHAR(80)
);










CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
parent_product_id INTEGER,
product_category_code VARCHAR(20) NOT NULL,
date_product_first_available DATETIME,
date_product_discontinued DATETIME,
product_name VARCHAR(80),
product_description VARCHAR(255),
product_price DECIMAL(19,4)
);






CREATE TABLE Complaints (
complaint_id INTEGER NOT NULL ,
product_id INTEGER NOT NULL,
customer_id INTEGER NOT NULL,
complaint_outcome_code VARCHAR(20) NOT NULL,
complaint_status_code VARCHAR(20) NOT NULL,
complaint_type_code VARCHAR(20) NOT NULL,
date_complaint_raised DATETIME,
date_complaint_closed DATETIME,
staff_id INTEGER NOT NULL ,
FOREIGN KEY (staff_id ) REFERENCES Staff(staff_id ),
FOREIGN KEY (product_id ) REFERENCES Products(product_id ),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);














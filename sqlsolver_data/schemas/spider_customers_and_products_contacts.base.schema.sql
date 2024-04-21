
CREATE TABLE Addresses (
address_id INTEGER PRIMARY KEY,
line_1_number_building VARCHAR(80),
city VARCHAR(50),
zip_postcode VARCHAR(20),
state_province_county VARCHAR(50),
country VARCHAR(50)
);



















CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
product_type_code VARCHAR(15),
product_name VARCHAR(80),
product_price DOUBLE NULL
);

















CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
payment_method_code VARCHAR(15),
customer_number VARCHAR(20),
customer_name VARCHAR(80),
customer_address VARCHAR(255),
customer_phone VARCHAR(80),
customer_email VARCHAR(80)
);
















CREATE TABLE Contacts (
contact_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
gender VARCHAR(1),
first_name VARCHAR(80),
last_name VARCHAR(50),
contact_phone VARCHAR(80)
);


















CREATE TABLE Customer_Address_History (
customer_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_from DATETIME NOT NULL,
date_to DATETIME,
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id ),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id )
);
CREATE TABLE Customer_Orders (
order_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_date DATETIME NOT NULL,
order_status_code VARCHAR(15),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);

CREATE TABLE Order_Items (
order_item_id INTEGER NOT NULL ,
order_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
order_quantity VARCHAR(80),
FOREIGN KEY (product_id ) REFERENCES Products(product_id ),
FOREIGN KEY (order_id ) REFERENCES Customer_Orders(order_id )
);





















































CREATE TABLE Premises (
premise_id INTEGER PRIMARY KEY,
premises_type VARCHAR(15) NOT NULL,
premise_details VARCHAR(255)
);

















CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
product_category VARCHAR(15) NOT NULL,
product_name VARCHAR(80)
);



















CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
payment_method VARCHAR(15) NOT NULL,
customer_name VARCHAR(80),
customer_phone VARCHAR(80),
customer_email VARCHAR(80),
customer_address VARCHAR(255),
customer_login VARCHAR(80),
customer_password VARCHAR(10)
);

















CREATE TABLE Mailshot_Campaigns (
mailshot_id INTEGER PRIMARY KEY,
product_category VARCHAR(15),
mailshot_name VARCHAR(80),
mailshot_start_date DATETIME,
mailshot_end_date DATETIME
);






















CREATE TABLE Customer_Addresses (
customer_id INTEGER NOT NULL,
premise_id INTEGER NOT NULL,
date_address_from DATETIME NOT NULL,
address_type_code VARCHAR(15) NOT NULL,
date_address_to DATETIME,
FOREIGN KEY (premise_id ) REFERENCES Premises(premise_id )
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);


















CREATE TABLE Customer_Orders (
order_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_status_code VARCHAR(15) NOT NULL,
shipping_method_code VARCHAR(15) NOT NULL,
order_placed_datetime DATETIME NOT NULL,
order_delivered_datetime DATETIME,
order_shipping_charges VARCHAR(255),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);

CREATE TABLE Mailshot_Customers (
mailshot_id INTEGER NOT NULL,
customer_id INTEGER NOT NULL,
outcome_code VARCHAR(15) NOT NULL,
mailshot_customer_date DATETIME,
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id ),
FOREIGN KEY (mailshot_id ) REFERENCES Mailshot_Campaigns(mailshot_id )
);
CREATE TABLE Order_Items (
item_id INTEGER NOT NULL ,
order_item_status_code VARCHAR(15) NOT NULL,
order_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
item_status_code VARCHAR(15),
item_delivered_datetime DATETIME,
item_order_quantity VARCHAR(80),
FOREIGN KEY (product_id ) REFERENCES Products(product_id ),
FOREIGN KEY (order_id ) REFERENCES Customer_Orders(order_id )
);















































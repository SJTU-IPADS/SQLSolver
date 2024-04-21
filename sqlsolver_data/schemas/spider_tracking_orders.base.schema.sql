
CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
customer_name VARCHAR(80),
customer_details VARCHAR(255)
);
CREATE TABLE Invoices (
invoice_number INTEGER PRIMARY KEY,
invoice_date DATETIME,
invoice_details VARCHAR(255)
);

CREATE TABLE Orders (
order_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_status VARCHAR(10) NOT NULL,
date_order_placed DATETIME NOT NULL,
order_details VARCHAR(255),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);

CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
product_name VARCHAR(80),
product_details VARCHAR(255)
);

CREATE TABLE Order_Items (
order_item_id INTEGER PRIMARY KEY,
product_id INTEGER NOT NULL,
order_id INTEGER NOT NULL,
order_item_status VARCHAR(10) NOT NULL,
order_item_details VARCHAR(255),
FOREIGN KEY (order_id ) REFERENCES Orders(order_id ),
FOREIGN KEY (product_id ) REFERENCES Products(product_id )
);

CREATE TABLE Shipments (
shipment_id INTEGER PRIMARY KEY,
order_id INTEGER NOT NULL,
invoice_number INTEGER NOT NULL,
shipment_tracking_number VARCHAR(80),
shipment_date DATETIME,
other_shipment_details VARCHAR(255),
FOREIGN KEY (order_id ) REFERENCES Orders(order_id ),
FOREIGN KEY (invoice_number ) REFERENCES Invoices(invoice_number )
);

CREATE TABLE Shipment_Items (
shipment_id INTEGER NOT NULL,
order_item_id INTEGER NOT NULL,
FOREIGN KEY (order_item_id ) REFERENCES Order_Items(order_item_id ),
FOREIGN KEY (shipment_id ) REFERENCES Shipments(shipment_id )
);

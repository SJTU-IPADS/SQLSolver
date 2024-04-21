
CREATE TABLE Addresses (
address_id INTEGER PRIMARY KEY,
address_details VARCHAR(255)
);



East Burdettestad, IA 21232');

Port Abefurt, IA 84402-4249');

West Lindsey, DE 76199-8015');

Marvinburgh, OH 16085-1623');

Jenkinsmouth, OK 22345');

Port Isaac, NV 61159');

Langworthborough, OH 95195');

New Cali, RI 42319');

Lake Zariaburgh, IL 98085');

Pourosfurt, IA 98649');

Mooreside, ME 41586-5022');

Wilkinsonstad, CO 79055-7622');

East Rheaview, ID 47493');

Willmsport, NV 36680');

Reingerland, HI 97099-1005');

CREATE TABLE Staff (
staff_id INTEGER PRIMARY KEY,
staff_gender VARCHAR(1),
staff_name VARCHAR(80)
);

















CREATE TABLE Suppliers (
supplier_id INTEGER PRIMARY KEY,
supplier_name VARCHAR(80),
supplier_phone VARCHAR(80)
);






CREATE TABLE Department_Store_Chain (
dept_store_chain_id INTEGER PRIMARY KEY,
dept_store_chain_name VARCHAR(80)
);







CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
payment_method_code VARCHAR(10) NOT NULL,
customer_code VARCHAR(20),
customer_name VARCHAR(80),
customer_address VARCHAR(255),
customer_phone VARCHAR(80),
customer_email VARCHAR(80)
);

South Norrisland, SC 80546', '254-072-4068x33935', 'margarett.vonrueden@example.com');

East Dasiabury, IL 72656-3552', '+41(8)1897032009', 'stiedemann.sigrid@example.com');

Lake Annalise, TN 35791-8871', '197-417-3557', 'joelle.monahan@example.com');

East Cathryn, WY 30751-4404', '+08(3)8056580281', 'gbrekke@example.com');

East Chris, NH 41624', '1-064-498-6609x051', 'nicholas44@example.com');

South Dionbury, NC 62021', '(443)013-3112x528', 'cconroy@example.net');

Schneiderland, IA 93624', '877-150-8674x63517', 'shawna.cummerata@example.net');

Coymouth, IL 97300-7731', '1-695-364-7586x59256', 'kathlyn24@example.org');

Lake Moriahbury, OH 91556-2122', '587.398.2400x31176', 'ludwig54@example.net');

Lizethtown, DE 56522', '857-844-9339x40140', 'moriah91@example.com');

New Mckenna, CA 98525-5674', '(730)934-8249', 'qstokes@example.org');

Port Zita, SD 39410', '117.822.3577', 'gwisozk@example.net');

Yesseniaville, TN 60847', '08023680831', 'maxime86@example.net');

Darrionborough, SC 53915-0479', '07594320656', 'celine.bogan@example.com');

Wyatttown, UT 12697', '1-472-036-0434', 'schultz.arnoldo@example.net');


CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
product_type_code VARCHAR(10) NOT NULL,
product_name VARCHAR(80),
product_price DECIMAL(19,4)
);
















CREATE TABLE Supplier_Addresses (
supplier_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_from DATETIME NOT NULL,
date_to DATETIME,
PRIMARY KEY (supplier_id, address_id),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id ),
FOREIGN KEY (supplier_id ) REFERENCES Suppliers(supplier_id )
);







CREATE TABLE Customer_Addresses (
customer_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_from DATETIME NOT NULL,
date_to DATETIME,
PRIMARY KEY (customer_id, address_id),
FOREIGN KEY (address_id ) REFERENCES Addresses(address_id ),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);




















CREATE TABLE Customer_Orders (
order_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
order_status_code VARCHAR(10) NOT NULL,
order_date DATETIME NOT NULL,
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);


















CREATE TABLE Department_Stores (
dept_store_id INTEGER PRIMARY KEY,
dept_store_chain_id INTEGER,
store_name VARCHAR(80),
store_address VARCHAR(255),
store_phone VARCHAR(80),
store_email VARCHAR(80),
FOREIGN KEY (dept_store_chain_id ) REFERENCES Department_Store_Chain(dept_store_chain_id )
);



North Arielle, MS 51249', '(948)944-5099x2027', 'bmaggio@example.com');

O\'Connellshire, IL 31732', '877-917-5029', 'larissa10@example.org');

North Wadeton, WV 27575-3951', '1-216-312-0375', 'alexandro.mcclure@example.net');

Mitchellton, TN 84209', '670-466-6367', 'bryon24@example.org');

Sporermouth, MN 25962', '01399327266', 'creola23@example.org');

Ninamouth, WA 86667', '1-859-843-1957', 'jerod.reynolds@example.net');

New Eviestad, NY 17573', '1-109-872-9142x77078', 'ihamill@example.org');

West Zacheryshire, NC 17408', '+67(5)4983519062', 'casper.adolfo@example.org');

Devonton, NJ 61782-9006', '(723)503-7086x356', 'selmer.stiedemann@example.org');

Strosinville, VA 03998-3292', '07126036440', 'luisa57@example.org');

South Jeremiehaven, GA 08730', '611-037-9309', 'vonrueden.vern@example.org');

North Ginahaven, CT 85046', '(626)763-7031', 'freda.toy@example.org');

West Dulceside, UT 58085-8998', '1-764-126-7567x0795', 'katlynn62@example.com');

North Garettton, AL 84756-4375', '319.331.3397', 'mohr.elwin@example.net');

Wehnermouth, NC 76791', '(587)993-3604x3077', 'kelly30@example.com');


CREATE TABLE Departments (
department_id INTEGER PRIMARY KEY,
dept_store_id INTEGER NOT NULL,
department_name VARCHAR(80),
FOREIGN KEY (dept_store_id ) REFERENCES Department_Stores(dept_store_id )
);







CREATE TABLE Order_Items (
order_item_id INTEGER PRIMARY KEY,
order_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
FOREIGN KEY (order_id ) REFERENCES Customer_Orders(order_id ),
FOREIGN KEY (product_id ) REFERENCES Products(product_id )
);















CREATE TABLE Product_Suppliers (
product_id INTEGER NOT NULL,
supplier_id INTEGER NOT NULL,
date_supplied_from DATETIME NOT NULL,
date_supplied_to DATETIME,
total_amount_purchased VARCHAR(80),
total_value_purchased DECIMAL(19,4),
PRIMARY KEY (product_id, supplier_id),
FOREIGN KEY (supplier_id ) REFERENCES Suppliers(supplier_id ),
FOREIGN KEY (product_id ) REFERENCES Products(product_id )
);


CREATE TABLE Staff_Department_Assignments (
staff_id INTEGER NOT NULL,
department_id INTEGER NOT NULL,
date_assigned_from DATETIME NOT NULL,
job_title_code VARCHAR(10) NOT NULL,
date_assigned_to DATETIME,
PRIMARY KEY (staff_id, department_id),
FOREIGN KEY (department_id ) REFERENCES Departments(department_id ),
FOREIGN KEY (staff_id ) REFERENCES Staff(staff_id )
);



































CREATE TABLE Discount_Coupons (
coupon_id INTEGER PRIMARY KEY,
date_issued DATETIME,
coupon_amount DECIMAL(19,4)
);

















CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
coupon_id INTEGER NOT NULL,
good_or_bad_customer VARCHAR(4),
first_name VARCHAR(80),
last_name VARCHAR(80),
gender_mf VARCHAR(1),
date_became_customer DATETIME,
date_last_hire DATETIME,
FOREIGN KEY (coupon_id ) REFERENCES Discount_Coupons(coupon_id )
);

















CREATE TABLE Bookings (
booking_id INTEGER PRIMARY KEY ,
customer_id INTEGER NOT NULL,
booking_status_code VARCHAR(10) NOT NULL,
returned_damaged_yn VARCHAR(40),
booking_start_date DATETIME,
booking_end_date DATETIME,
count_hired VARCHAR(40),
amount_payable DECIMAL(19,4),
amount_of_discount DECIMAL(19,4),
amount_outstanding DECIMAL(19,4),
amount_of_refund DECIMAL(19,4),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);



















CREATE TABLE Products_for_Hire (
product_id INTEGER PRIMARY KEY,
product_type_code VARCHAR(15) NOT NULL,
daily_hire_cost DECIMAL(19,4),
product_name VARCHAR(80),
product_description VARCHAR(255)
);








CREATE TABLE Payments (
payment_id INTEGER PRIMARY KEY,
booking_id INTEGER,
customer_id INTEGER NOT NULL,
payment_type_code VARCHAR(15) NOT NULL,
amount_paid_in_full_yn VARCHAR(1),
payment_date DATETIME,
amount_due DECIMAL(19,4),
amount_paid DECIMAL(19,4),
FOREIGN KEY (booking_id ) REFERENCES Bookings(booking_id ),
FOREIGN KEY (customer_id ) REFERENCES Customers(customer_id )
);

















CREATE TABLE Products_Booked (
booking_id INTEGER NOT NULL,
product_id INTEGER NOT NULL,
returned_yn VARCHAR(1),
returned_late_yn VARCHAR(1),
booked_count INTEGER,
booked_amount FLOAT NULL,
PRIMARY KEY (booking_id, product_id)
FOREIGN KEY (booking_id ) REFERENCES Bookings(booking_id ),
FOREIGN KEY (product_id ) REFERENCES Products_for_Hire(product_id )
);

CREATE TABLE View_Product_Availability (
product_id INTEGER NOT NULL,
booking_id INTEGER NOT NULL,
status_date DATETIME PRIMARY KEY,
available_yn VARCHAR(1),
FOREIGN KEY (booking_id ) REFERENCES Bookings(booking_id ),
FOREIGN KEY (product_id ) REFERENCES Products_for_Hire(product_id )
);

















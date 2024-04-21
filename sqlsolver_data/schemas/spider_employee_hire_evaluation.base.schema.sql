CREATE TABLE employee (
Employee_ID INT,
Name VARCHAR(255),
Age INT,
City VARCHAR(255),
PRIMARY KEY (Employee_ID)
);

CREATE TABLE shop (
Shop_ID INT,
Name VARCHAR(255),
Location VARCHAR(255),
District VARCHAR(255),
Number_products INT,
Manager_name VARCHAR(255),
PRIMARY KEY (Shop_ID)
);

CREATE TABLE hiring (
Shop_ID INT,
Employee_ID INT,
Start_from VARCHAR(255),
Is_full_time BOOLEAN,
PRIMARY KEY (Employee_ID),
FOREIGN KEY (Shop_ID) REFERENCES shop(Shop_ID),
FOREIGN KEY (Employee_ID) REFERENCES employee(Employee_ID)
);

CREATE TABLE evaluation (
Employee_ID INT,
Year_awarded VARCHAR(255),
Bonus REAL,
PRIMARY KEY (Employee_ID,Year_awarded),
FOREIGN KEY (Employee_ID) REFERENCES employee(Employee_ID)
);

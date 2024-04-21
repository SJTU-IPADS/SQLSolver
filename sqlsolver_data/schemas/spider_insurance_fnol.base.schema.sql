CREATE TABLE Customers (
Customer_ID INTEGER NOT NULL,
Customer_name VARCHAR(40),
PRIMARY KEY (Customer_ID)
);

CREATE TABLE Services (
Service_ID INTEGER NOT NULL,
Service_name VARCHAR(40),
PRIMARY KEY (Service_ID)
);






CREATE TABLE Available_Policies (
Policy_ID INTEGER NOT NULL,
policy_type_code CHAR(15),
Customer_Phone VARCHAR(255),
PRIMARY KEY (Policy_ID),
UNIQUE (Policy_ID)
);
CREATE TABLE Customers_Policies (
Customer_ID INTEGER NOT NULL,
Policy_ID INTEGER NOT NULL,
Date_Opened DATE,
Date_Closed DATE,
PRIMARY KEY (Customer_ID, Policy_ID),
FOREIGN KEY (Customer_ID) REFERENCES Customers (Customer_ID),
FOREIGN KEY (Policy_ID) REFERENCES Available_Policies (Policy_ID)
);

CREATE TABLE First_Notification_of_Loss (
FNOL_ID INTEGER NOT NULL,
Customer_ID INTEGER NOT NULL,
Policy_ID INTEGER NOT NULL,
Service_ID INTEGER NOT NULL,
PRIMARY KEY (FNOL_ID),
UNIQUE (FNOL_ID),
FOREIGN KEY (Service_ID) REFERENCES Services (Service_ID),
FOREIGN KEY (Customer_ID, Policy_ID) REFERENCES Customers_Policies (Customer_ID,Policy_ID)
);

CREATE TABLE Claims (
Claim_ID INTEGER NOT NULL,
FNOL_ID INTEGER NOT NULL,
Effective_Date DATE,
PRIMARY KEY (Claim_ID),
UNIQUE (Claim_ID),
FOREIGN KEY (FNOL_ID) REFERENCES First_Notification_of_Loss (FNOL_ID)
);
CREATE TABLE Settlements (
Settlement_ID INTEGER NOT NULL,
Claim_ID INTEGER,
Effective_Date DATE,
Settlement_Amount REAL,
PRIMARY KEY (Settlement_ID),
UNIQUE (Settlement_ID),
FOREIGN KEY (Claim_ID) REFERENCES Claims (Claim_ID)
);


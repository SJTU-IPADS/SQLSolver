
CREATE TABLE Customers (
Customer_ID INTEGER NOT NULL,
Customer_Details VARCHAR(255) NOT NULL,
PRIMARY KEY (Customer_ID)
);
















CREATE TABLE Customer_Policies (
Policy_ID INTEGER NOT NULL,
Customer_ID INTEGER NOT NULL,
Policy_Type_Code CHAR(15) NOT NULL,
Start_Date DATE,
End_Date DATE,
PRIMARY KEY (Policy_ID),
FOREIGN KEY (Customer_ID) REFERENCES Customers (Customer_ID)
);
















CREATE TABLE Claims (
Claim_ID INTEGER NOT NULL,
Policy_ID INTEGER NOT NULL,
Date_Claim_Made DATE,
Date_Claim_Settled DATE,
Amount_Claimed INTEGER,
Amount_Settled INTEGER,
PRIMARY KEY (Claim_ID),
FOREIGN KEY (Policy_ID) REFERENCES Customer_Policies (Policy_ID)
);



















CREATE TABLE Settlements (
Settlement_ID INTEGER NOT NULL,
Claim_ID INTEGER NOT NULL,
Date_Claim_Made DATE,
Date_Claim_Settled DATE,
Amount_Claimed INTEGER,
Amount_Settled INTEGER,
Customer_Policy_ID INTEGER NOT NULL,
PRIMARY KEY (Settlement_ID),
FOREIGN KEY (Claim_ID) REFERENCES Claims (Claim_ID)
);
CREATE TABLE Payments (
Payment_ID INTEGER NOT NULL,
Settlement_ID INTEGER NOT NULL,
Payment_Method_Code VARCHAR(255),
Date_Payment_Made DATE,
Amount_Payment INTEGER,
PRIMARY KEY (Payment_ID),
FOREIGN KEY (Settlement_ID) REFERENCES Settlements (Settlement_ID)
);



































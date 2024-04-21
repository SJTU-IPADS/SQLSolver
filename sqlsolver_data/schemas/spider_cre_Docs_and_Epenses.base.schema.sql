

CREATE TABLE Ref_Document_Types (
Document_Type_Code CHAR(15) NOT NULL,
Document_Type_Name VARCHAR(255) NOT NULL,
Document_Type_Description VARCHAR(255) NOT NULL,
PRIMARY KEY (Document_Type_Code)
);
CREATE TABLE Ref_Budget_Codes (
Budget_Type_Code CHAR(15) NOT NULL,
Budget_Type_Description VARCHAR(255) NOT NULL,
PRIMARY KEY (Budget_Type_Code)
);










CREATE TABLE Projects (
Project_ID INTEGER NOT NULL,
Project_Details VARCHAR(255),
PRIMARY KEY (Project_ID)
);







CREATE TABLE Documents (
Document_ID INTEGER NOT NULL,
Document_Type_Code CHAR(15) NOT NULL,
Project_ID INTEGER NOT NULL,
Document_Date DATETIME,
Document_Name VARCHAR(255),
Document_Description VARCHAR(255),
Other_Details VARCHAR(255),
PRIMARY KEY (Document_ID),
FOREIGN KEY (Document_Type_Code) REFERENCES Ref_Document_Types (Document_Type_Code),
FOREIGN KEY (Project_ID) REFERENCES Projects (Project_ID)
);

















CREATE TABLE Statements (
Statement_ID INTEGER NOT NULL,
Statement_Details VARCHAR(255),
PRIMARY KEY (Statement_ID),
FOREIGN KEY (Statement_ID) REFERENCES Documents (Document_ID)
);





CREATE TABLE Documents_with_Expenses (
Document_ID INTEGER NOT NULL,
Budget_Type_Code CHAR(15) NOT NULL,
Document_Details VARCHAR(255),
PRIMARY KEY (Document_ID),
FOREIGN KEY (Budget_Type_Code) REFERENCES Ref_Budget_Codes (Budget_Type_Code),
FOREIGN KEY (Document_ID) REFERENCES Documents (Document_ID)
);












CREATE TABLE Accounts (
Account_ID INTEGER NOT NULL,
Statement_ID INTEGER NOT NULL,
Account_Details VARCHAR(255),
PRIMARY KEY (Account_ID),
FOREIGN KEY (Statement_ID) REFERENCES Statements (Statement_ID)
);
















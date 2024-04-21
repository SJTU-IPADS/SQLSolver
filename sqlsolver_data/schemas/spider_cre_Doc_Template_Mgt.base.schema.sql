CREATE TABLE Ref_Template_Types (
Template_Type_Code CHAR(15) NOT NULL,
Template_Type_Description VARCHAR(255) NOT NULL,
PRIMARY KEY (Template_Type_Code)
);
CREATE TABLE Templates (
Template_ID INTEGER NOT NULL,
Version_Number INTEGER NOT NULL,
Template_Type_Code CHAR(15) NOT NULL,
Date_Effective_From DATETIME,
Date_Effective_To DATETIME,
Template_Details VARCHAR(255) NOT NULL,
PRIMARY KEY (Template_ID),
FOREIGN KEY (Template_Type_Code) REFERENCES Ref_Template_Types (Template_Type_Code)
);
CREATE TABLE Documents (
Document_ID INTEGER NOT NULL,
Template_ID INTEGER,
Document_Name VARCHAR(255),
Document_Description VARCHAR(255),
Other_Details VARCHAR(255),
PRIMARY KEY (Document_ID),
FOREIGN KEY (Template_ID) REFERENCES Templates (Template_ID)
);
CREATE TABLE Paragraphs (
Paragraph_ID INTEGER NOT NULL,
Document_ID INTEGER NOT NULL,
Paragraph_Text VARCHAR(255),
Other_Details VARCHAR(255),
PRIMARY KEY (Paragraph_ID),
FOREIGN KEY (Document_ID) REFERENCES Documents (Document_ID)
);




CREATE TABLE Addresses (
Address_ID INTEGER NOT NULL ,
address_details VARCHAR(255),
PRIMARY KEY (Address_ID),
UNIQUE (Address_ID)
);
CREATE TABLE Locations (
Location_ID INTEGER NOT NULL ,
Other_Details VARCHAR(255),
PRIMARY KEY (Location_ID)
);
CREATE TABLE Products (
Product_ID INTEGER NOT NULL,
Product_Type_Code CHAR(15),
Product_Name VARCHAR(255),
Product_Price DECIMAL(20,4),
PRIMARY KEY (Product_ID),
UNIQUE (Product_ID)
);
CREATE TABLE Parties (
Party_ID INTEGER NOT NULL,
Party_Details VARCHAR(255),
PRIMARY KEY (Party_ID)
);
CREATE TABLE Assets (
Asset_ID INTEGER NOT NULL ,
Other_Details VARCHAR(255),
PRIMARY KEY (Asset_ID)
);
CREATE TABLE Channels (
Channel_ID INTEGER NOT NULL ,
Other_Details VARCHAR(255),
PRIMARY KEY (Channel_ID)
);
CREATE TABLE Finances (
Finance_ID INTEGER NOT NULL ,
Other_Details VARCHAR(255),
PRIMARY KEY (Finance_ID)
);



West Mafalda, CO 23309');

Port Delbert, OK 66249');

Bogisichland, VT 71460');

East Marionfort, VT 89477-0433');

Luellamouth, MT 67912');

O\'Reillychester, CA 92522-9526');

Chelsealand, NE 22947-6129');

Jaydenfurt, NE 85011-5059');

Gaylordtown, VT 05705');

Lake Immanuel, UT 01388');

Haagberg, AK 41204-1496');

East Beauview, LA 19968-4755');

North Scottymouth, IN 85224-1392');

Ricechester, DC 70816-9058');

West Colemanburgh, MO 87777');












































































CREATE TABLE Events (
Event_ID INTEGER NOT NULL ,
Address_ID INTEGER,
Channel_ID INTEGER NOT NULL,
Event_Type_Code CHAR(15),
Finance_ID INTEGER NOT NULL,
Location_ID INTEGER NOT NULL,
PRIMARY KEY (Event_ID),
UNIQUE (Event_ID),
FOREIGN KEY (Location_ID) REFERENCES Locations (Location_ID),
FOREIGN KEY (Address_ID) REFERENCES Addresses (Address_ID),
FOREIGN KEY (Finance_ID) REFERENCES Finances (Finance_ID)
);
















CREATE TABLE Products_in_Events (
Product_in_Event_ID INTEGER NOT NULL,
Event_ID INTEGER NOT NULL,
Product_ID INTEGER NOT NULL,
PRIMARY KEY (Product_in_Event_ID),
FOREIGN KEY (Event_ID) REFERENCES Events (Event_ID),
FOREIGN KEY (Product_ID) REFERENCES Products (Product_ID)
);

















CREATE TABLE Parties_in_Events (
Party_ID INTEGER NOT NULL,
Event_ID INTEGER NOT NULL,
Role_Code CHAR(15),
PRIMARY KEY (Party_ID, Event_ID),
FOREIGN KEY (Party_ID) REFERENCES Parties (Party_ID),
FOREIGN KEY (Event_ID) REFERENCES Events (Event_ID)
);
















CREATE TABLE Agreements (
Document_ID INTEGER NOT NULL ,
Event_ID INTEGER NOT NULL,
PRIMARY KEY (Document_ID),
FOREIGN KEY (Event_ID) REFERENCES Events (Event_ID)
);
















CREATE TABLE Assets_in_Events (
Asset_ID INTEGER NOT NULL,
Event_ID INTEGER NOT NULL,
PRIMARY KEY (Asset_ID, Event_ID),
FOREIGN KEY (Event_ID) REFERENCES Events (Event_ID),
FOREIGN KEY (Event_ID) REFERENCES Events (Event_ID)
);















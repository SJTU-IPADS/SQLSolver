
CREATE TABLE Ref_Characteristic_Types (
characteristic_type_code VARCHAR(15) PRIMARY KEY,
characteristic_type_description VARCHAR(80)
);
CREATE TABLE Ref_Colors (
color_code VARCHAR(15) PRIMARY KEY,
color_description VARCHAR(80)
);
CREATE TABLE Ref_Product_Categories (
product_category_code VARCHAR(15) PRIMARY KEY,
product_category_description VARCHAR(80),
unit_of_measure VARCHAR(20)
);














CREATE TABLE Characteristics (
characteristic_id INTEGER PRIMARY KEY,
characteristic_type_code VARCHAR(15) NOT NULL,
characteristic_data_type VARCHAR(10),
characteristic_name VARCHAR(80),
other_characteristic_details VARCHAR(255),
FOREIGN KEY (characteristic_type_code ) REFERENCES Ref_Characteristic_Types(characteristic_type_code )
);

















CREATE TABLE Products (
product_id INTEGER PRIMARY KEY,
color_code VARCHAR(15) NOT NULL,
product_category_code VARCHAR(15) NOT NULL,
product_name VARCHAR(80),
typical_buying_price VARCHAR(20),
typical_selling_price VARCHAR(20),
product_description VARCHAR(255),
other_product_details VARCHAR(255),
FOREIGN KEY (product_category_code ) REFERENCES Ref_Product_Categories(product_category_code ),FOREIGN KEY (color_code ) REFERENCES Ref_Colors(color_code )
);

















CREATE TABLE Product_Characteristics (
product_id INTEGER NOT NULL,
characteristic_id INTEGER NOT NULL,
product_characteristic_value VARCHAR(50),
FOREIGN KEY (characteristic_id ) REFERENCES Characteristics(characteristic_id ),
FOREIGN KEY (product_id ) REFERENCES Products(product_id )
);
















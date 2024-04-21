

CREATE TABLE Customer_Master_Index (
master_customer_id INTEGER NOT NULL,
cmi_details VARCHAR(255),
PRIMARY KEY (master_customer_id)
);










CREATE TABLE CMI_Cross_References (
cmi_cross_ref_id INTEGER NOT NULL,
master_customer_id INTEGER NOT NULL,
source_system_code CHAR(15) NOT NULL,
PRIMARY KEY (cmi_cross_ref_id),
FOREIGN KEY (master_customer_id) REFERENCES Customer_Master_Index (master_customer_id)

);






















CREATE TABLE Council_Tax (
council_tax_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (council_tax_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);
CREATE TABLE Business_Rates (
business_rates_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (business_rates_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);
CREATE TABLE Benefits_Overpayments (
council_tax_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (council_tax_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);
CREATE TABLE Parking_Fines (
council_tax_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (council_tax_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);
CREATE TABLE Rent_Arrears (
council_tax_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (council_tax_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);
CREATE TABLE Electoral_Register (
electoral_register_id INTEGER NOT NULL,
cmi_cross_ref_id INTEGER NOT NULL,
PRIMARY KEY (electoral_register_id),
FOREIGN KEY (cmi_cross_ref_id) REFERENCES CMI_Cross_References (cmi_cross_ref_id)
);






























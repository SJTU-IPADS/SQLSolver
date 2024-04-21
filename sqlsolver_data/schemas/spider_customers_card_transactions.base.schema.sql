

CREATE TABLE Accounts (
account_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
account_name VARCHAR(50),
other_account_details VARCHAR(255)
);
CREATE TABLE Customers (
customer_id INTEGER PRIMARY KEY,
customer_first_name VARCHAR(20),
customer_last_name VARCHAR(20),
customer_address VARCHAR(255),
customer_phone VARCHAR(255),
customer_email VARCHAR(255),
other_customer_details VARCHAR(255)
);
CREATE TABLE Customers_Cards (
card_id INTEGER PRIMARY KEY,
customer_id INTEGER NOT NULL,
card_type_code VARCHAR(15) NOT NULL,
card_number VARCHAR(80),
date_valid_from DATETIME,
date_valid_to DATETIME,
other_card_details VARCHAR(255)
);
CREATE TABLE Financial_Transactions (
transaction_id INTEGER NOT NULL ,
previous_transaction_id INTEGER,
account_id INTEGER NOT NULL,
card_id INTEGER NOT NULL,
transaction_type VARCHAR(15) NOT NULL,
transaction_date DATETIME,
transaction_amount DOUBLE NULL,
transaction_comment VARCHAR(255),
other_transaction_details VARCHAR(255),
FOREIGN KEY (card_id ) REFERENCES Customers_Cards(card_id ),
FOREIGN KEY (account_id ) REFERENCES Accounts(account_id )
);

















Lake Brody, VT 57078', '(673)872-5338', 'fahey.dorian@example.com', NULL);

Schimmelmouth, VT 96364-4898', '679-845-8645x94312', 'idickinson@example.com', NULL);

Port Lilla, LA 44867', '1-511-656-6664', 'nichole.rodriguez@example.com', NULL);

New Elbert, DE 86980-8517', '941-213-6716x675', 'enrique59@example.com', NULL);

Unaview, SC 86336-3287', '224-123-1012', 'dauer@example.net', NULL);

Coleberg, FL 69194-5357', '1-564-044-3909', 'ebert.omer@example.net', NULL);

Schmidtmouth, NH 15794', '751.049.9948', 'kling.catalina@example.com', NULL);

Webstertown, KY 91980-4004', '+12(6)9024410984', 'dell13@example.com', NULL);

Reynoldsfurt, NM 94584-3767', '284.749.0453', 'ahomenick@example.org', NULL);

East Alisonville, NH 14890', '+90(8)1290735932', 'kyra.murazik@example.org', NULL);

South Johnfort, SD 67577-9504', '1-207-977-5182', 'keegan16@example.com', NULL);

North Laurenland, KY 46376', '(415)237-0701x3115', 'grady.general@example.org', NULL);

Torphyberg, OK 34312-0380', '1-894-567-2283', 'schaden.katrina@example.net', NULL);

Jocelynfurt, OH 59023-2787', '(703)950-4708x8972', 'huels.antonina@example.com', NULL);

New Ceciltown, AL 64723-5646', '246-469-4472x359', 'earlene.carroll@example.net', NULL);































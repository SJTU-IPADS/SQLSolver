

CREATE TABLE Apartment_Buildings (
building_id INTEGER NOT NULL,
building_short_name CHAR(15),
building_full_name VARCHAR(80),
building_description VARCHAR(255),
building_address VARCHAR(255),
building_manager VARCHAR(50),
building_phone VARCHAR(80),
PRIMARY KEY (building_id),
UNIQUE (building_id)
);


Marquiseberg, CA 70496', 'Emma', '(948)040-1064x387');

Charliefort, VT 71664', 'Brenden', '915-617-2408x832');

Wisozkburgh, AL 08256', 'Melyssa', '(609)946-0491');

West Efrainburgh, DE 40074', 'Kathlyn', '681.772.2454');

Mohrland, AL 56839-5028', 'Kyle', '1-724-982-9507x640');

Ahmedberg, WI 48788', 'Albert', '376-017-3538');

East Ottis, ND 73970', 'Darlene', '1-224-619-0295x13195');

New Korbinmouth, KS 88726-1376', 'Marie', '(145)411-6406');

West Whitney, ID 66511', 'Ewald', '(909)086-5221x3455');

Leuschkeland, OK 12009-8683', 'Rogers', '1-326-267-3386x613');

Port Luz, VA 29660-6703', 'Olaf', '(480)480-7401');

Myrnatown, CT 13528', 'Claude', '1-667-728-2287x3158');

Sipesview, DE 69053', 'Sydni', '544-148-5565x2847');

Daytonland, ID 88081-3991', 'Juvenal', '318-398-8140');

Gerholdland, ID 23342', 'Holly', '1-605-511-1973x25011');

CREATE TABLE Apartments (
apt_id INTEGER NOT NULL ,
building_id INTEGER NOT NULL,
apt_type_code CHAR(15),
apt_number CHAR(10),
bathroom_count INTEGER,
bedroom_count INTEGER,
room_count CHAR(5),
PRIMARY KEY (apt_id),
UNIQUE (apt_id),
FOREIGN KEY (building_id) REFERENCES Apartment_Buildings (building_id)
);
















CREATE TABLE Apartment_Facilities (
apt_id INTEGER NOT NULL,
facility_code CHAR(15) NOT NULL,
PRIMARY KEY (apt_id, facility_code),
FOREIGN KEY (apt_id) REFERENCES Apartments (apt_id)
);







CREATE TABLE Guests (
guest_id INTEGER NOT NULL ,
gender_code CHAR(1),
guest_first_name VARCHAR(80),
guest_last_name VARCHAR(80),
date_of_birth DATETIME,
PRIMARY KEY (guest_id),
UNIQUE (guest_id)
);


















CREATE TABLE Apartment_Bookings (
apt_booking_id INTEGER NOT NULL,
apt_id INTEGER,
guest_id INTEGER NOT NULL,
booking_status_code CHAR(15) NOT NULL,
booking_start_date DATETIME,
booking_end_date DATETIME,
PRIMARY KEY (apt_booking_id),
UNIQUE (apt_booking_id),
FOREIGN KEY (apt_id) REFERENCES Apartments (apt_id),
FOREIGN KEY (guest_id) REFERENCES Guests (guest_id)
);
CREATE TABLE View_Unit_Status (
apt_id INTEGER,
apt_booking_id INTEGER,
status_date DATETIME NOT NULL,
available_yn BIT,
PRIMARY KEY (status_date),
FOREIGN KEY (apt_id) REFERENCES Apartments (apt_id),
FOREIGN KEY (apt_booking_id) REFERENCES Apartment_Bookings (apt_booking_id)
);





































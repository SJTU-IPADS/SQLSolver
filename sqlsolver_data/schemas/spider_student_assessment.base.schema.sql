CREATE TABLE Addresses (
address_id INTEGER NOT NULL,
line_1 VARCHAR(80),
line_2 VARCHAR(80),
city VARCHAR(50),
zip_postcode CHAR(20),
state_province_county VARCHAR(50),
country VARCHAR(50),
PRIMARY KEY (address_id)
);

CREATE TABLE People (
person_id INTEGER NOT NULL,
first_name VARCHAR(255),
middle_name VARCHAR(255),
last_name VARCHAR(255),
cell_mobile_number VARCHAR(40),
email_address VARCHAR(40),
login_name VARCHAR(40),
password VARCHAR(40),
PRIMARY KEY (person_id)
);











CREATE TABLE Students (
student_id INTEGER NOT NULL,
student_details VARCHAR(255),
PRIMARY KEY (student_id),
FOREIGN KEY (student_id) REFERENCES People (person_id)
);










CREATE TABLE Courses (
course_id VARCHAR(100) NOT NULL,
course_name VARCHAR(120),
course_description VARCHAR(255),
other_details VARCHAR(255),
PRIMARY KEY (course_id)
);









CREATE TABLE People_Addresses (
person_address_id INTEGER NOT NULL,
person_id INTEGER NOT NULL,
address_id INTEGER NOT NULL,
date_from DATETIME,
date_to DATETIME,
PRIMARY KEY (person_address_id),
FOREIGN KEY (person_id) REFERENCES People (person_id),
FOREIGN KEY (address_id) REFERENCES Addresses (address_id)
);










CREATE TABLE Student_Course_Registrations (
student_id INTEGER NOT NULL,
course_id INTEGER NOT NULL,
registration_date DATETIME NOT NULL,
PRIMARY KEY (student_id, course_id),
FOREIGN KEY (student_id) REFERENCES Students (student_id),
FOREIGN KEY (course_id) REFERENCES Courses (course_id)
);












CREATE TABLE Student_Course_Attendance (
student_id INTEGER NOT NULL,
course_id INTEGER NOT NULL,
date_of_attendance DATETIME NOT NULL,
PRIMARY KEY (student_id, course_id),
FOREIGN KEY (student_id, course_id) REFERENCES Student_Course_Registrations (student_id,course_id)
);











CREATE TABLE Candidates (
candidate_id INTEGER NOT NULL ,
candidate_details VARCHAR(255),
PRIMARY KEY (candidate_id),
FOREIGN KEY (candidate_id) REFERENCES People (person_id)
);
CREATE TABLE Candidate_Assessments (
candidate_id INTEGER NOT NULL,
qualification CHAR(15) NOT NULL,
assessment_date DATETIME NOT NULL,
asessment_outcome_code CHAR(15) NOT NULL,
PRIMARY KEY (candidate_id, qualification),
FOREIGN KEY (candidate_id) REFERENCES Candidates (candidate_id)
);
















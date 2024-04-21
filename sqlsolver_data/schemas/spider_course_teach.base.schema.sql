CREATE TABLE course (
Course_ID INT,
Staring_Date VARCHAR(255),
Course VARCHAR(255),
PRIMARY KEY (Course_ID)
);

CREATE TABLE teacher (
Teacher_ID INT,
Name VARCHAR(255),
Age VARCHAR(255),
Hometown VARCHAR(255),
PRIMARY KEY (Teacher_ID)
);

CREATE TABLE course_arrange (
Course_ID INT,
Teacher_ID INT,
Grade INT,
PRIMARY KEY (Course_ID,Teacher_ID,Grade),
FOREIGN KEY (Course_ID) REFERENCES course(Course_ID),
FOREIGN KEY (Teacher_ID) REFERENCES teacher(Teacher_ID)
);
CREATE TABLE Highschooler(
ID INT PRIMARY KEY,
name VARCHAR(255),
grade INT
);

CREATE TABLE Friend(
student_id INT,
friend_id INT,
PRIMARY KEY (student_id,friend_id),
FOREIGN KEY(student_id) REFERENCES Highschooler(ID),
FOREIGN KEY (friend_id) REFERENCES Highschooler(ID)
);

CREATE TABLE Likes(
student_id INT,
liked_id INT,
PRIMARY KEY (student_id, liked_id),
FOREIGN KEY (liked_id) REFERENCES Highschooler(ID),
FOREIGN KEY (student_id) REFERENCES Highschooler(ID)
);
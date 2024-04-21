create table Student (
        StuID        INTEGER PRIMARY KEY,
        LName        VARCHAR(12),
        Fname        VARCHAR(12),
        Age      INTEGER,
        Sex      VARCHAR(1),
        Major        INTEGER,
        Advisor      INTEGER,
        city_code    VARCHAR(3)
 );

create table Faculty (
       FacID 	       INTEGER PRIMARY KEY,
       Lname		VARCHAR(15),
       Fname		VARCHAR(15),
       Rank		VARCHAR(15),
       Sex		VARCHAR(1),
       Phone		INTEGER,
       Room		VARCHAR(5),
       Building		VARCHAR(13)
);

create table Department (
       DNO   		INTEGER PRIMARY KEY,
       Division		VARCHAR(2),
       DName		VARCHAR(25),
       Room		VARCHAR(5),
       Building		VARCHAR(13),
       DPhone		INTEGER
);

create table Member_of (
       FacID 	       INTEGER,
       DNO	       INTEGER,
       Appt_Type       VARCHAR(15),
       FOREIGN KEY(FacID) REFERENCES Faculty(FacID),
       FOREIGN KEY(DNO) REFERENCES Department(DNO)
);

create table Course (
       CID   	    	VARCHAR(7) PRIMARY KEY,
       CName		VARCHAR(40),
       Credits		INTEGER,
       Instructor	INTEGER,
       Days		VARCHAR(5),
       Hours		VARCHAR(11),
       DNO		INTEGER,
       FOREIGN KEY(Instructor) REFERENCES Faculty(FacID),
       FOREIGN KEY(DNO) REFERENCES Department(DNO)
);

create table Minor_in (
       StuID 	      INTEGER,
       DNO		INTEGER,
       FOREIGN KEY(StuID) REFERENCES Student(StuID),
       FOREIGN KEY(DNO) REFERENCES Department(DNO)
);

create table Enrolled_in (
       StuID 		 INTEGER,
       CID		VARCHAR(7),
       Grade		VARCHAR(2),
       FOREIGN KEY(StuID) REFERENCES Student(StuID),
       FOREIGN KEY(CID) REFERENCES Course(CID),
       FOREIGN KEY(Grade) REFERENCES Gradeconversion(lettergrade)
);

create table Gradeconversion (
       lettergrade	     VARCHAR(2) PRIMARY KEY,
       gradepoint	     FLOAT
);

insert into Minor_in values ( 1004, 520);
insert into Minor_in values ( 1005, 550);
insert into Minor_in values ( 1006, 050);
insert into Minor_in values ( 1007, 520);
insert into Minor_in values ( 1008, 550);
insert into Minor_in values ( 1014, 090);
insert into Minor_in values ( 1015, 140);
insert into Minor_in values ( 1016, 190);
insert into Minor_in values ( 1027, 530);
insert into Minor_in values ( 1031, 540);


insert into Enrolled_in values ( 1001, '550.681', 'A-');
insert into Enrolled_in values ( 1001, '600.303', 'B');
insert into Enrolled_in values ( 1001, '600.315', 'B+');
insert into Enrolled_in values ( 1001, '600.337', 'A');
insert into Enrolled_in values ( 1001, '600.461', 'B-');
insert into Enrolled_in values ( 1001, '600.465', 'B');
insert into Enrolled_in values ( 1002, '520.213', 'B+');
insert into Enrolled_in values ( 1002, '600.211', 'C');
insert into Enrolled_in values ( 1002, '600.303', 'C+');
insert into Enrolled_in values ( 1002, '600.337', 'A');
insert into Enrolled_in values ( 1002, '600.463', 'B');
insert into Enrolled_in values ( 1002, '600.465', 'B+');
insert into Enrolled_in values ( 1003, '600.333', 'B');
insert into Enrolled_in values ( 1003, '600.337', 'B');
insert into Enrolled_in values ( 1003, '600.415', 'B');
insert into Enrolled_in values ( 1003, '600.461', 'B+');
insert into Enrolled_in values ( 1003, '600.465', 'B');
insert into Enrolled_in values ( 1004, '600.303', 'C-');
insert into Enrolled_in values ( 1004, '600.415', 'C-');
insert into Enrolled_in values ( 1004, '600.437', 'C-');
insert into Enrolled_in values ( 1004, '600.445', 'A-');
insert into Enrolled_in values ( 1004, '600.461', 'C');
insert into Enrolled_in values ( 1004, '600.463', 'A+');
insert into Enrolled_in values ( 1004, '600.465', 'A');
insert into Enrolled_in values ( 1005, '600.103', 'A');
insert into Enrolled_in values ( 1005, '600.107', 'C+');
insert into Enrolled_in values ( 1005, '600.113', 'C');
insert into Enrolled_in values ( 1005, '600.227', 'A');
insert into Enrolled_in values ( 1005, '600.303', 'B');
insert into Enrolled_in values ( 1006, '550.420', 'B');
insert into Enrolled_in values ( 1006, '600.107', 'B+');
insert into Enrolled_in values ( 1006, '600.227', 'B-');
insert into Enrolled_in values ( 1006, '600.232', 'C-');
insert into Enrolled_in values ( 1006, '600.303', 'A-');
insert into Enrolled_in values ( 1006, '600.315', 'A');
insert into Enrolled_in values ( 1007, '550.420', 'A');
insert into Enrolled_in values ( 1007, '600.113', 'A-');
insert into Enrolled_in values ( 1007, '600.227', 'C+');
insert into Enrolled_in values ( 1007, '600.315', 'A');
insert into Enrolled_in values ( 1007, '600.333', 'A-');
insert into Enrolled_in values ( 1007, '600.337', 'C');
insert into Enrolled_in values ( 1008, '600.415', 'A+');
insert into Enrolled_in values ( 1008, '600.463', 'B');
insert into Enrolled_in values ( 1008, '600.465', 'B');
insert into Enrolled_in values ( 1008, '600.657', 'B');
insert into Enrolled_in values ( 1008, '600.787', 'B');
insert into Enrolled_in values ( 1009, '550.413', 'B+');
insert into Enrolled_in values ( 1009, '550.471', 'C');
insert into Enrolled_in values ( 1009, '550.620', 'A-');
insert into Enrolled_in values ( 1009, '550.626', 'B');
insert into Enrolled_in values ( 1009, '550.671', 'C');
insert into Enrolled_in values ( 1009, '550.681', 'A');
insert into Enrolled_in values ( 1009, '550.661', 'B-');
insert into Enrolled_in values ( 1009, '550.631', 'A-');
insert into Enrolled_in values ( 1010, '550.291', 'A');
insert into Enrolled_in values ( 1010, '550.310', 'A');
insert into Enrolled_in values ( 1010, '550.413', 'C+');
insert into Enrolled_in values ( 1010, '550.420', 'A');
insert into Enrolled_in values ( 1010, '550.471', 'A');
insert into Enrolled_in values ( 1010, '600.107', 'B+');
insert into Enrolled_in values ( 1011, '520.213', 'B');
insert into Enrolled_in values ( 1011, '520.345', 'B');
insert into Enrolled_in values ( 1011, '520.349', 'A');
insert into Enrolled_in values ( 1011, '520.353', 'A-');
insert into Enrolled_in values ( 1011, '550.420', 'B');
insert into Enrolled_in values ( 1011, '600.415', 'B+');
insert into Enrolled_in values ( 1012, '050.109', 'B-');
insert into Enrolled_in values ( 1012, '050.203', 'B-');
insert into Enrolled_in values ( 1012, '050.325', 'A-');
insert into Enrolled_in values ( 1012, '600.107', 'A');
insert into Enrolled_in values ( 1012, '600.315', 'B');
insert into Enrolled_in values ( 1014, '600.107', 'A');
insert into Enrolled_in values ( 1014, '600.227', 'A');
insert into Enrolled_in values ( 1014, '600.232', 'A');
insert into Enrolled_in values ( 1014, '600.315', 'A+');
insert into Enrolled_in values ( 1014, '600.445', 'B');
insert into Enrolled_in values ( 1014, '600.461', 'B');
insert into Enrolled_in values ( 1014, '600.463', 'B');
insert into Enrolled_in values ( 1015, '550.420', 'A');
insert into Enrolled_in values ( 1015, '600.227', 'A+');
insert into Enrolled_in values ( 1015, '600.303', 'A');
insert into Enrolled_in values ( 1015, '600.315', 'C-');
insert into Enrolled_in values ( 1015, '600.333', 'A');
insert into Enrolled_in values ( 1016, '050.109', 'B-');
insert into Enrolled_in values ( 1016, '050.203', 'D-');
insert into Enrolled_in values ( 1016, '050.325', 'A');
insert into Enrolled_in values ( 1016, '050.821', 'A');
insert into Enrolled_in values ( 1016, '550.420', 'A-');
insert into Enrolled_in values ( 1016, '600.107', 'B+');
insert into Enrolled_in values ( 1016, '600.315', 'B-');
insert into Enrolled_in values ( 1017, '050.427', 'B');
insert into Enrolled_in values ( 1017, '050.670', 'B');
insert into Enrolled_in values ( 1017, '050.802', 'C');
insert into Enrolled_in values ( 1017, '550.681', 'B');
insert into Enrolled_in values ( 1017, '600.109', 'A-');
insert into Enrolled_in values ( 1017, '600.461', 'A');
insert into Enrolled_in values ( 1017, '600.465', 'C');
insert into Enrolled_in values ( 1018, '520.213', 'A-');
insert into Enrolled_in values ( 1018, '600.211', 'A');
insert into Enrolled_in values ( 1018, '600.303', 'A');
insert into Enrolled_in values ( 1018, '600.337', 'C-');
insert into Enrolled_in values ( 1018, '600.463', 'B');
insert into Enrolled_in values ( 1018, '600.465', 'B');
insert into Enrolled_in values ( 1019, '600.103', 'B');
insert into Enrolled_in values ( 1019, '600.107', 'B');
insert into Enrolled_in values ( 1019, '600.113', 'D+');
insert into Enrolled_in values ( 1019, '600.227', 'A');
insert into Enrolled_in values ( 1019, '600.303', 'A');
insert into Enrolled_in values ( 1020, '600.333', 'A');
insert into Enrolled_in values ( 1020, '600.337', 'A');
insert into Enrolled_in values ( 1020, '600.415', 'A');
insert into Enrolled_in values ( 1020, '600.461', 'A');
insert into Enrolled_in values ( 1020, '600.465', 'A');
insert into Enrolled_in values ( 1021, '600.303', 'B-');
insert into Enrolled_in values ( 1021, '600.303', 'B');
insert into Enrolled_in values ( 1021, '600.415', 'B');
insert into Enrolled_in values ( 1021, '600.437', 'B');
insert into Enrolled_in values ( 1021, '600.437', 'B');
insert into Enrolled_in values ( 1021, '600.445', 'B-');
insert into Enrolled_in values ( 1021, '600.445', 'C');
insert into Enrolled_in values ( 1021, '600.463', 'A');
insert into Enrolled_in values ( 1021, '600.463', 'B');
insert into Enrolled_in values ( 1022, '550.420', 'B');
insert into Enrolled_in values ( 1022, '550.420', 'B+');
insert into Enrolled_in values ( 1022, '600.107', 'A');
insert into Enrolled_in values ( 1022, '600.227', 'A');
insert into Enrolled_in values ( 1022, '600.227', 'A');
insert into Enrolled_in values ( 1022, '600.232', 'B');
insert into Enrolled_in values ( 1022, '600.303', 'B');
insert into Enrolled_in values ( 1022, '600.315', 'D');
insert into Enrolled_in values ( 1022, '600.461', 'A');
insert into Enrolled_in values ( 1023, '600.113', 'A-');
insert into Enrolled_in values ( 1023, '600.315', 'B');
insert into Enrolled_in values ( 1023, '600.333', 'B');
insert into Enrolled_in values ( 1023, '600.337', 'B+');
insert into Enrolled_in values ( 1023, '600.463', 'A');
insert into Enrolled_in values ( 1023, '600.465', 'A');
insert into Enrolled_in values ( 1023, '600.657', 'B');
insert into Enrolled_in values ( 1023, '600.787', 'B');
insert into Enrolled_in values ( 1024, '550.291', 'B');
insert into Enrolled_in values ( 1024, '550.413', 'C');
insert into Enrolled_in values ( 1024, '550.471', 'A-');
insert into Enrolled_in values ( 1024, '550.620', 'A');
insert into Enrolled_in values ( 1024, '550.626', 'B');
insert into Enrolled_in values ( 1024, '550.671', 'B');
insert into Enrolled_in values ( 1024, '550.681', 'B');
insert into Enrolled_in values ( 1024, '600.415', 'B');
insert into Enrolled_in values ( 1025, '520.213', 'A');
insert into Enrolled_in values ( 1025, '520.345', 'A+');
insert into Enrolled_in values ( 1025, '550.310', 'A');
insert into Enrolled_in values ( 1025, '550.413', 'A');
insert into Enrolled_in values ( 1025, '550.420', 'C');
insert into Enrolled_in values ( 1025, '550.471', 'B');
insert into Enrolled_in values ( 1025, '600.107', 'B');
insert into Enrolled_in values ( 1026, '520.349', 'A');
insert into Enrolled_in values ( 1026, '520.353', 'A');
insert into Enrolled_in values ( 1026, '600.303', 'A');
insert into Enrolled_in values ( 1026, '600.437', 'A');
insert into Enrolled_in values ( 1026, '600.445', 'A');
insert into Enrolled_in values ( 1026, '600.463', 'B-');
insert into Enrolled_in values ( 1027, '600.107', 'B');
insert into Enrolled_in values ( 1027, '600.227', 'B');
insert into Enrolled_in values ( 1027, '600.232', 'B');
insert into Enrolled_in values ( 1027, '600.303', 'B');
insert into Enrolled_in values ( 1027, '600.315', 'B-');
insert into Enrolled_in values ( 1027, '600.461', 'B-');
insert into Enrolled_in values ( 1027, '600.463', 'B');
insert into Enrolled_in values ( 1028, '550.420', 'B+');
insert into Enrolled_in values ( 1028, '600.227', 'A');
insert into Enrolled_in values ( 1028, '600.315', 'A+');
insert into Enrolled_in values ( 1028, '600.333', 'A');
insert into Enrolled_in values ( 1028, '600.337', 'A+');
insert into Enrolled_in values ( 1029, '550.413', 'C-');
insert into Enrolled_in values ( 1029, '550.471', 'A');
insert into Enrolled_in values ( 1029, '550.620', 'B-');
insert into Enrolled_in values ( 1029, '550.671', 'A-');
insert into Enrolled_in values ( 1029, '600.113', 'B-');
insert into Enrolled_in values ( 1029, '600.463', 'A+');
insert into Enrolled_in values ( 1030, '520.345', 'B');
insert into Enrolled_in values ( 1030, '550.291', 'B');
insert into Enrolled_in values ( 1030, '550.310', 'B-');
insert into Enrolled_in values ( 1030, '550.413', 'B-');
insert into Enrolled_in values ( 1030, '550.420', 'B');
insert into Enrolled_in values ( 1030, '550.471', 'B+');
insert into Enrolled_in values ( 1030, '600.107', 'B');
insert into Enrolled_in values ( 1031, '520.213', 'B+');
insert into Enrolled_in values ( 1031, '520.349', 'B');
insert into Enrolled_in values ( 1031, '520.353', 'C');
insert into Enrolled_in values ( 1031, '600.437', 'A+');
insert into Enrolled_in values ( 1032, '550.420', 'A-');
insert into Enrolled_in values ( 1032, '550.420', 'D-');
insert into Enrolled_in values ( 1032, '600.232', 'A-');
insert into Enrolled_in values ( 1032, '600.303', 'A');
insert into Enrolled_in values ( 1032, '600.315', 'A');
insert into Enrolled_in values ( 1033, '600.113', 'A');
insert into Enrolled_in values ( 1033, '600.227', 'A');
insert into Enrolled_in values ( 1033, '600.315', 'A');
insert into Enrolled_in values ( 1033, '600.333', 'A');
insert into Enrolled_in values ( 1033, '600.337', 'B');
insert into Enrolled_in values ( 1034, '050.109', 'B+');
insert into Enrolled_in values ( 1034, '050.203', 'B');
insert into Enrolled_in values ( 1034, '050.325', 'B');
insert into Enrolled_in values ( 1034, '600.107', 'B+');
insert into Enrolled_in values ( 1034, '600.315', 'B');
insert into Enrolled_in values ( 1035, '050.381', 'B-');
insert into Enrolled_in values ( 1035, '050.427', 'A-');
insert into Enrolled_in values ( 1035, '050.670', 'B');
insert into Enrolled_in values ( 1035, '050.802', 'D');
insert into Enrolled_in values ( 1035, '050.821', 'A');
insert into Enrolled_in values ( 1035, '600.109', 'B-');


insert into Gradeconversion values ('A+', 4.0);
insert into Gradeconversion values ('A',  4.0);
insert into Gradeconversion values ('A-', 3.7);
insert into Gradeconversion values ('B+', 3.3);
insert into Gradeconversion values ('B',  3.0);
insert into Gradeconversion values ('B-', 2.7);
insert into Gradeconversion values ('C+', 2.3);
insert into Gradeconversion values ('C',  2.0);
insert into Gradeconversion values ('C-', 1.7);
insert into Gradeconversion values ('D+', 1.3);
insert into Gradeconversion values ('D',  1.0);
insert into Gradeconversion values ('D-', 0.7);
insert into Gradeconversion values ('F',  0.0);


insert into Faculty  values ( 1082, 'Giuliano', 'Mark', 'Instructor', 'M', 2424, '224', 'NEB');
insert into Faculty  values ( 1121, 'Goodrich', 'Michael', 'Professor', 'M', 3593, '219', 'NEB');
insert into Faculty  values ( 1148, 'Masson', 'Gerald', 'Professor', 'M', 3402, '224B', 'NEB');
insert into Faculty  values ( 1193, 'Jones', 'Stacey', 'Instructor', 'F', 3550, '224', 'NEB');
insert into Faculty  values ( 2192, 'Yarowsky', 'David', 'AsstProf', 'M', 6587, '324', 'NEB');
insert into Faculty  values ( 3457, 'Smith', 'Scott', 'AssocProf', 'M', 1035, '318', 'NEB');
insert into Faculty  values ( 4230, 'Houlahan', 'Joanne', 'Instructor', 'F', 1260, '328', 'NEB');
insert into Faculty  values ( 6112, 'Beach', 'Louis', 'Instructor', 'M', 1838, '207', 'NEB');
insert into Faculty  values ( 7712, 'Awerbuch', 'Baruch', 'Professor', 'M', 2105, '220', 'NEB');
insert into Faculty  values ( 7792, 'Brill', 'Eric', 'AsstProf', 'M', 2303, '324B', 'NEB');
insert into Faculty  values ( 7723, 'Taylor', 'Russell', 'Professor', 'M', 2435, '317', 'NEB');
insert into Faculty  values ( 8114, 'Angelopoulou', 'Ellie', 'Instructor', 'F', 2152, '316', 'NEB');
insert into Faculty  values ( 8423, 'Kumar', 'Subodh', 'AsstProf', 'M', 2522, '218', 'NEB');
insert into Faculty  values ( 8721, 'Wolff', 'Lawrence', 'AssocProf', 'M', 2342, '316', 'NEB');
insert into Faculty  values ( 8741, 'Salzberg', 'Steven', 'AssocProf', 'M', 2641,   '324A', 'NEB');
insert into Faculty  values ( 8918, 'Amir', 'Yair', 'AsstProf', 'M', 2672, '308', 'NEB');
insert into Faculty  values ( 9172, 'Kosaraju', 'Rao', 'Professor', 'M', 2757, '319', 'NEB');
insert into Faculty  values ( 9826, 'Delcher', 'Arthur', 'Instructor', 'M', 2956, '329', 'NEB');
insert into Faculty  values ( 1172, 'Runolfsson', 'Thordur', 'AssocProf', 'M', 3121, '119', 'Barton');
insert into Faculty  values ( 1177, 'Naiman', 'Daniel', 'Professor', 'M', 3571, '288', 'Krieger');
insert into Faculty  values ( 1823, 'Davidson', 'Frederic', 'Professor', 'M', 5629, '119', 'Barton');
insert into Faculty  values ( 2028, 'Brody', 'William', 'Professor', 'M', 6073, '119', 'Barton');
insert into Faculty  values ( 2119, 'Meyer', 'Gerard', 'Professor', 'M', 6350, '119', 'Barton');
insert into Faculty  values ( 2291, 'Scheinerman', 'Edward', 'Professor', 'M', 6654, '288', 'Krieger');
insert into Faculty  values ( 2311, 'Priebe', 'Carey', 'AsstProf', 'M', 6953, '288', 'Krieger');
insert into Faculty  values ( 2738, 'Fill', 'James', 'Professor', 'M', 8209, '288', 'Krieger');
insert into Faculty  values ( 2881, 'Goldman', 'Alan', 'Professor', 'M', 8335, '288', 'Krieger');
insert into Faculty  values ( 4432, 'Burzio', 'Luigi', 'Professor', 'M', 1813, '288', 'Krieger');
insert into Faculty  values ( 5718, 'Frank', 'Robert', 'AsstProf', 'M', 1751, '288', 'Krieger');
insert into Faculty  values ( 6182, 'Cheng', 'Cheng', 'AsstProf', 'M', 1856, '288', 'Krieger');
insert into Faculty  values ( 6191, 'Kaplan', 'Alexander', 'Professor', 'M', 1825, '119', 'Barton');
insert into Faculty  values ( 6330, 'Byrne', 'William', 'Instructor', 'M', 1691, '119', 'Barton');
insert into Faculty  values ( 6541, 'Han', 'Shih-Ping', 'Professor', 'M', 1914, '288', 'Krieger');
insert into Faculty  values ( 6910, 'Smolensky', 'Paul', 'Professor', 'M', 2072, '288', 'Krieger');
insert into Faculty  values ( 6925, 'Iglesias', 'Pablo', 'AsstProf', 'M', 2021, '119', 'Barton');
insert into Faculty  values ( 7134, 'Goutsias', 'John', 'Professor', 'M', 2184, '119', 'Barton');
insert into Faculty  values ( 7231, 'Rugh', 'Wilson', 'Professor', 'M', 2191, '119', 'Barton');
insert into Faculty  values ( 7271, 'Jelinek', 'Frederick', 'Professor', 'M', 2890, '119', 'Barton');
insert into Faculty  values ( 7506, 'Westgate', 'Charles', 'Professor', 'M', 2932, '119', 'Barton');
insert into Faculty  values ( 8102, 'James', 'Lancelot', 'AsstProf', 'M', 2792, '288', 'Krieger');
insert into Faculty  values ( 8118, 'Weinert', 'Howard', 'Professor', 'M', 3272, '119', 'Barton');
insert into Faculty  values ( 8122, 'Wierman', 'John', 'Professor', 'M', 3392,'288', 'Krieger');
insert into Faculty  values ( 8722, 'Cauwenberghs', 'Gert', 'AsstProf', 'M', 1372, '119', 'Barton');
insert into Faculty  values ( 8723, 'Andreou', 'Andreas', 'Professor', 'M', 1402, '119', 'Barton');
insert into Faculty  values ( 8772, 'Cowen', 'Lenore', 'AsstProf', 'F', 2870, '288', 'Krieger');
insert into Faculty  values ( 8791, 'McCloskey', 'Michael', 'Professor', 'M', 3440, '288', 'Krieger');
insert into Faculty  values ( 8989, 'Brent', 'Michael', 'AsstProf', 'M', 9373, '288', 'Krieger');
insert into Faculty  values ( 9011, 'Rapp', 'Brenda', 'AsstProf', 'F', 2032, '288', 'Krieger');
insert into Faculty  values ( 9191, 'Collins', 'Oliver', 'AssocProf', 'M', 5427, '119', 'Barton');
insert into Faculty  values ( 9199, 'Hughes', 'Brian', 'AssocProf', 'M', 5666, '119', 'Barton');
insert into Faculty  values ( 9210, 'Joseph', 'Richard', 'Professor', 'M', 5996, '119', 'Barton');
insert into Faculty  values ( 9514, 'Prince', 'Jerry', 'AssocProf', 'M', 5106, '119', 'Barton');
insert into Faculty  values ( 9823, 'Pang', 'Jong-Shi', 'Professor', 'M', 4366, '288', 'Krieger');
insert into Faculty  values ( 9824, 'Glaser', 'Robert', 'Instructor', 'M', 4396, '119', 'Barton');
insert into Faculty  values ( 9811, 'Wu', 'Colin', 'AsstProf', 'M', 2906, '288', 'Krieger');
insert into Faculty  values ( 9643, 'Legendre', 'Geraldine', 'AssocProf', 'F', 8972, '288', 'Krieger');
insert into Faculty  values ( 9379, 'Khurgin', 'Jacob', 'Professor', 'M', 1060, '119', 'Barton');
insert into Faculty  values ( 9922, 'Hall', 'Leslie', 'AsstProf', 'F', 7332, '288', 'Krieger');


insert into Department  values ( 010, 'AS', 'History of Art', '268', 'Mergenthaler', 7117);
insert into Department  values ( 020, 'AS', 'Biology', '144', 'Mudd', 7330);
insert into Department  values ( 030, 'AS', 'Chemistry', '113', 'Remsen', 7429);
insert into Department  values ( 040, 'AS', 'Classics', '121', 'Gilman', 7556);
insert into Department  values ( 050, 'AS', 'Cognitive Science', '381', 'Krieger', 7119);
insert into Department  values ( 060, 'AS', 'English', '146', 'Gilman', 7544);
insert into Department  values ( 070, 'AS', 'Anthropology', '404B', 'Macaulay', 7272);
insert into Department  values ( 090, 'AS', 'German', '245', 'Gilman', 7508);
insert into Department  values ( 100, 'AS', 'History', '312', 'Gilman', 7575);
insert into Department  values ( 110, 'AS', 'Mathematics', '404', 'Krieger', 7399);
insert into Department  values ( 130, 'AS', 'Near Eastern Studies', '128', 'Gilman', 7499);
insert into Department  values ( 140, 'AS', 'History of Science', '234', 'Gilman', 7501);
insert into Department  values ( 150, 'AS', 'Philosophy', '347', 'Gilman', 7524);
insert into Department  values ( 170, 'AS', 'Physics and Astronomy', '366', 'Bloomberg', 7347);
insert into Department  values ( 180, 'AS', 'Economics', '440', 'Mergenthaler', 7601);
insert into Department  values ( 190, 'AS', 'Political Science', '338', 'Mergenthaler', 7540);
insert into Department  values ( 200, 'AS', 'Psychology', '223', 'Ames', 7055);
insert into Department  values ( 340, 'AS', 'French', '225', 'Gilman', 7227);
insert into Department  values ( 350, 'AS', 'Hispanic/Italian Studies', '221', 'Gilman', 7226);
insert into Department  values ( 520, 'EN', 'ECE', '105', 'Barton', 7033);
insert into Department  values ( 530, 'EN', 'Mechanical Engineering', '122', 'Latrobe', 7132);
insert into Department  values ( 540, 'EN', 'Chemical Engineering', '24', 'NEB', 7170);
insert into Department  values ( 550, 'EN', 'Mathematical Sciences', '221', 'Maryland', 7195);
insert into Department  values ( 560, 'EN', 'Civil Engineering', '206', 'Latrobe', 8680);
insert into Department  values ( 580, 'EN', 'Biomedical Engineering', '144', 'NEB', 8482);
insert into Department  values ( 600, 'EN', 'Computer Science', '224', 'NEB', 8577);


insert into Member_of values (7792,  600, 'Primary');
insert into Member_of values (9210,  520, 'Primary');
insert into Member_of values (9811,  550, 'Primary');
insert into Member_of values (9643,  050, 'Primary');
insert into Member_of values (9379,  520, 'Primary');
insert into Member_of values (8918,  600, 'Primary');
insert into Member_of values (7712,  600, 'Primary');
insert into Member_of values (1121,  600, 'Primary');
insert into Member_of values (9172,  600, 'Primary');
insert into Member_of values (8423,  600, 'Primary');
insert into Member_of values (1148,  600, 'Primary');
insert into Member_of values (8741,  600, 'Primary');
insert into Member_of values (3457,  600, 'Primary');
insert into Member_of values (7723,  600, 'Primary');
insert into Member_of values (8721,  600, 'Primary');
insert into Member_of values (2192,  600, 'Primary');
insert into Member_of values (8114,  600, 'Primary');
insert into Member_of values (6112,  600, 'Primary');
insert into Member_of values (9826,  600, 'Primary');
insert into Member_of values (1193,  600, 'Primary');
insert into Member_of values (1082,  600, 'Primary');
insert into Member_of values (4230,  600, 'Primary');
insert into Member_of values (8989,  600, 'Secondary');
insert into Member_of values (7271,  600, 'Secondary');
insert into Member_of values (8721,  520, 'Secondary');
insert into Member_of values (8741,  050, 'Secondary');
insert into Member_of values (7271,  050, 'Secondary');
insert into Member_of values (6182,  550, 'Primary');
insert into Member_of values (8772,  550, 'Primary');
insert into Member_of values (2738,  550, 'Primary');
insert into Member_of values (2881,  550, 'Primary');
insert into Member_of values (9922,  550, 'Primary');
insert into Member_of values (6541,  550, 'Primary');
insert into Member_of values (8102,  550, 'Primary');
insert into Member_of values (1177,  550, 'Primary');
insert into Member_of values (9823,  550, 'Primary');
insert into Member_of values (2311,  550, 'Primary');
insert into Member_of values (2291,  550, 'Primary');
insert into Member_of values (8122,  550, 'Primary');
insert into Member_of values (8989,  050, 'Primary');
insert into Member_of values (4432,  050, 'Primary');
insert into Member_of values (5718,  050, 'Primary');
insert into Member_of values (8791,  050, 'Primary');
insert into Member_of values (9011,  050, 'Primary');
insert into Member_of values (6910,  050, 'Primary');
insert into Member_of values (8723,  520, 'Primary');
insert into Member_of values (2028,  520, 'Primary');
insert into Member_of values (8722,  520, 'Primary');
insert into Member_of values (9191,  520, 'Primary');
insert into Member_of values (1823,  520, 'Primary');
insert into Member_of values (7134,  520, 'Primary');
insert into Member_of values (9199,  520, 'Primary');
insert into Member_of values (6925,  520, 'Primary');
insert into Member_of values (7271,  520, 'Primary');
insert into Member_of values (6191,  520, 'Primary');
insert into Member_of values (2119,  520, 'Primary');
insert into Member_of values (9514,  520, 'Primary');
insert into Member_of values (7231,  520, 'Primary');
insert into Member_of values (1172,  520, 'Primary');
insert into Member_of values (8118,  520, 'Primary');
insert into Member_of values (7506,  520, 'Primary');
insert into Member_of values (6330,  520, 'Primary');
insert into Member_of values (9824,  520, 'Primary');


insert into Course values ( '600.101', 'COMPUTER LITERACY', 3, 6112, 'MTW', '3',600);
insert into Course values ( '600.103', 'INTRODUCTION TO COMPUTER SCIENCE', 1, 4230, 'Th', '4',600);
insert into Course values ( '600.107', 'INTRO TO PROGRAMMING IN JAVA', 3, 1193, 'MTW', '3',600);
insert into Course values ( '600.109', 'INTRO TO PROGRAMMING IN C/C++', 3, 4230, 'MTW', '12',600);
insert into Course values ( '600.113', 'EXPLORING THE INTERNET', 3, 6112, 'MTW', '4',600);
insert into Course values ( '600.121', 'JAVA PROGRAMMING', 3, 6112, 'ThF', '10:30-12',600);
insert into Course values ( '600.211', 'UNIX SYSTEMS PROGRAMMING', 3, 6112, 'ThF', '1-2:15',600);
insert into Course values ( '600.227', 'DATA STRUCTURES in JAVA', 3, 1121, 'MTW', '9',600);
insert into Course values ( '600.232', 'MULTIMEDIA COMPUTING', 3, 9826, 'MW', '1-2:30',600);
insert into Course values ( '600.271', 'COMPUTATIONAL MODELS', 3, 9172, 'MTW', '1',600);
insert into Course values ( '600.303', 'SUPERCOMPUTING', 1, 9826, 'W', '4-6:20',600);
insert into Course values ( '600.315', 'DATABASE SYSTEMS', 3, 2192, 'ThF', '2:30-4',600);
insert into Course values ( '600.333', 'COMPUTER SYSTEM FUNDAMENTALS', 3, 1148, 'MTW', '8',600);
insert into Course values ( '600.337', 'DISTRIBUTED SYSTEMS', 3, 8918, 'M', '3',600);
insert into Course values ( '600.363', 'INTRODUCTION TO ALGORITHMS', 3, 7712, 'MTW', '9',600);
insert into Course values ( '600.415', 'DATABASE SYSTEMS', 3, 2192, 'ThF', '2:30-4',600);
insert into Course values ( '600.433', 'COMPUTER SYSTEMS', 3, 1148, 'MTW', '8',600);
insert into Course values ( '600.437', 'DISTRIBUTED SYSTEMS', 3, 8918, 'M', '3',600);
insert into Course values ( '600.445', 'QUANTITATIVE MEDICAL COMPUTING', 3, 7723, 'ThF', '1-2:15',600);
insert into Course values ( '600.461', 'COMPUTER VISION', 3, 8114, 'MTW', '1',600);
insert into Course values ( '600.463', 'ALGORITHMS I', 3, 7712, 'MTW', '9',600);
insert into Course values ( '600.465', 'INTRO TO NATURAL LANGUAGE PROCESSING', 3, 7792, 'MTW', '2',600);
insert into Course values ( '600.509', 'COMPUTER SCIENCE INTERNSHIP', 3, 1121, 'M', '1',600);
insert into Course values ( '600.601', 'COMPUTER SCIENCE SEMINAR', 1, 6191, 'ThF', '10:30-12',600);
insert into Course values ( '600.657', 'HIGH PERFORMANCE GRAPHICS AND MODELING', 3, 8423, 'M', '4-5:30',600);
insert into Course values ( '600.787', 'SEMINAR ON COMPUTATIONAL GEOMETRY', 3, 1121, 'Th', '2',600);
insert into Course values ( '550.111', 'STATISTICAL ANALYSIS', 4, 2311, 'MTW', '12',550);
insert into Course values ( '550.171', 'DISCRETE MATHEMATICS', 4, 8772, 'MTW', '11',550);
insert into Course values ( '500.203', 'ACCOUNTING I', 3, 9823, 'T', '6:15-8:45',550);
insert into Course values ( '500.204', 'ACCOUNTING II', 3, 9823, 'Th', '6:15-8:45',550);
insert into Course values ( '500.205', 'BUSINESS LAW I', 3, 8791, 'W', '6:15-8:45',550);
insert into Course values ( '500.206', 'BUSINESS LAW II', 3, 8791, 'M', '6:15-8:45',550);
insert into Course values ( '550.291', 'LINEAR ALGEBRA AND DIFFERENTIAL EQNS', 4, 6541, 'MTW', '9',550);
insert into Course values ( '550.310', 'PROBABILITY AND STATISTICS', 4, 8102, 'MTW', '10',550);
insert into Course values ( '550.361', 'INTRODUCTION TO OPTIMIZATION', 4, 2881, 'MTW', '2',550);
insert into Course values ( '550.413', 'APPLIED STATISTICS AND DATA ANALYSIS', 4, 1177, 'MTW', '11',550);
insert into Course values ( '550.420', 'INTRODUCTION TO PROBABILITY', 4, 2738, 'MTW', '1',550);
insert into Course values ( '550.471', 'COMBINATORIAL ANALYSIS', 4, 8772, 'MTW', '12',550);
insert into Course values ( '550.620', 'PROBABILITY THEORY I', 3, 2738, 'MTW', '2',550);
insert into Course values ( '550.626', 'STOCHASTIC PROCESSES II', 3, 8102, 'MTW', '1',550);
insert into Course values ( '550.631', 'STATISTICAL INFERENCE', 3, 6182, 'MTW', '3',550);
insert into Course values ( '550.661', 'FOUNDATIONS OF OPTIMIZATION', 3, 9823, 'MTW', '10',550);
insert into Course values ( '550.671', 'COMBINATORIAL ANALYSIS', 3, 8772, 'MTW', '12',550);
insert into Course values ( '550.681', 'NUMERICAL ANALYSIS', 3, 6541, 'MTW', '11',550);
insert into Course values ( '550.721', 'PERCOLATION THEORY', 3, 8122, 'MTW', '9',550);
insert into Course values ( '550.750', 'TOPICS IN OPERATIONS RESEARCH', 3, 9922, 'MW', '3-4:30',550);
insert into Course values ( '550.790', 'TOPICS IN APPLIED MATH', 2, 2881, 'MT', '4:30-6',550);
insert into Course values ( '520.137', 'INTRODUCTION TO ECE', 3, 8723, 'MTW', '11',520);
insert into Course values ( '520.213', 'CIRCUITS', 4, 9210, 'MTW', '2',520);
insert into Course values ( '520.219', 'FIELDS, MATTER AND WAVES', 3, 9210, 'MTW', '3',520);
insert into Course values ( '520.325', 'INTEGRATED ELECTRONICS', 3, 6191, 'MTW', '3',520);
insert into Course values ( '520.345', 'ECE LABORATORY', 3, 1823, 'W', '2',520);
insert into Course values ( '520.349', 'MICROPROCESSOR LAB I', 3, 9824, 'Th', '8',520);
insert into Course values ( '520.353', 'CONTROL SYSTEMS', 3, 6925, 'MTW', '10',520);
insert into Course values ( '520.401', 'BASIC COMMUNICATIONS', 3, 6191, 'MTW', '1',520);
insert into Course values ( '520.410', 'FIBER OPTICS AND PHOTONICS', 3, 6191, 'MTW', '1',520);
insert into Course values ( '520.419', 'ITERATIVE ALGORITHMS', 3, 2119, 'MT', '4-5:15',520);
insert into Course values ( '520.421', 'INTRODUCTION TO NON-LINEAR SYSTEMS', 3, 7231, 'MTW', '9',520);
insert into Course values ( '520.432', 'TOPICS IN MEDICAL IMAGING SYSTEMS', 3, 9514, 'TTh', '8:30-10',520);
insert into Course values ( '520.435', 'DIGITAL SIGNAL PROCESSING', 4, 8118, 'MTW', '11',520);
insert into Course values ( '520.475', 'PROCESSING AND RECOGNITION OF SPEECH', 3, 6330, 'TW', '2-3:30',520);
insert into Course values ( '520.490', 'ANALOG AND DIGITAL VLSI SYSTEMS', 3, 8722, 'ThF', '10:30-12',520);
insert into Course values ( '520.603', 'ELECTROMAGNETIC WAVES', 4, 9210, 'Th', '1-4:30',520);
insert into Course values ( '520.605', 'SOLID STATE PHYSICS', 3, 9379, 'Tu', '1-4',520);
insert into Course values ( '520.609', 'NONLINEAR TECHNICAL IMAGE PROCESSING', 3, 7134, 'Th', '1-4',520);
insert into Course values ( '520.651', 'RANDOM SIGNAL ANALYSIS', 3, 9514, 'ThF', '10:30-12',520);
insert into Course values ( '050.102', 'LANGUAGE AND MIND', 3, 4432, 'MTW', '10',050);
insert into Course values ( '050.109', 'MIND, BRAIN, COMPUTERS', 3, 6910, 'MW', '2-3:15',050);
insert into Course values ( '050.203', 'COGNITIVE NEUROSCIENCE', 4, 9011, 'MT', '3:30-4:45',050);
insert into Course values ( '050.325', 'SOUND STRUCTURES IN NATURAL LANGUAGE', 3, 4432, 'T', '10-12',050);
insert into Course values ( '050.370', 'FORMAL METHODS IN COGNITIVE SCIENCE', 3, 6910, 'MW', '11:30',050);
insert into Course values ( '050.381', 'LANGUAGE DEVELOPMENT', 3, 8989, 'T', '1-3',050);
insert into Course values ( '050.427', 'THE HISTORY OF ROMANCE LANGUAGES', 3, 4432, 'W', '1-3',050);
insert into Course values ( '050.670', 'FORMAL METHODS IN COGNITIVE SCIENCE', 3, 4432, 'MW', '11:30-12:45',050);
insert into Course values ( '050.802', 'RESEARCH SEMINAR IN COGNITIVE PROCESSES', 1, 9011, 'W', '1-3',050);
insert into Course values ( '050.821', 'COMP. MODELS OF SENTENCE PROCESSING', 3, 5718, 'M', '1-4',050);

insert into Student values ( 1001, 'Smith', 'Linda', 18, 'F', 600, 1121,'BAL');
 insert into Student values ( 1002, 'Kim', 'Tracy', 19, 'F', 600, 7712,'HKG');
 insert into Student values ( 1003, 'Jones', 'Shiela', 21, 'F', 600, 7792,'WAS');
 insert into Student values ( 1004, 'Kumar', 'Dinesh', 20, 'M', 600, 8423,'CHI');
 insert into Student values ( 1005, 'Gompers', 'Paul', 26, 'M', 600, 1121,'YYZ');
 insert into Student values ( 1006, 'Schultz', 'Andy', 18, 'M', 600, 1148,'BAL');
 insert into Student values ( 1007, 'Apap', 'Lisa', 18, 'F', 600, 8918,'PIT');
 insert into Student values ( 1008, 'Nelson', 'Jandy', 20, 'F', 600, 9172,'BAL');
 insert into Student values ( 1009, 'Tai', 'Eric', 19, 'M', 600, 2192,'YYZ');
 insert into Student values ( 1010, 'Lee', 'Derek', 17, 'M', 600, 2192,'HOU');
 insert into Student values ( 1011, 'Adams', 'David', 22, 'M', 600, 1148,'PHL');
 insert into Student values ( 1012, 'Davis', 'Steven', 20, 'M', 600, 7723,'PIT');
 insert into Student values ( 1014, 'Norris', 'Charles', 18, 'M', 600, 8741, 'DAL');
 insert into Student values ( 1015, 'Lee', 'Susan', 16, 'F', 600, 8721,'HKG');
 insert into Student values ( 1016, 'Schwartz', 'Mark', 17, 'M', 600, 2192,'DET');
 insert into Student values ( 1017, 'Wilson', 'Bruce', 27, 'M', 600, 1148,'LON');
 insert into Student values ( 1018, 'Leighton', 'Michael', 20, 'M', 600, 1121, 'PIT');
 insert into Student values ( 1019, 'Pang', 'Arthur', 18, 'M', 600, 2192,'WAS');
 insert into Student values ( 1020, 'Thornton', 'Ian', 22, 'M', 520, 7271,'NYC');
 insert into Student values ( 1021, 'Andreou', 'George', 19, 'M', 520, 8722, 'NYC');
 insert into Student values ( 1022, 'Woods', 'Michael', 17, 'M', 540, 8722,'PHL');
 insert into Student values ( 1023, 'Shieber', 'David', 20, 'M', 520, 8722,'NYC');
 insert into Student values ( 1024, 'Prater', 'Stacy', 18, 'F', 540, 7271,'BAL');
 insert into Student values ( 1025, 'Goldman', 'Mark', 18, 'M', 520, 7134,'PIT');
 insert into Student values ( 1026, 'Pang', 'Eric', 19, 'M', 520, 7134,'HKG');
 insert into Student values ( 1027, 'Brody', 'Paul', 18, 'M', 520, 8723,'LOS');
 insert into Student values ( 1028, 'Rugh', 'Eric', 20, 'M', 550, 2311,'ROC');
 insert into Student values ( 1029, 'Han', 'Jun', 17, 'M', 100, 2311,'PEK');
 insert into Student values ( 1030, 'Cheng', 'Lisa', 21, 'F', 550, 2311,'SFO');
 insert into Student values ( 1031, 'Smith', 'Sarah', 20, 'F', 550, 8772,'PHL');
 insert into Student values ( 1032, 'Brown', 'Eric', 20, 'M', 550, 8772,'ATL');
 insert into Student values ( 1033, 'Simms', 'William', 18, 'M', 550, 8772,'NAR');
 insert into Student values ( 1034, 'Epp', 'Eric', 18, 'M', 050, 5718,'BOS');
 insert into Student values ( 1035, 'Schmidt', 'Sarah', 26, 'F', 050, 5718,'WAS');

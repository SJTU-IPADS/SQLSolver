create table Allergy_Type (
       Allergy 		  VARCHAR(20) PRIMARY KEY,
       AllergyType 	  VARCHAR(20)
);

create table Has_Allergy (
       StuID 		 INTEGER,
       Allergy 		 VARCHAR(20),
       FOREIGN KEY(StuID) REFERENCES Student(StuID),
       FOREIGN KEY(Allergy) REFERENCES Allergy_Type(Allergy)
);

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

insert into Has_Allergy values ( 1001, 'Cat' );
insert into Has_Allergy values ( 1002, 'Shellfish' );
insert into Has_Allergy values ( 1002, 'Tree Pollen' );
insert into Has_Allergy values ( 1003, 'Dog' );
insert into Has_Allergy values ( 1004, 'Nuts' );
insert into Has_Allergy values ( 1005, 'Nuts' );
insert into Has_Allergy values ( 1005, 'Tree Pollen' );
insert into Has_Allergy values ( 1006, 'Nuts' );
insert into Has_Allergy values ( 1007, 'Ragweed' );
insert into Has_Allergy values ( 1007, 'Tree Pollen' );
insert into Has_Allergy values ( 1007, 'Grass Pollen' );
insert into Has_Allergy values ( 1007, 'Eggs' );
insert into Has_Allergy values ( 1007, 'Milk' );
insert into Has_Allergy values ( 1007, 'Shellfish' );
insert into Has_Allergy values ( 1007, 'Anchovies' );
insert into Has_Allergy values ( 1007, 'Cat' );
insert into Has_Allergy values ( 1007, 'Dog' );
insert into Has_Allergy values ( 1009, 'Tree Pollen' );
insert into Has_Allergy values ( 1010, 'Ragweed' );
insert into Has_Allergy values ( 1010, 'Tree Pollen' );
insert into Has_Allergy values ( 1010, 'Grass Pollen' );
insert into Has_Allergy values ( 1010, 'Eggs' );
insert into Has_Allergy values ( 1010, 'Milk' );
insert into Has_Allergy values ( 1010, 'Shellfish' );
insert into Has_Allergy values ( 1010, 'Anchovies' );
insert into Has_Allergy values ( 1010, 'Cat' );
insert into Has_Allergy values ( 1010, 'Dog' );
insert into Has_Allergy values ( 1011, 'Ragweed' );
insert into Has_Allergy values ( 1012, 'Ragweed' );
insert into Has_Allergy values ( 1013, 'Ragweed' );
insert into Has_Allergy values ( 1014, 'Nuts' );
insert into Has_Allergy values ( 1015, 'Nuts' );
insert into Has_Allergy values ( 1015, 'Soy' );
insert into Has_Allergy values ( 1016, 'Nuts' );
insert into Has_Allergy values ( 1016, 'Milk' );
insert into Has_Allergy values ( 1017, 'Tree Pollen' );
insert into Has_Allergy values ( 1018, 'Nuts'  );
insert into Has_Allergy values ( 1018, 'Soy'  );
insert into Has_Allergy values ( 1019, 'Tree Pollen' );
insert into Has_Allergy values ( 1020, 'Tree Pollen' );
insert into Has_Allergy values ( 1021, 'Tree Pollen' );
insert into Has_Allergy values ( 1022, 'Nuts'  );
insert into Has_Allergy values ( 1022, 'Anchovies'  );
insert into Has_Allergy values ( 1023, 'Rodent'  );
insert into Has_Allergy values ( 1023, 'Cat'  );
insert into Has_Allergy values ( 1023, 'Nuts'  );
insert into Has_Allergy values ( 1024, 'Ragweed' );
insert into Has_Allergy values ( 1024, 'Tree Pollen' );
insert into Has_Allergy values ( 1025, 'Tree Pollen' );
insert into Has_Allergy values ( 1026, 'Grass Pollen' );
insert into Has_Allergy values ( 1027, 'Tree Pollen' );
insert into Has_Allergy values ( 1028, 'Tree Pollen' );
insert into Has_Allergy values ( 1029, 'Soy'  );
insert into Has_Allergy values ( 1029, 'Nuts'  );
insert into Has_Allergy values ( 1029, 'Eggs'  );
insert into Has_Allergy values ( 1030, 'Grass Pollen' );
insert into Has_Allergy values ( 1031, 'Nuts' );
insert into Has_Allergy values ( 1031, 'Shellfish' );
insert into Has_Allergy values ( 1031, 'Soy' );

insert into Allergy_Type values ( 'Eggs', 'food' );
insert into Allergy_Type values ( 'Nuts', 'food' );
insert into Allergy_Type values ( 'Milk', 'food' );
insert into Allergy_Type values ( 'Shellfish', 'food' );
insert into Allergy_Type values ( 'Anchovies', 'food' );
insert into Allergy_Type values ( 'Wheat', 'food' );
insert into Allergy_Type values ( 'Soy', 'food' );
insert into Allergy_Type values ( 'Ragweed', 'environmental' );
insert into Allergy_Type values ( 'Tree Pollen', 'environmental' );
insert into Allergy_Type values ( 'Grass Pollen', 'environmental' );
insert into Allergy_Type values ( 'Cat', 'animal' );
insert into Allergy_Type values ( 'Dog', 'animal' );
insert into Allergy_Type values ( 'Rodent', 'animal' );
insert into Allergy_Type values ( 'Bee Stings', 'animal' );

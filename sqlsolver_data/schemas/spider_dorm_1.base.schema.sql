
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
 

create table Dorm (
       dormid		INTEGER,
       dorm_name	VARCHAR(20),
       student_capacity	INTEGER,
       gender			VARCHAR(1)
) ;

create table Dorm_amenity (
       amenid			INTEGER,
       amenity_name		VARCHAR(25)
) ;

create table Has_amenity (
       dormid			INTEGER,
       amenid				INTEGER,
	FOREIGN KEY (dormid) REFERENCES Dorm(dormid),
	FOREIGN KEY (amenid) REFERENCES Dorm_amenity(amenid)
);

create table Lives_in (
       stuid 	      INTEGER,
       dormid		INTEGER,
       room_number	INTEGER,
	FOREIGN KEY (stuid) REFERENCES Student(StuID),
        FOREIGN KEY (dormid) REFERENCES Dorm(dormid)
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

insert into Dorm values (100,'Smith Hall',85,'X');
insert into Dorm values (110,'Bud Jones Hall',116,'M');
insert into Dorm values (140,'Fawlty Towers',355,'X');
insert into Dorm values (160,'Dorm-plex 2000',400,'X');
insert into Dorm values (109,'Anonymous Donor Hall',128,'F');
insert into Dorm values (117,'University Hovels',40,'X');
insert into Dorm values (104,'Grad Student Asylum',256,'X');


insert into Dorm_amenity values ( 900 , 'TV Lounge' ) ;
insert into Dorm_amenity values ( 901 , 'Study Room' ) ;
insert into Dorm_amenity values ( 902 , 'Pub in Basement' ) ;
insert into Dorm_amenity values ( 903 , 'Carpeted Rooms' ) ;
insert into Dorm_amenity values ( 904 , '4 Walls' ) ;
insert into Dorm_amenity values ( 930 , 'Roof' ) ;
insert into Dorm_amenity values ( 931 , 'Ethernet Ports' ) ;
insert into Dorm_amenity values ( 932 , 'Air Conditioning' ) ;
insert into Dorm_amenity values ( 922 , 'Heat' ) ;
insert into Dorm_amenity values ( 950 , 'Working Fireplaces' ) ;
insert into Dorm_amenity values ( 955 , 'Kitchen in Every Room' ) ;
insert into Dorm_amenity values ( 909 , 'Allows Pets' ) ;


insert into Has_amenity values ( 109 ,  900) ;
insert into Has_amenity values ( 109 ,  901) ;
insert into Has_amenity values ( 109 ,  903) ;
insert into Has_amenity values ( 109 ,  904 ) ;
insert into Has_amenity values ( 109 ,  930 ) ;
insert into Has_amenity values ( 109 ,  931) ;
insert into Has_amenity values ( 109 ,  932) ;
insert into Has_amenity values ( 109 ,  922) ;
insert into Has_amenity values ( 104 ,  901) ;
insert into Has_amenity values ( 104 ,  904) ;
insert into Has_amenity values ( 104 ,  930 ) ;
insert into Has_amenity values ( 160 , 900 ) ;
insert into Has_amenity values ( 160 , 901 ) ;
insert into Has_amenity values ( 160 , 902 ) ;
insert into Has_amenity values ( 160 , 903 ) ;
insert into Has_amenity values ( 160 , 931 ) ;
insert into Has_amenity values ( 160 ,  904) ;
insert into Has_amenity values ( 160 ,  930 ) ;
insert into Has_amenity values ( 160 ,  922 ) ;
insert into Has_amenity values ( 160 , 932 ) ;
insert into Has_amenity values ( 160 , 950 ) ;
insert into Has_amenity values ( 160 , 955 ) ;
insert into Has_amenity values ( 160 , 909 ) ;
insert into Has_amenity values ( 100 ,  901) ;
insert into Has_amenity values ( 100 ,  903) ;
insert into Has_amenity values ( 100 ,  904) ;
insert into Has_amenity values ( 100 ,  930 ) ;
insert into Has_amenity values ( 100 ,  922 ) ;
insert into Has_amenity values ( 117 ,  930 ) ;
insert into Has_amenity values ( 110 ,  901) ;
insert into Has_amenity values ( 110 ,  903) ;
insert into Has_amenity values ( 110 ,  904) ;
insert into Has_amenity values ( 110 ,  930 ) ;
insert into Has_amenity values ( 110 ,  922 ) ;
insert into Has_amenity values ( 140 ,  909 ) ;
insert into Has_amenity values ( 140 ,  900) ;
insert into Has_amenity values ( 140 ,  902) ;
insert into Has_amenity values ( 140 ,  904) ;
insert into Has_amenity values ( 140 ,  930 ) ;
insert into Has_amenity values ( 140 ,  932 ) ;


insert into Lives_in values ( 1001  , 109  , 105  ) ;
insert into Lives_in values ( 1002  , 100  , 112  ) ;
insert into Lives_in values ( 1003  , 100  , 124  ) ;
insert into Lives_in values ( 1004  , 140  , 215  ) ;
insert into Lives_in values ( 1005  , 160  , 333  ) ;
insert into Lives_in values ( 1007  , 140  , 113  ) ;
insert into Lives_in values ( 1008  , 160  , 334  ) ;
insert into Lives_in values ( 1009  , 140  , 332  ) ;
insert into Lives_in values ( 1010  , 160  , 443  ) ;
insert into Lives_in values ( 1011  , 140  , 102  ) ;
insert into Lives_in values ( 1012  , 160  , 333  ) ;
insert into Lives_in values ( 1014  , 104  , 211  ) ;
insert into Lives_in values ( 1015  , 160  , 443  ) ;
insert into Lives_in values ( 1016  , 140  , 301  ) ;
insert into Lives_in values ( 1017  , 140  , 319  ) ;
insert into Lives_in values ( 1018  , 140  , 225  ) ;
insert into Lives_in values ( 1020  , 160  , 405  ) ;
insert into Lives_in values ( 1021  , 160  , 405  ) ;
insert into Lives_in values ( 1022  , 100  , 153  ) ;
insert into Lives_in values ( 1023  , 160  , 317  ) ;
insert into Lives_in values ( 1024  , 100  , 151  ) ;
insert into Lives_in values ( 1025  , 160  , 317  ) ;
insert into Lives_in values ( 1027  , 140  , 208  ) ;
insert into Lives_in values ( 1028  , 110  , 48  ) ;
insert into Lives_in values ( 1029  , 140  , 418  ) ;
insert into Lives_in values ( 1030  , 109  , 211  ) ;
insert into Lives_in values ( 1031  , 100  , 112  ) ;
insert into Lives_in values ( 1032  , 109  , 105  ) ;
insert into Lives_in values ( 1033  , 117  , 3  ) ;
insert into Lives_in values ( 1034  , 160  , 105  ) ;
insert into Lives_in values ( 1035  , 100  , 124  ) ;


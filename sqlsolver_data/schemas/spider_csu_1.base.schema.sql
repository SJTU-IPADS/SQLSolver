DROP TABLE IF EXISTS Campuses;
CREATE TABLE IF NOT EXISTS Campuses (
	Id INTEGER PRIMARY KEY, 
	Campus TEXT, 
	Location TEXT, 
	County TEXT, 
	Year INTEGER 
);

DROP TABLE IF EXISTS csu_fees;
CREATE TABLE IF NOT EXISTS csu_fees ( 
	Campus INTEGER PRIMARY KEY, 
	Year INTEGER, 
	CampusFee INTEGER,
	FOREIGN KEY (Campus) REFERENCES Campuses(Id)
);

DROP TABLE IF EXISTS degrees;
CREATE TABLE IF NOT EXISTS degrees ( 
	Year INTEGER,
	Campus INTEGER, 
	Degrees INTEGER,
	PRIMARY KEY (Year, Campus),
	FOREIGN KEY (Campus) REFERENCES Campuses(Id)
);



DROP TABLE IF EXISTS discipline_enrollments;
CREATE TABLE IF NOT EXISTS discipline_enrollments ( 
	Campus INTEGER, 
	Discipline INTEGER, 
	Year INTEGER, 
	Undergraduate INTEGER, 
	Graduate INTEGER,
	PRIMARY KEY (Campus, Discipline),
	FOREIGN KEY (Campus) REFERENCES Campuses(Id)
);



DROP TABLE IF EXISTS enrollments;
CREATE TABLE IF NOT EXISTS enrollments ( 
	Campus INTEGER, 
	Year INTEGER, 
	TotalEnrollment_AY INTEGER, 
	FTE_AY INTEGER,
	PRIMARY KEY(Campus, Year),
	FOREIGN KEY (Campus) REFERENCES Campuses(Id)
);

DROP TABLE IF EXISTS faculty;
CREATE TABLE IF NOT EXISTS faculty ( 
	Campus INTEGER, 
	Year INTEGER, 
	Faculty REAL,
	FOREIGN KEY (Campus) REFERENCES Campuses(Id) 
);



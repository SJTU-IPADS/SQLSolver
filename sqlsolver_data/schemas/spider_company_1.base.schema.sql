CREATE TABLE works_on
(
    Essn  INTEGER,
    Pno   INTEGER,
    Hours REAL,
    PRIMARY KEY (Essn, Pno)
);

CREATE TABLE employee
(
    Fname     TEXT,
    Minit     TEXT,
    Lname     TEXT,
    Ssn       INTEGER PRIMARY KEY,
    Bdate     TEXT,
    Address   TEXT,
    Sex       TEXT,
    Salary    INTEGER,
    Super_ssn INTEGER,
    Dno       INTEGER
);

CREATE TABLE department
(
    Dname          TEXT,
    Dnumber        INTEGER PRIMARY KEY,
    Mgr_ssn        INTEGER,
    Mgr_start_date TEXT
);

CREATE TABLE project
(
    Pname     TEXT,
    Pnumber   INTEGER PRIMARY KEY,
    Plocation TEXT,
    Dnum      INTEGER
);

CREATE TABLE dependent
(
    Essn           INTEGER,
    Dependent_name TEXT,
    Sex            TEXT,
    Bdate          TEXT,
    Relationship   TEXT,
    PRIMARY KEY (Essn, Dependent_name)
);

CREATE TABLE dept_locations
(
    Dnumber   INTEGER,
    Dlocation TEXT,
    PRIMARY KEY (Dnumber, Dlocation)
);

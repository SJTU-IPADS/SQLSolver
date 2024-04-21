CREATE TABLE CLASS
(
    CLASS_CODE    varchar(5) PRIMARY KEY,
    CRS_CODE      varchar(10),
    CLASS_SECTION varchar(2),
    CLASS_TIME    varchar(20),
    CLASS_ROOM    varchar(8),
    PROF_NUM      int,
    FOREIGN KEY (CRS_CODE) REFERENCES COURSE (CRS_CODE),
    FOREIGN KEY (PROF_NUM) REFERENCES EMPLOYEE (EMP_NUM)
);

CREATE TABLE COURSE
(
    CRS_CODE        varchar(10) PRIMARY KEY,
    DEPT_CODE       varchar(10),
    CRS_DESCRIPTION varchar(35),
    CRS_CREDIT      float(8),
    FOREIGN KEY (DEPT_CODE) REFERENCES DEPARTMENT (DEPT_CODE)
);

CREATE TABLE DEPARTMENT
(
    DEPT_CODE      varchar(10) PRIMARY KEY,
    DEPT_NAME      varchar(30),
    SCHOOL_CODE    varchar(8),
    EMP_NUM        int,
    DEPT_ADDRESS   varchar(20),
    DEPT_EXTENSION varchar(4),
    FOREIGN KEY (EMP_NUM) REFERENCES EMPLOYEE (EMP_NUM)
);

CREATE TABLE EMPLOYEE
(
    EMP_NUM      int PRIMARY KEY,
    EMP_LNAME    varchar(15),
    EMP_FNAME    varchar(12),
    EMP_INITIAL  varchar(1),
    EMP_JOBCODE  varchar(5),
    EMP_HIREDATE datetime,
    EMP_DOB      datetime
);

CREATE TABLE ENROLL
(
    CLASS_CODE   varchar(5),
    STU_NUM      int,
    ENROLL_GRADE varchar(50),
    FOREIGN KEY (CLASS_CODE) REFERENCES CLASS (CLASS_CODE),
    FOREIGN KEY (STU_NUM) REFERENCES STUDENT (STU_NUM)
);

CREATE TABLE PROFESSOR
(
    EMP_NUM          int,
    DEPT_CODE        varchar(10),
    PROF_OFFICE      varchar(50),
    PROF_EXTENSION   varchar(4),
    PROF_HIGH_DEGREE varchar(5),
    FOREIGN KEY (EMP_NUM) REFERENCES EMPLOYEE (EMP_NUM),
    FOREIGN KEY (DEPT_CODE) REFERENCES DEPARTMENT (DEPT_CODE)
);

CREATE TABLE STUDENT
(
    STU_NUM      int PRIMARY KEY,
    STU_LNAME    varchar(15),
    STU_FNAME    varchar(15),
    STU_INIT     varchar(1),
    STU_DOB      datetime,
    STU_HRS      int,
    STU_CLASS    varchar(2),
    STU_GPA      float(8),
    STU_TRANSFER numeric,
    DEPT_CODE    varchar(18),
    STU_PHONE    varchar(4),
    PROF_NUM     int,
    FOREIGN KEY (DEPT_CODE) REFERENCES DEPARTMENT (DEPT_CODE)
);

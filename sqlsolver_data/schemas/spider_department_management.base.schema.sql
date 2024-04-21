
CREATE TABLE IF NOT EXISTS department (
Department_ID int,
Name text,
Creation text,
Ranking int,
Budget_in_Billions real,
Num_Employees real,
PRIMARY KEY (Department_ID)
);















CREATE TABLE IF NOT EXISTS head (
head_ID int,
name text,
born_state text,
age real,
PRIMARY KEY (head_ID)
);










CREATE TABLE IF NOT EXISTS management (
department_ID int,
head_ID int,
temporary_acting text,
PRIMARY KEY (Department_ID,head_ID),
FOREIGN KEY (Department_ID) REFERENCES department(Department_ID),
FOREIGN KEY (head_ID) REFERENCES head(head_ID)
);







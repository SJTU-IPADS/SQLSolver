

DROP TABLE IF EXISTS School;
CREATE TABLE IF NOT EXISTS School (
School_id text,
School_name text,
Location text,
Mascot text,
Enrollment int,
IHSAA_Class text,
IHSAA_Football_Class text,
County text,
PRIMARY KEY (School_id)
);

DROP TABLE IF EXISTS budget;
CREATE TABLE IF NOT EXISTS budget (
School_id int,
Year int,
Budgeted int,
total_budget_percent_budgeted real,
Invested int,
total_budget_percent_invested real,
Budget_invested_percent text,
PRIMARY KEY(School_id,YEAR),
FOREIGN KEY(School_id) REFERENCES School(School_id)

);

DROP TABLE IF EXISTS endowment;
CREATE TABLE IF NOT EXISTS endowment (
endowment_id int,
School_id int,
donator_name text,
amount real,
PRIMARY KEY(endowment_id),
FOREIGN KEY(School_id) REFERENCES School(School_id)
);






























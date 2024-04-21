
CREATE TABLE IF NOT EXISTS Web_client_accelerator (
id int,
name text,
Operating_system text,
Client text,
Connection text,
primary key(id)
);



















CREATE TABLE IF NOT EXISTS browser (
id int,
name text,
market_share real,
primary key(id)
);




CREATE TABLE IF NOT EXISTS accelerator_compatible_browser (
accelerator_id int,
browser_id int,
compatible_since_year int,
primary key(accelerator_id, browser_id),
foreign key (accelerator_id) references Web_client_accelerator(id),
foreign key (browser_id) references browser(id)
);











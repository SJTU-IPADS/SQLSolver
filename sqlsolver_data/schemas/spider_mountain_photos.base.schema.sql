
CREATE TABLE IF NOT EXISTS mountain (
id int,
name text,
Height real,
Prominence real,
Range text,
Country text,
primary key(id)
);























CREATE TABLE IF NOT EXISTS camera_lens (
id int,
brand text,
name text,
focal_length_mm real,
max_aperture real,
primary key(id)
);











CREATE TABLE IF NOT EXISTS photos (
id int, 
camera_lens_id int,
mountain_id int,
color text, 
name text,
primary key(id),
foreign key(camera_lens_id) references camera_lens(id),
foreign key(mountain_id) references mountain(id)
);












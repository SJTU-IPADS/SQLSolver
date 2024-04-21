
DROP TABLE IF EXISTS actor;
CREATE TABLE IF NOT EXISTS actor (
aid int,
gender text,
name text,
nationality text,
birth_city text,
birth_year int,
primary key(aid)
);


DROP TABLE IF EXISTS copyright;
CREATE TABLE IF NOT EXISTS copyright (
id int,
msid int,
cid int,
primary key(id)
);
DROP TABLE IF EXISTS cast;
CREATE TABLE IF NOT EXISTS cast (
id int,
msid int,
aid int,
role int,
primary key(id),
foreign key(aid) references actor(aid),
foreign key(msid) references copyright(msid)
);

DROP TABLE IF EXISTS genre;
CREATE TABLE IF NOT EXISTS genre (
gid int,
genre text,
primary key(gid)
);

DROP TABLE IF EXISTS classification;
CREATE TABLE IF NOT EXISTS classification (
id int,
msid int,
gid int,
primary key(id),
foreign key(gid) references genre(gid),
foreign key(msid) references copyright(msid)
);

DROP TABLE IF EXISTS company;
CREATE TABLE IF NOT EXISTS company (
id int,
name text,
country_code text,
primary key(id)
);


DROP TABLE IF EXISTS director;
CREATE TABLE IF NOT EXISTS director (
did int,
gender text,
name text,
nationality text,
birth_city text,
birth_year int,
primary key(did)
);

DROP TABLE IF EXISTS producer;
CREATE TABLE IF NOT EXISTS producer (
pid int,
gender text,
name text,
nationality text,
birth_city text,
birth_year int,
primary key(pid)
);

DROP TABLE IF EXISTS directed_by;
CREATE TABLE IF NOT EXISTS directed_by (
id int,
msid int,
did int,
primary key(id),
foreign key(msid) references copyright(msid),
foreign key(did) references director(did)
);

DROP TABLE IF EXISTS keyword;
CREATE TABLE IF NOT EXISTS keyword (
id int,
keyword text,
primary key(id)
);

DROP TABLE IF EXISTS made_by;
CREATE TABLE IF NOT EXISTS made_by (
id int,
msid int,
pid int,
primary key(id),
foreign key(msid) references copyright(msid),
foreign key(pid) references producer(pid)
);

DROP TABLE IF EXISTS movie;
CREATE TABLE IF NOT EXISTS movie (
mid int,
title text,
release_year int,
title_aka text,
budget text,
primary key(mid)
);
DROP TABLE IF EXISTS tags;
CREATE TABLE IF NOT EXISTS tags (
id int,
msid int,
kid int,
primary key(id),
foreign key(msid) references copyright(msid),
foreign key(kid) references keyword(kid)
);
DROP TABLE IF EXISTS tv_series;
CREATE TABLE IF NOT EXISTS tv_series (
sid int,
title text,
release_year int,
num_of_seasons int,
num_of_episodes int,
title_aka text,
budget text,
primary key(sid)
);
DROP TABLE IF EXISTS writer;
CREATE TABLE IF NOT EXISTS writer (
wid int,
gender text,
name int,
nationality int,
num_of_episodes int,
birth_city text,
birth_year int,
primary key(wid)
);
DROP TABLE IF EXISTS written_by;
CREATE TABLE IF NOT EXISTS written_by (
id int,
msid int,
wid int,
foreign key(msid) references copyright(msid),
foreign key(wid) references writer(wid)
);

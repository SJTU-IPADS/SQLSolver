
DROP TABLE IF EXISTS author;
CREATE TABLE IF NOT EXISTS author (
aid int,
homepage text,
name text,
oid int,
primary key(aid)
);
DROP TABLE IF EXISTS conference;
CREATE TABLE IF NOT EXISTS conference (
cid int,
homepage text,
name text,
primary key (cid)
);
DROP TABLE IF EXISTS domain;
CREATE TABLE IF NOT EXISTS domain (
did int,
name text,
primary key (did)
);
DROP TABLE IF EXISTS domain_author;
CREATE TABLE IF NOT EXISTS domain_author (
aid int, 
did int,
primary key (did, aid),
foreign key(aid) references author(aid),
foreign key(did) references domain(did)
);

DROP TABLE IF EXISTS domain_conference;
CREATE TABLE IF NOT EXISTS domain_conference (
cid int,
did int,
primary key (did, cid),
foreign key(cid) references conference(cid),
foreign key(did) references domain(did)
);
DROP TABLE IF EXISTS journal;
CREATE TABLE IF NOT EXISTS journal (
homepage text,
jid int,
name text,
primary key(jid)
);
DROP TABLE IF EXISTS domain_journal;
CREATE TABLE IF NOT EXISTS domain_journal (
did int,
jid int,
primary key (did, jid),
foreign key(jid) references journal(jid),
foreign key(did) references domain(did)
);
DROP TABLE IF EXISTS keyword;
CREATE TABLE IF NOT EXISTS keyword (
keyword text,
kid int,
primary key(kid)
);
DROP TABLE IF EXISTS domain_keyword;
CREATE TABLE IF NOT EXISTS domain_keyword (
did int,
kid int,
primary key (did, kid),
foreign key(kid) references keyword(kid),
foreign key(did) references domain(did)
);
DROP TABLE IF EXISTS publication;
CREATE TABLE IF NOT EXISTS publication (
abstract text,
cid text,
citation_num int,
jid int,
pid int,
reference_num int,
title text,
year int,
primary key(pid),
foreign key(jid) references journal(jid),
foreign key(cid) references conference(cid)
);
DROP TABLE IF EXISTS domain_publication;
CREATE TABLE IF NOT EXISTS domain_publication (
did int,
pid int,
primary key (did, pid),
foreign key(pid) references publication(pid),
foreign key(did) references domain(did)
);

DROP TABLE IF EXISTS organization;
CREATE TABLE IF NOT EXISTS organization (
continent text,
homepage text,
name text,
oid int,
primary key(oid)
);

DROP TABLE IF EXISTS publication_keyword;
CREATE TABLE IF NOT EXISTS publication_keyword (
pid int,
kid int,
primary key (kid, pid),
foreign key(pid) references publication(pid),
foreign key(kid) references keyword(kid)
);
DROP TABLE IF EXISTS writes;
CREATE TABLE IF NOT EXISTS writes (
aid int,
pid int,
primary key (aid, pid),
foreign key(pid) references publication(pid),
foreign key(aid) references author(aid)
);
DROP TABLE IF EXISTS cite;
CREATE TABLE IF NOT EXISTS cite (
cited int,
citing  int,
foreign key(cited) references publication(pid),
foreign key(citing) references publication(pid)
);

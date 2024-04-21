
--data definition

create table genre(
	g_name varchar2(20) not null,
	rating varchar2(10),
	most_popular_in varchar2(50),
	primary key(g_name)
);

create table artist(
	artist_name varchar2(50) not null,
	country varchar2(20),
	gender varchar2(20),
	preferred_genre varchar2(50),
	constraint a_name primary key(artist_name),
	foreign key(preferred_genre) references genre(g_name) ON DELETE CASCADE
);

create table files(
	f_id number(10) not null,
	artist_name varchar2(50),
	file_size varchar2(20),
	duration varchar2(20),
	formats varchar2(20),
	primary key(f_id),
	foreign key(artist_name) references artist(artist_name) ON DELETE CASCADE
);


create table song(
	song_name varchar2(50),
	artist_name varchar2(50),
	country varchar2(20),
	f_id number(10),
    	genre_is varchar2(20),
	rating number(10) check(rating>0 and rating<11),
	languages varchar2(20),
	releasedate Date, 
	resolution number(10) not null,
	constraint s_name primary key(song_name),
	foreign key(artist_name) references artist(artist_name) ON DELETE CASCADE,
	foreign key(f_id) references files(f_id) ON DELETE CASCADE,
	foreign key(genre_is) references genre(g_name) ON DELETE CASCADE
);


--insertion of attributes in the table


insert into genre(g_name,rating,most_popular_in) values ('tagore','8','Bangladesh');
insert into genre values ('nazrul','7','Bangladesh');
insert into genre values ('folk','9','Sylhet,Chittagong,Kustia');
insert into genre values ('modern','8','Bangladesh');
insert into genre values ('blues','7','Canada');
insert into genre values ('pop','9','America');


insert into artist(artist_name,country,gender,preferred_genre) values('Shrikanta','India','Male','tagore');
insert into artist values('Prity','Bangladesh','Female','nazrul');
insert into artist values('Farida','Bangladesh','Female','folk');
insert into artist values('Topu','India','Female','modern');
insert into artist values('Enrique','USA','Male','blues');
insert into artist values('Michel','UK','Male','pop');


insert into files(f_id,artist_name,file_size,duration,formats) values (1,'Shrikanta','3.78 MB','3:45','mp4');
insert into files values (2,'Prity','4.12 MB','2:56','mp3');
insert into files values (3,'Farida','3.69 MB','4:12','mp4');
insert into files values (4,'Enrique','4.58 MB','5:23','mp4');
insert into files values (5,'Michel','5.10 MB','4:34','mp3');
insert into files values (6,'Topu','4.10 MB','4:30','mp4');

insert into song(song_name,artist_name,country,f_id,genre_is,rating,languages,releasedate,resolution) values ('Tumi robe nirobe','Shrikanta','India','1','tagore','8','bangla','28-AUG-2011',1080);
insert into song values ('Shukno patar nupur pae','Prity','Bangladesh','2','nazrul','5','bangla','21-SEP-1997',512);
insert into song values ('Ami opar hoye','Farida','Bangladesh','3','folk','7','bangla','7-APR-2001',320);
insert into song values ('My love','Enrique','USA','4','blues','6','english','24-JAN-2007',1080);
insert into song values ('Just beat it','Michel','UK','5','pop','8','english','17-MAR-2002',720);
insert into song values ('Aj ei akash','Topu','India','6','modern','10','bangla','27-MAR-2004',320);

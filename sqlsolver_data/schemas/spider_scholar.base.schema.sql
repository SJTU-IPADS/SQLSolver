



CREATE TABLE venue (
  venueId integer NOT NULL
,  venueName varchar(100) DEFAULT NULL
,  PRIMARY KEY (venueId)
);



CREATE TABLE author (
  authorId integer NOT NULL
,  authorName varchar(50) DEFAULT NULL
,  PRIMARY KEY (authorId)
);


CREATE TABLE dataset (
  datasetId integer NOT NULL
,  datasetName varchar(50) DEFAULT NULL
,  PRIMARY KEY (datasetId)
);


CREATE TABLE journal (
  journalId integer NOT NULL
,  journalName varchar(100) DEFAULT NULL
,  PRIMARY KEY (journalId)
);

CREATE TABLE keyphrase (
  keyphraseId integer NOT NULL
,  keyphraseName varchar(50) DEFAULT NULL
,  PRIMARY KEY (keyphraseId)
);


CREATE TABLE paper (
  paperId integer NOT NULL
,  title varchar(300) DEFAULT NULL
,  venueId integer DEFAULT NULL
,  year integer DEFAULT NULL
,  numCiting integer DEFAULT NULL
,  numCitedBy integer DEFAULT NULL
,  journalId integer DEFAULT NULL
,  PRIMARY KEY (paperId)
,  FOREIGN KEY(journalId) REFERENCES journal(journalId)
,  FOREIGN KEY(venueId) REFERENCES venue(venueId)
);



CREATE TABLE cite (
  citingPaperId integer NOT NULL
,  citedPaperId integer NOT NULL
,  PRIMARY KEY (citingPaperId,citedPaperId)
,  FOREIGN KEY(citedpaperId) REFERENCES paper(paperId)
,  FOREIGN KEY(citingpaperId) REFERENCES paper(paperId)
);


CREATE TABLE paperDataset (
  paperId integer DEFAULT NULL
,  datasetId integer DEFAULT NULL
,  PRIMARY KEY (datasetId, paperId)
);



CREATE TABLE paperKeyphrase (
  paperId integer DEFAULT NULL
,  keyphraseId integer DEFAULT NULL
,  PRIMARY KEY (keyphraseId,paperId)
,  FOREIGN KEY(paperId) REFERENCES paper(paperId)
,  FOREIGN KEY(keyphraseId) REFERENCES keyphrase(keyphraseId)
);


CREATE TABLE writes (
  paperId integer DEFAULT NULL
,  authorId integer DEFAULT NULL
,  PRIMARY KEY (paperId,authorId)
,  FOREIGN KEY(paperId) REFERENCES paper(paperId)
,  FOREIGN KEY(authorId) REFERENCES author(authorId)
);


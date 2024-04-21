CREATE TABLE `Album`
(
    `AlbumId`  int(11)      NOT NULL,
    `Title`    varchar(160) NOT NULL,
    `ArtistId` int(11)      NOT NULL,
    PRIMARY KEY (`AlbumId`),
    CONSTRAINT `FK_AlbumArtistId` FOREIGN KEY (`ArtistId`) REFERENCES `Artist` (`ArtistId`) ON DELETE NO ACTION ON UPDATE NO ACTION
);
CREATE TABLE `Artist`
(
    `ArtistId` int(11) NOT NULL,
    `Name`     varchar(120) DEFAULT NULL,
    PRIMARY KEY (`ArtistId`)
);
CREATE TABLE `Customer`
(
    `CustomerId`   int(11)     NOT NULL,
    `FirstName`    varchar(40) NOT NULL,
    `LastName`     varchar(20) NOT NULL,
    `Company`      varchar(80) DEFAULT NULL,
    `Address`      varchar(70) DEFAULT NULL,
    `City`         varchar(40) DEFAULT NULL,
    `State`        varchar(40) DEFAULT NULL,
    `Country`      varchar(40) DEFAULT NULL,
    `PostalCode`   varchar(10) DEFAULT NULL,
    `Phone`        varchar(24) DEFAULT NULL,
    `Fax`          varchar(24) DEFAULT NULL,
    `Email`        varchar(60) NOT NULL,
    `SupportRepId` int(11)     DEFAULT NULL,
    PRIMARY KEY (`CustomerId`),
    CONSTRAINT `FK_CustomerSupportRepId` FOREIGN KEY (`SupportRepId`) REFERENCES `Employee` (`EmployeeId`) ON DELETE NO ACTION ON UPDATE NO ACTION
);
CREATE TABLE `Employee`
(
    `EmployeeId` int(11)     NOT NULL,
    `LastName`   varchar(20) NOT NULL,
    `FirstName`  varchar(20) NOT NULL,
    `Title`      varchar(30) DEFAULT NULL,
    `ReportsTo`  int(11)     DEFAULT NULL,
    `BirthDate`  datetime    DEFAULT NULL,
    `HireDate`   datetime    DEFAULT NULL,
    `Address`    varchar(70) DEFAULT NULL,
    `City`       varchar(40) DEFAULT NULL,
    `State`      varchar(40) DEFAULT NULL,
    `Country`    varchar(40) DEFAULT NULL,
    `PostalCode` varchar(10) DEFAULT NULL,
    `Phone`      varchar(24) DEFAULT NULL,
    `Fax`        varchar(24) DEFAULT NULL,
    `Email`      varchar(60) DEFAULT NULL,
    PRIMARY KEY (`EmployeeId`),
    CONSTRAINT `FK_EmployeeReportsTo` FOREIGN KEY (`ReportsTo`) REFERENCES `Employee` (`EmployeeId`) ON DELETE NO ACTION ON UPDATE NO ACTION
);
CREATE TABLE `Genre`
(
    `GenreId` int(11) NOT NULL,
    `Name`    varchar(120) DEFAULT NULL,
    PRIMARY KEY (`GenreId`)
);
CREATE TABLE `Invoice`
(
    `InvoiceId`         int(11)        NOT NULL,
    `CustomerId`        int(11)        NOT NULL,
    `InvoiceDate`       datetime       NOT NULL,
    `BillingAddress`    varchar(70) DEFAULT NULL,
    `BillingCity`       varchar(40) DEFAULT NULL,
    `BillingState`      varchar(40) DEFAULT NULL,
    `BillingCountry`    varchar(40) DEFAULT NULL,
    `BillingPostalCode` varchar(10) DEFAULT NULL,
    `Total`             decimal(10, 2) NOT NULL,
    PRIMARY KEY (`InvoiceId`),
    CONSTRAINT `FK_InvoiceCustomerId` FOREIGN KEY (`CustomerId`) REFERENCES `Customer` (`CustomerId`) ON DELETE NO ACTION ON UPDATE NO ACTION
);
CREATE TABLE `InvoiceLine`
(
    `InvoiceLineId` int(11)        NOT NULL,
    `InvoiceId`     int(11)        NOT NULL,
    `TrackId`       int(11)        NOT NULL,
    `UnitPrice`     decimal(10, 2) NOT NULL,
    `Quantity`      int(11)        NOT NULL,
    PRIMARY KEY (`InvoiceLineId`),
    CONSTRAINT `FK_InvoiceLineInvoiceId` FOREIGN KEY (`InvoiceId`) REFERENCES `Invoice` (`InvoiceId`) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT `FK_InvoiceLineTrackId` FOREIGN KEY (`TrackId`) REFERENCES `Track` (`TrackId`) ON DELETE NO ACTION ON UPDATE NO ACTION
);
CREATE TABLE `MediaType`
(
    `MediaTypeId` int(11) NOT NULL,
    `Name`        varchar(120) DEFAULT NULL,
    PRIMARY KEY (`MediaTypeId`)
);
CREATE TABLE `Playlist`
(
    `PlaylistId` int(11) NOT NULL,
    `Name`       varchar(120) DEFAULT NULL,
    PRIMARY KEY (`PlaylistId`)
);
CREATE TABLE PlaylistTrack
(
    PlaylistId int NOT NULL,
    TrackId    int NOT NULL,
    PRIMARY KEY (PlaylistId, TrackId),
    CONSTRAINT FK_PlaylistTrackPlaylistId FOREIGN KEY (PlaylistId) REFERENCES Playlist (PlaylistId) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_PlaylistTrackTrackId FOREIGN KEY (TrackId) REFERENCES Track (TrackId) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE TABLE Track
(
    TrackId      int            NOT NULL,
    Name         varchar(200)   NOT NULL,
    AlbumId      int          DEFAULT NULL,
    MediaTypeId  int            NOT NULL,
    GenreId      int          DEFAULT NULL,
    Composer     varchar(220) DEFAULT NULL,
    Milliseconds int            NOT NULL,
    Bytes        int          DEFAULT NULL,
    UnitPrice    decimal(10, 2) NOT NULL,
    PRIMARY KEY (TrackId),
    CONSTRAINT FK_TrackAlbumId FOREIGN KEY (AlbumId) REFERENCES Album (AlbumId) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_TrackGenreId FOREIGN KEY (GenreId) REFERENCES Genre (GenreId) ON DELETE NO ACTION ON UPDATE NO ACTION,
    CONSTRAINT FK_TrackMediaTypeId FOREIGN KEY (MediaTypeId) REFERENCES MediaType (MediaTypeId) ON DELETE NO ACTION ON UPDATE NO ACTION
);

CREATE INDEX idx_InvoiceLine_IFK_InvoiceLineInvoiceId ON InvoiceLine (InvoiceId);
CREATE INDEX idx_InvoiceLine_IFK_InvoiceLineTrackId ON InvoiceLine (TrackId);
CREATE INDEX idx_Album_IFK_AlbumArtistId ON Album (ArtistId);
CREATE INDEX idx_Employee_IFK_EmployeeReportsTo ON Employee (ReportsTo);
CREATE INDEX idx_PlaylistTrack_IFK_PlaylistTrackTrackId ON PlaylistTrack (TrackId);
CREATE INDEX idx_Customer_IFK_CustomerSupportRepId ON Customer (SupportRepId);
CREATE INDEX idx_Track_IFK_TrackAlbumId ON Track (AlbumId);
CREATE INDEX idx_Track_IFK_TrackGenreId ON Track (GenreId);
CREATE INDEX idx_Track_IFK_TrackMediaTypeId ON Track (MediaTypeId);
CREATE INDEX idx_Invoice_IFK_InvoiceCustomerId ON Invoice (CustomerId);
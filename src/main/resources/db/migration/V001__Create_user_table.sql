create table USER(
    ID int not null AUTO_INCREMENT PRIMARY KEY,
    FIRST_NAME varchar(50) not null,
    LAST_NAME varchar(50) not null,
    EMAIL varchar(45) not null,
    PASSWORD varchar(64) not null,
    GENDER varchar(15) not null,
--    CREATED_BY  not null,
    CREATED_DATE DATETIME not null,
--    LAST_MODIFIED_BY not null,
    LAST_MODIFIED_DATE DATETIME not null
);
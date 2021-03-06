CREATE TABLE VERSION (
	VALUE INTEGER NOT NULL,
	VALUE_DATE TIMESTAMP NOT NULL,
	CONSTRAINT PK_VERSION PRIMARY KEY(VALUE)
);

CREATE TABLE PROJECT (
	ID INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1) NOT NULL,
	NAME VARCHAR(40) NOT NULL,
	CONSTRAINT PK_PROJECT PRIMARY KEY(ID),
	CONSTRAINT UQ_PROJECT_NAME UNIQUE(NAME)
);
CREATE TABLE logbackup
(
	logbackup_id integer NOT NULL,
	object_id integer,
	type_object integer,
	handle character varying(256),
	action text,
	last_modification timestamp with time zone,
	CONSTRAINT logbackup_pkey PRIMARY KEY (logbackup_id)
);

ALTER TABLE logbackup OWNER TO dspace;

CREATE SEQUENCE logbackup_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE logbackup_seq OWNER TO dspace;


CREATE TABLE sthanfile
(
	sthanfile_id integer NOT NULL,
	object_id integer,
	type_object integer,
	handle character varying(256),
	last_backup timestamp with time zone,
	md5 character varying(256),
	last_sendcloud timestamp with time zone,
	etag character varying(256),	
	CONSTRAINT sthanfile_pkey PRIMARY KEY (sthanfile_id)
);

ALTER TABLE sthanfile OWNER TO dspace;

CREATE SEQUENCE sthanfile_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

ALTER TABLE sthanfile_seq OWNER TO dspace;


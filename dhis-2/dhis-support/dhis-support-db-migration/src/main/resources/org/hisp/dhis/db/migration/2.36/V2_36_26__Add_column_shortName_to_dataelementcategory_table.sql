alter table dataelementcategory
    add column if not exists shortname character varying(50);

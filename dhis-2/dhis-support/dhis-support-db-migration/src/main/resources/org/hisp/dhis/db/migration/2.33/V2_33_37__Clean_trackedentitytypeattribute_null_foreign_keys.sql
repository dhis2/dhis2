delete from trackedentitytypeattribute where trackedentityattributeid is null;
ALTER TABLE trackedentitytypeattribute ALTER COLUMN trackedentityattributeid SET NOT NULL;

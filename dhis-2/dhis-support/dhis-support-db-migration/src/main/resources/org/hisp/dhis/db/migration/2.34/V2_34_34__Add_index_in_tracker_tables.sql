create unique index if not exists in_unique_trackedentityprogramowner_teiid_programid_ouid on trackedentityprogramowner (trackedentityinstanceid, programid, organisationunitid);
create index if not exists in_programinstance_programid on programinstance (programid);
create unique index if not exists in_trackedentityinstance_trackedentityattribute_value on trackedentityattributevalue (trackedentityinstanceid, trackedentityattributeid, lower(value));
create index in_programstageinstance_status_executiondate on programstageinstance USING btree (status,executiondate);

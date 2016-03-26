CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- A table that will store the whole transaction log of the database.
CREATE TABLE IF NOT EXISTS txlog (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT clock_timestamp(),
  payload text
) WITH (OIDS=FALSE);

CREATE OR REPLACE FUNCTION update_modified_at()
  RETURNS TRIGGER AS $updt$
  BEGIN
    NEW.modified_at := clock_timestamp();
    RETURN NEW;
  END;
$updt$ LANGUAGE plpgsql;

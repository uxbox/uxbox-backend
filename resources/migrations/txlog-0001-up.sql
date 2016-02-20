-- A table that will store the whole transaction log of the database.
CREATE TABLE IF NOT EXISTS txlog (
  id uuid PRIMARY KEY,
  created_at timestamptz,
  payload text
) WITH (OIDS=FALSE);

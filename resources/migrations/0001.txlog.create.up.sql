CREATE EXTENSION "uuid-ossp";

-- A table that will store the whole transaction log of the database.
CREATE TABLE IF NOT EXISTS txlog (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  payload text
) WITH (OIDS=FALSE);

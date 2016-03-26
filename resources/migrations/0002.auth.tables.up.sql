CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT clock_timestamp(),
  modified_at timestamptz DEFAULT clock_timestamp(),
  fullname text NOT NULL DEFAULT '',
  username text NOT NULL,
  email text NOT NULL,
  password text NOT NULL,
  metadata text NOT NULL
) WITH (OIDS=FALSE);

CREATE UNIQUE INDEX users_username_idx
  ON users USING btree (username);

CREATE UNIQUE INDEX users_email_idx
  ON users USING btree (email);

CREATE TRIGGER users_modified_at_tgr BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();


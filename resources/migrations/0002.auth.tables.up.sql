CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  modified_at timestamptz DEFAULT current_timestamp,
  photo text,
  username text,
  email text,
  password text
) WITH (OIDS=FALSE);

CREATE UNIQUE INDEX users_username_idx
  ON users USING btree (username);

CREATE UNIQUE INDEX users_email_idx
  ON users USING btree (email);

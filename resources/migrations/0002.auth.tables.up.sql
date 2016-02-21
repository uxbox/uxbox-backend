CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  username text,
  email text,
  password text
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS tokens (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  salt text,
  user_id uuid references users(id)
) WITH (OIDS=FALSE);

CREATE UNIQUE INDEX users_username_idx
  ON users USING btree (username);

CREATE UNIQUE INDEX users_email_idx
  ON users USING btree (email);

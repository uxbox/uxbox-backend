CREATE TABLE IF NOT EXISTS projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  modified_at timestamptz DEFAULT current_timestamp,
  name text NOT NULL,

  meta_width bigint NOT NULL,
  meta_height bigint NOT NULL,
  meta_layout text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS pages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  project uuid REFERENCES projects(id),
  name text NOT NULL,
  data text NOT NULL,

  meta_width bigint NOT NULL,
  meta_height bigint NOT NULL,
  meta_layout text NOT NULL,
) WITH (OIDS=FALSE);

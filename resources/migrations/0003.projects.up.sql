-- Table

CREATE TABLE IF NOT EXISTS projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  deleted boolean DEFAULT false,
  version bigint NOT NULL DEFAULT 0,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS projects_share (
  project uuid PRIMARY KEY REFERENCES projects(id),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  token text
) WITH (OIDS=FALSE);

-- Triggers

CREATE OR REPLACE FUNCTION handle_project_create()
  RETURNS TRIGGER AS $$
  DECLARE
    token text;
  BEGIN
    SELECT encode(digest(gen_random_bytes(128), 'sha256'), 'hex')
      INTO token;

    INSERT INTO projects_share (project, token)
    VALUES (NEW.id, token);

    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_project_delete()
  RETURNS TRIGGER AS $$
  BEGIN
    DELETE FROM pages WHERE project = OLD.id;
    RETURN OLD;
  END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER project_on_create_tgr
 AFTER INSERT ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_project_create();

CREATE TRIGGER project_on_delete_tgr
 BEFORE DELETE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_project_delete();

CREATE TRIGGER project_occ_tgr
 BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER projects_modified_at_tgr
 BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER projects_share_modified_at_tgr
 BEFORE UPDATE ON projects_share
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

-- Indexes

CREATE INDEX projects_deleted_idx
    ON projects(deleted)
 WHERE deleted = true;

CREATE INDEX projects_user_idx
    ON projects("user");

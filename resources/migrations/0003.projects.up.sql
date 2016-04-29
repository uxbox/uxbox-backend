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

-- Triggers

CREATE OR REPLACE FUNCTION handle_project_delete()
  RETURNS TRIGGER AS $projectdelete$
  BEGIN
    DELETE FROM pages WHERE project = OLD.id;
    RETURN OLD;
  END;
$projectdelete$ LANGUAGE plpgsql;

CREATE TRIGGER project_on_delete_tgr BEFORE DELETE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_project_delete();

CREATE TRIGGER project_occ_tgr BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER projects_modified_at_tgr BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

-- Indexes

CREATE INDEX deleted_projects_idx
  ON projects(deleted)
  WHERE deleted = true;

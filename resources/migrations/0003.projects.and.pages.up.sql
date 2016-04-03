CREATE TABLE IF NOT EXISTS projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  version bigint NOT NULL DEFAULT 0,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS pages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT clock_timestamp(),
  modified_at timestamptz DEFAULT clock_timestamp(),

  "user" uuid NOT NULL REFERENCES users(id),
  project uuid NOT NULL REFERENCES projects(id),
  name text NOT NULL,
  data text NOT NULL,
  options text NOT NULL,

  version bigint DEFAULT 0,

  width bigint NOT NULL,
  height bigint NOT NULL,
  layout text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS pages_history (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  page uuid NOT NULL REFERENCES pages(id),
  "user" uuid NOT NULL REFERENCES users(id),
  created_at timestamptz NOT NULL,

  pinned bool NOT NULL DEFAULT false,
  label text NOT NULL DEFAULT '',

  data text NOT NULL,
  version bigint NOT NULL DEFAULT 0
) WITH (OIDS=FALSE);

CREATE OR REPLACE FUNCTION handle_project_delete()
  RETURNS TRIGGER AS $projectdelete$
  BEGIN
    DELETE FROM pages WHERE project = OLD.id;
    RETURN OLD;
  END;
$projectdelete$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_page_delete()
  RETURNS TRIGGER AS $pagedelete$
  BEGIN
    --- Update projects modified_at attribute when a
    --- page of that project is modified.
    UPDATE projects SET modified_at = clock_timestamp()
      WHERE id = OLD.project;

    --- Delete all history entries if page is deleted.
    DELETE FROM pages_history WHERE page = OLD.id;
    RETURN OLD;
  END;
$pagedelete$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION handle_page_update()
  RETURNS TRIGGER AS $pagechange$
  BEGIN
    --- Update projects modified_at attribute when a
    --- page of that project is modified.
    UPDATE projects SET modified_at = clock_timestamp()
      WHERE id = OLD.project;

    --- Register a new history entry if the data
    --- property is changed.
    IF (OLD.data != NEW.data) THEN
      INSERT INTO pages_history (page, "user", created_at, data, version)
        VALUES (OLD.id, OLD."user", OLD.modified_at, OLD.data, OLD.version);
    END IF;

    RETURN NEW;
  END;
$pagechange$ LANGUAGE plpgsql;

-- Changes

CREATE TRIGGER project_on_delete_tgr BEFORE DELETE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_project_delete();

CREATE TRIGGER page_on_update_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_page_update();

CREATE TRIGGER page_on_delete_tgr BEFORE DELETE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_page_delete();

-- OCC

CREATE TRIGGER project_occ_tgr BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER page_occ_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

-- Modified at

CREATE TRIGGER projects_modified_at_tgr BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER pages_modified_at_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

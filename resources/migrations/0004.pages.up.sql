-- Tables

CREATE TABLE IF NOT EXISTS pages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  "user" uuid NOT NULL REFERENCES users(id),
  project uuid NOT NULL REFERENCES projects(id),
  name text NOT NULL,
  data bytea NOT NULL,
  options bytea NOT NULL,

  deleted boolean DEFAULT false,
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
  modified_at timestamptz NOT NULL,

  pinned bool NOT NULL DEFAULT false,
  label text NOT NULL DEFAULT '',

  data bytea NOT NULL,
  version bigint NOT NULL DEFAULT 0
) WITH (OIDS=FALSE);

-- Triggers

CREATE OR REPLACE FUNCTION handle_page_delete()
  RETURNS TRIGGER AS $pagedelete$
  BEGIN
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
      INSERT INTO pages_history (page, "user", created_at,
                                 modified_at, data, version)
        VALUES (OLD.id, OLD."user", OLD.modified_at,
                OLD.modified_at, OLD.data, OLD.version);
    END IF;

    RETURN NEW;
  END;
$pagechange$ LANGUAGE plpgsql;

CREATE TRIGGER page_on_update_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_page_update();

CREATE TRIGGER page_on_delete_tgr BEFORE DELETE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_page_delete();

CREATE TRIGGER page_occ_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER pages_modified_at_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER pages_history_modified_at_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

-- Indexes

CREATE INDEX deleted_pages_idx
  ON pages USING btree (deleted)
  WHERE deleted = true;

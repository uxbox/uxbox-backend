CREATE TABLE IF NOT EXISTS projects (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  modified_at timestamptz DEFAULT current_timestamp,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS pages (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz DEFAULT current_timestamp,
  modified_at timestamptz DEFAULT current_timestamp,

  "user" uuid NOT NULL REFERENCES users(id),
  project uuid NOT NULL REFERENCES projects(id),
  name text NOT NULL,
  data text,

  version bigint DEFAULT 0,

  width bigint NOT NULL,
  height bigint NOT NULL,
  layout text NOT NULL
) WITH (OIDS=FALSE);

CREATE TABLE IF NOT EXISTS pages_history (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  page uuid REFERENCES pages(id),
  created_at timestamptz,

  data text,
  version bigint DEFAULT 0
) WITH (OIDS=FALSE);

CREATE OR REPLACE FUNCTION insert_page_history()
  RETURNS TRIGGER AS $history$
  BEGIN
    IF (TG_OP = 'DELETE') THEN
      DELETE FROM pages_history WHERE page = OLD.id;
      RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
      INSERT INTO pages_history (page, created_at, data, version)
        VALUES (OLD.id, OLD.modified_at, OLD.data, OLD.version);

      NEW.version := OLD.version + 1;
      RETURN NEW;
    END IF;
    RETURN NULL; -- result is ignored since this is an AFTER trigger
  END;
$history$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_modified_at()
  RETURNS TRIGGER AS $updt$
  BEGIN
    NEW.modified_at := current_timestamp;
    RETURN NEW;
  END;
$updt$ LANGUAGE plpgsql;

CREATE TRIGGER pages_history_tgr BEFORE UPDATE OR DELETE ON pages
  FOR EACH ROW EXECUTE PROCEDURE insert_page_history();

CREATE TRIGGER projects_modified_at_tgr BEFORE UPDATE ON projects
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TRIGGER pages_modified_at_tgr BEFORE UPDATE ON pages
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

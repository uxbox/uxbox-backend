CREATE TABLE IF NOT EXISTS color_collections (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),

  version bigint NOT NULL DEFAULT 0,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL,
  data text NOT NULL
) WITH (OIDS=FALSE);

CREATE TRIGGER color_collections_occ_tgr BEFORE UPDATE ON color_collections
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER color_collections_modified_at_tgr BEFORE UPDATE ON color_collections
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

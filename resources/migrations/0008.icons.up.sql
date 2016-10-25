CREATE TABLE IF NOT EXISTS icon_collections (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  version bigint NOT NULL DEFAULT 0,

  "user" uuid REFERENCES users(id),
  name text NOT NULL,

  deleted boolean NOT NULL DEFAULT false
) WITH (OIDS=FALSE);

CREATE TRIGGER icon_collections_occ_tgr BEFORE UPDATE ON icon_collections
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER icon_collections_modified_at_tgr BEFORE UPDATE ON icon_collections
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TABLE IF NOT EXISTS icons (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  metadata bytea NOT NULL,
  name text NOT NULL,
  content text NOT NULL,

  version bigint NOT NULL DEFAULT 0,
  "user" uuid REFERENCES users(id),

  collection uuid REFERENCES icon_collections(id)
                  ON DELETE SET NULL
                  DEFAULT NULL,

  deleted boolean NOT NULL DEFAULT false
) WITH (OIDS=FALSE);

CREATE INDEX icons_collection_idx
    ON icons (collection);

CREATE TRIGGER icons_occ_tgr BEFORE UPDATE ON icons
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER icons_modified_at_tgr BEFORE UPDATE ON icons
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();


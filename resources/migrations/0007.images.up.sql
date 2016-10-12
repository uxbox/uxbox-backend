CREATE TABLE IF NOT EXISTS image_collections (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  version bigint NOT NULL DEFAULT 0,

  "user" uuid NOT NULL REFERENCES users(id),
  name text NOT NULL,

  deleted boolean NOT NULL DEFAULT false
) WITH (OIDS=FALSE);

CREATE TRIGGER image_collections_occ_tgr BEFORE UPDATE ON image_collections
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER image_collections_modified_at_tgr BEFORE UPDATE ON image_collections
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();

CREATE TABLE IF NOT EXISTS images (
  id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
  created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  modified_at timestamptz NOT NULL DEFAULT clock_timestamp(),
  deleted_at timestamptz DEFAULT NULL,

  width int NOT NULL,
  height int NOT NULL,
  mimetype text NOT NULL,

  version bigint NOT NULL DEFAULT 0,
  "user" uuid NOT NULL REFERENCES users(id),

  collection uuid REFERENCES image_collections(id)
                  ON DELETE SET NULL
                  DEFAULT NULL,

  name text NOT NULL,
  path text NOT NULL,

  deleted boolean NOT NULL DEFAULT false
) WITH (OIDS=FALSE);

CREATE INDEX images_collection_idx
    ON images (collection);

CREATE TRIGGER images_occ_tgr BEFORE UPDATE ON images
  FOR EACH ROW EXECUTE PROCEDURE handle_occ();

CREATE TRIGGER images_modified_at_tgr BEFORE UPDATE ON images
  FOR EACH ROW EXECUTE PROCEDURE update_modified_at();


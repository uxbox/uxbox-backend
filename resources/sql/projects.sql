-- :name create-project :<! :1
-- :doc Create new project entry.
insert into projects (id, "user", name)
  values (:id, :user, :name)
  returning *;

-- :name update-project :<! :1
-- :doc Update a existing project entry.
update projects
  set name = :name, version = :version
  where id = :id AND
        "user" = :user AND
        deleted = false
  returning *;

-- :name delete-project :! :n
-- :doc Delete specified project
update projects
  set deleted = true, deleted_at = clock_timestamp()
  where id = :id AND "user" = :user AND deleted = false;

-- :name get-projects :? :*
-- :doc Get the project list with page counter (by user)
select pr.*, count(pg.id) as total_pages
  from projects as pr
  left outer join pages as pg
  on pg.project = pr.id
  where pr.user = :user AND
        pr.deleted = false
  group by pr.id
  order by pr.created_at desc;

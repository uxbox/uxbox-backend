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

-- :name get-project-by-id :? :1
select p.*
  from projects as p
 where p.id = :id;

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

-- :name get-project-by-share-token :? :*
select p.*
  from projects as p
  inner join project_shares as ps
          on (p.id = ps.project)
  where ps.token = :token;

-- :name get-share-tokens-for-project
select s.*
  from project_shares as s
 where s.project = :project
 order by s.created_at desc;

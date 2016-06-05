-- :name create-page :<! :1
-- :doc Insert new page entry in the database.
insert into pages (id, "user", project, name, width,
                   height, layout, data, options)
values (:id, :user, :project, :name, :width,
        :height, :layout, :data, :options)
returning *;

-- :name update-page :<! :1
-- :doc Update the page entry by id and user.
update pages set name = :name,
                 width = :width,
                 height = :height,
                 layout = :layout,
                 data = :data,
                 version = :version,
                 options = :options
  where id = :id and "user" = :user and deleted = false
  returning *;

-- :name update-page-metadata :<! :1
-- :doc A limited version of update-page function.
update pages set name = :name,
                 width = :width,
                 height = :height,
                 layout = :layout,
                 version = :version,
                 options = :options
  where id = :id and "user" = :user and deleted = false
  returning *;

-- :name delete-page :! :n
-- :doc Delete page entry.
update pages set deleted = true,
                 deleted_at = clock_timestamp()
  where id = :id and "user" = :user and deleted = false;

-- :name get-pages :? :*
-- :doc Retrieve pages by user.
select pg.* from pages as pg
  where pg.user = :user and pg.deleted = false
  order by created_at asc;

-- :name get-page-by-id :? :1
-- :doc Retrieve page by id
select pg.* from pages as pg where id = :id and deleted = false;

-- :name get-pages-for-user-and-project :? :*
-- :doc Retrieve pages by user.
select pg.* from pages as pg
  where pg.user = :user and
        pg.project = :project and
        pg.deleted = false
  order by created_at asc;

-- :name get-pages-for-project :? :*
-- :doc Retrieve pages by user.
select pg.* from pages as pg
  where pg.project = :project and
        pg.deleted = false
  order by created_at asc;

-- :name create-page-history :! :n
insert into page_history (id, "user", page, pinned, label, data, version);
values (:id, :user, :page, :pinned :label, :data, :version);

-- :name get-page-history :? :*
-- :doc Retrieve page history.
select ph.* from pages_history as ph
  where ph.user = :user and
        ph.page = :page and
        ph.version < :since
--~ (when (:pinned params) "and ph.pinned = true")
  order by ph.version desc
  limit :max;

-- :name get-page-history-for-project :? :*
-- :doc Retrieve page history for the entire project
select ph.* from pages_history as ph
 inner join pages as p on (p.id = ph.page)
 where p.project = :project;

-- :name update-page-history :? :*
-- :doc Update page history entry by user.
update pages_history
  set label = :label, pinned = :pinned
  where id = :id and "user" = :user
  returning *;

-- :name create-icon-collection :<! :1
insert into icon_collections (id, "user", name)
values (:id, :user, :name)
returning *;

-- :name update-icon-collection :<! :1
update icon_collections
   set name = :name,
       version = :version
 where id = :id
   and "user" = :user
returning *;

-- :name get-icon-collections :? :*
select *,
       (select count(*) from icons where collection = ic.id) as num_icons
  from icon_collections as ic
 where ic."user" = :user
   and ic.deleted = false
 order by ic.created_at desc;

-- :name delete-icon-collection :! :n
update icon_collections
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

-- :name get-icons-by-collection :? :*
select *
  from icons as i
 where i."user" = :user
   and i.deleted = false
   and i."collection" = :collection
 order by i.created_at desc;

-- :name get-icons :? :*
select * from icons
 where "user" = :user and deleted = false and "collection" is null
order by created_at desc;

-- :name create-icon :<! :1
insert into icons ("user", name, collection, metadata, content)
values (:user, :name, :collection, :metadata, :content)
returning *;

-- :name update-icon :<! :1
update icons
   set name = :name,
       collection = :collection,
       version = :version
 where id = :id and "user" = :user
returning *;

-- :name delete-icon :! :n
update icons
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

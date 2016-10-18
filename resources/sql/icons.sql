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
select * from icon_collections
 where "user" = :user and deleted = false
order by created_at desc;

-- :name delete-icon-collection :! :n
update icon_collections
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

-- :name get-icons :? :*
select * from icons
 where "user" = :user and deleted = false and "collection" = :collection
order by created_at desc;

-- :name get-icon :? :1
select * from icons
 where "user" = :user and id = :id and deleted = false;

-- :name create-icon :<! :1
insert into icons ("user", name, collection, metadata, content)
values (:user, :name, :collection, :metadata, :content)
returning *;

-- :name update-icon :<! :1
update icons
   set name = :name,
       version = :version
 where id = :id and "user" = :user
returning *;

-- :name delete-icon :! :n
update icons
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

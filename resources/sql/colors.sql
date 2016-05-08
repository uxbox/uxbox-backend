-- :name create-color-collection :<! :1
insert into color_collections (id, "user", name, data)
values (:id, :user, :name, :data)
returning *;

-- :name update-color-collection :<! :1
update color_collections
   set name = :name,
       version = :version,
       data = :data
 where id = :id
   and "user" = :user
returning *;

-- :name get-color-collections :? :*
select * from color_collections
 where "user" = :user and deleted = false
order by created_at desc;

-- :name delete-color-collection :! :n
update color_collections
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

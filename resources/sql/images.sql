-- :name create-image-collection :<! :1
insert into image_collections (id, "user", name)
values (:id, :user, :name)
returning *;

-- :name update-image-collection :<! :1
update image_collections
   set name = :name,
       version = :version
 where id = :id
   and "user" = :user
returning *;

-- :name get-image-collections :? :*
select * from image_collections
 where "user" = :user and deleted = false
order by created_at desc;

-- :name delete-image-collection :! :n
update image_collections
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

-- :name create-image :<! :1
insert into images ("user", name, collection, path)
values (:user, :name, :collection, :path)
returning *;

-- :name update-image :<! :1
update images
   set name = :name,
       version = :version
 where id = :id and "user" = :user
returning *;

-- :name delete-image :! :n
update images
   set deleted = true,
       deleted_at = clock_timestamp()
 where id = :id and "user" = :user;

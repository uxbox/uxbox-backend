-- :name create-profile :<! :1
insert into users (id, fullname, username, email, password, metadata, photo)
values (:id, :fullname, :username, :email, :password, :metadata, '')
returning *;

-- :name get-profile :? :1
select * from users
 where id = :id and deleted = false;

-- :name get-profile-by-username :? :1
select * from users
 where (username = :username or email = :username)
   and deleted = false;

-- :name update-profile :<! :1
update users
   set username = :username,
       email = :email,
       fullname = :fullname,
       metadata = :metadata
 where id = :id and deleted = false
returning *;

-- :name update-profile-password :! :n
update users set password = :password
 where id = :id and deleted = false;

-- :name update-profile-photo :! :n
update users set photo = :photo
 where id = :id and deleted = false;


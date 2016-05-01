-- :name create-profile :<! :1
-- :doc Create a new user entry.
insert into users (id, fullname, username, email, password, metadata)
  values (:id, :fullname, :username, :email, :password, :metadata)
  returning *;

-- :name get-profile :? :1
-- :doc Retrieve the profile data.
select * from users
  where id = :id and deleted = false;

-- :name get-profile-by-username :? :1
-- :doc Retrieve the profile data.
select * from users
  where (username = :username or email = :username)
        and deleted = false;

-- :name update-profile :<! :1
-- :doc Update profile.
update users set username = :username,
                 email = :email,
                 fullname = :fullname,
                 metadata = :metadata
  where id = :id and deleted = false
  returning *;

-- :name update-profile-password :! :n
-- :doc Update profile password
update users set password = :password
  where id = :id and deleted = false;

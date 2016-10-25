-- :name get-color-collection :? :1
select *
  from color_collections as cc
 where cc.id = :id and cc."user" is null;


create table feeds(
    id uuid not null primary key,
    nano_id char(18) not null,
    title varchar(255) not null,
    url varchar(255) not null,
    synchronized_at timestamp
);

create index nanoid_idx ON feeds using hash (nano_id);

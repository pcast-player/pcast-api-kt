create table feeds(
    id uuid not null primary key,
    title varchar(255) not null,
    url varchar(255) not null,
    synchronized_at timestamp
);

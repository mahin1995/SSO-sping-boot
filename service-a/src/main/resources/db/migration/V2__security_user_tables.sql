create table if not exists users (
    username varchar(50) primary key,
    password varchar(500) not null,
    enabled boolean not null
);

create table if not exists authorities (
    username varchar(50) not null,
    authority varchar(100) not null,
    constraint fk_authorities_users
        foreign key (username) references users (username)
            on delete cascade
);

create unique index if not exists ix_auth_username on authorities (username, authority);


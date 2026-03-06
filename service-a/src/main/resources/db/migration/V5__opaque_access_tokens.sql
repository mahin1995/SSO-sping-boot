create table if not exists opaque_access_tokens (
    token_hash varchar(64) primary key,
    subject varchar(200) not null,
    scope varchar(2000) not null,
    audience varchar(1000) not null,
    issuer varchar(200) not null,
    token_id varchar(100) not null,
    issued_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    active boolean not null default true
);

create index if not exists ix_opaque_access_tokens_expires_at on opaque_access_tokens (expires_at);
create index if not exists ix_opaque_access_tokens_active on opaque_access_tokens (active);

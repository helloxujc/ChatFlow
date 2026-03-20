-- PostgreSQL

-- Drop existing tables if re-running setup
DROP TABLE IF EXISTS user_room_activity;
DROP TABLE IF EXISTS messages;

-- Primary message store
CREATE TABLE messages (
    message_id       UUID         PRIMARY KEY,
    room_id          VARCHAR(20)  NOT NULL,
    user_id          VARCHAR(20)  NOT NULL,
    username         VARCHAR(20)  NOT NULL,
    message          TEXT         NOT NULL,
    message_type     VARCHAR(10)  NOT NULL,
    client_timestamp TIMESTAMPTZ  NOT NULL,
    server_id        VARCHAR(50),
    client_ip        VARCHAR(50)
);

-- Indexes covering all four core queries
CREATE INDEX idx_msg_room_time ON messages (room_id, client_timestamp);  -- Query 1: room + time range
CREATE INDEX idx_msg_user_time ON messages (user_id, client_timestamp);  -- Query 2: user history
CREATE INDEX idx_msg_time      ON messages (client_timestamp);           -- Query 3: count active users

-- Derived table: rooms a user has participated in (upsert target)
-- Answers Query 4 "get rooms user has participated in" via primary key lookup
CREATE TABLE user_room_activity (
    user_id       VARCHAR(20)  NOT NULL,
    room_id       VARCHAR(20)  NOT NULL,
    last_activity TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (user_id, room_id)
);

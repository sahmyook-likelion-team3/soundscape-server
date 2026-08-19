-- ============================================================
-- SoundScape 스키마 DDL (MySQL 8.x)
-- ============================================================
CREATE DATABASE IF NOT EXISTS soundscape
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE soundscape;

DROP TABLE IF EXISTS playlist_music;
DROP TABLE IF EXISTS music_theme;
DROP TABLE IF EXISTS playlist;
DROP TABLE IF EXISTS music;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    created_at     DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE music (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    title             VARCHAR(200)  NOT NULL,
    artist            VARCHAR(200)  NOT NULL,
    audio_object_key  VARCHAR(500)  NOT NULL,
    genre             VARCHAR(40)   NOT NULL,
    brightness        DECIMAL(4,3)  NOT NULL,
    energy            DECIMAL(4,3)  NOT NULL,
    calmness          DECIMAL(4,3)  NOT NULL,
    warmth            DECIMAL(4,3)  NOT NULL,
    positivity        DECIMAL(4,3)  NOT NULL,
    dreaminess        DECIMAL(4,3)  NOT NULL,
    duration_seconds  INT           NOT NULL,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_music_genre (genre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE music_theme (
    music_id  BIGINT      NOT NULL,
    theme     VARCHAR(40) NOT NULL,
    PRIMARY KEY (music_id, theme),
    CONSTRAINT fk_music_theme_music FOREIGN KEY (music_id) REFERENCES music (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    user_id           BIGINT        NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    source_type       VARCHAR(20)   NOT NULL,
    source_genre      VARCHAR(40)   NULL,
    cover_object_key  VARCHAR(500)  NULL,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    PRIMARY KEY (id),
    KEY idx_playlist_user (user_id, created_at),
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE playlist_music (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    playlist_id  BIGINT NOT NULL,
    music_id     BIGINT NOT NULL,
    `position`   INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_position (playlist_id, `position`),
    CONSTRAINT fk_pm_playlist FOREIGN KEY (playlist_id) REFERENCES playlist (id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_music    FOREIGN KEY (music_id)    REFERENCES music (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SoundScape 시드 데이터 (users 1명 + music 26곡 + music_theme 다중매핑)
-- 실행 순서: schema.sql -> seed.sql
-- ============================================================
USE soundscape;

-- [0. users 시드] X-User-Id: 1 임시 인증용 데모 계정. 없으면 플리 저장 시 FK 제약 위반 발생.
-- username: demo_user / password: demo1234
INSERT INTO users (id, username, password_hash, nickname, created_at)
VALUES (1, 'demo_user', '$2b$10$GUksG5juSaJ2qv29D8Dqb.pWs0h/jX05T9gvcS01wD5UZC6g6yBtK', 'demo_user', NOW());

-- [1. music 테이블 시드 데이터 26곡]
INSERT INTO music (id, title, artist, audio_object_key, genre, brightness, energy, calmness, warmth, positivity, dreaminess, duration_seconds, created_at, updated_at) VALUES
(1, 'Ask', 'lofizoo_music', 'music/ask.mp3', 'LOFI_INSTRUMENTAL', 0.45, 0.35, 0.85, 0.70, 0.50, 0.75, 124, NOW(), NOW()),
(2, 'Smooth', 'lofizoo_music', 'music/smooth.mp3', 'LOFI_INSTRUMENTAL', 0.48, 0.31, 0.83, 0.72, 0.54, 0.79, 122, NOW(), NOW()),
(3, 'Dark', 'lofizoo_music', 'music/dark.mp3', 'LOFI_INSTRUMENTAL', 0.18, 0.24, 0.89, 0.37, 0.28, 0.92, 75, NOW(), NOW()),
(4, 'BLACK BOX - Chill 2', 'BLACKBOX', 'music/blackbox_chill2.mp3', 'LOFI_INSTRUMENTAL', 0.52, 0.28, 0.86, 0.81, 0.58, 0.74, 88, NOW(), NOW()),
(5, 'Study Music Session', 'alex-morgan', 'music/study_session.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.58, 0.34, 0.88, 0.76, 0.63, 0.42, 181, NOW(), NOW()),
(6, 'Ambient Background Soft Air Drift', 'alex-morgan', 'music/soft_air_drift.mp3', 'LOFI_INSTRUMENTAL', 0.38, 0.12, 0.96, 0.64, 0.48, 0.84, 190, NOW(), NOW()),
(7, 'Geography Study', 'The_Mountain', 'music/geography_study.mp3', 'LOFI_INSTRUMENTAL', 0.61, 0.52, 0.67, 0.43, 0.68, 0.34, 127, NOW(), NOW()),
(8, 'Calm Ambient', 'Kulakovka', 'music/calm_ambient.mp3', 'LOFI_INSTRUMENTAL', 0.64, 0.18, 0.94, 0.86, 0.71, 0.78, 135, NOW(), NOW()),
(9, 'Study Vibe', 'The_Mountain', 'music/study_vibe.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.59, 0.27, 0.91, 0.84, 0.66, 0.72, 134, NOW(), NOW()),
(10, 'Ambient Forest Rain', 'NourishedByMusic', 'music/forest_rain.mp3', 'LOFI_INSTRUMENTAL', 0.32, 0.14, 0.97, 0.58, 0.64, 0.89, 294, NOW(), NOW()),
(11, 'Sparkling Glasses and Nocturnal Tunes', 'HarumachiMusic', 'music/nocturnal_tunes.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.62, 0.38, 0.87, 0.89, 0.74, 0.68, 171, NOW(), NOW()),
(12, 'Think about yesterday', 'HarumachiMusic', 'music/think_yesterday.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.53, 0.21, 0.93, 0.92, 0.69, 0.76, 198, NOW(), NOW()),
(13, 'Relaxing Music Coffee Shop', 'alex-morgan', 'music/relaxing_coffee_shop.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.63, 0.32, 0.89, 0.84, 0.72, 0.52, 160, NOW(), NOW()),
(14, 'Relax - Relaxing Background', 'andriig', 'music/relax_background.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.56, 0.19, 0.94, 0.90, 0.73, 0.78, 139, NOW(), NOW()),
(15, 'Acoustic Guitar Sunrise Travel', 'alex-morgan', 'music/sunrise_travel.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.78, 0.44, 0.81, 0.86, 0.84, 0.61, 149, NOW(), NOW()),
(16, 'Whisper of the Breeze', 'HirohaSaimoto', 'music/whisper_breeze.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.71, 0.23, 0.92, 0.87, 0.78, 0.73, 213, NOW(), NOW()),
(17, 'Whisper of the Pines', 'HirohaSaimoto', 'music/whisper_pines.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.76, 0.37, 0.88, 0.85, 0.83, 0.66, 240, NOW(), NOW()),
(18, 'feel the breeze', 'HarumachiMusic', 'music/feel_breeze.mp3', 'JAZZ_CLASSICAL_NEWAGE', 0.74, 0.42, 0.83, 0.86, 0.81, 0.63, 153, NOW(), NOW()),
(19, 'Dusty Roads', 'FreeMusicLab', 'music/dusty_roads.mp3', 'POP', 0.82, 0.84, 0.21, 0.63, 0.86, 0.32, 112, NOW(), NOW()),
(20, 'I TRAVEL', 'AlisiaBeats', 'music/i_travel.mp3', 'POP', 0.88, 0.79, 0.26, 0.68, 0.89, 0.41, 103, NOW(), NOW()),
(21, 'American Trip', 'Sonican', 'music/american_trip.mp3', 'POP', 0.83, 0.66, 0.42, 0.84, 0.87, 0.48, 62, NOW(), NOW()),
(22, 'Traveling Colours', 'Oleg-Mazur', 'music/traveling_colours.mp3', 'POP', 0.79, 0.72, 0.35, 0.62, 0.84, 0.56, 190, NOW(), NOW()),
(23, 'BLACK BOX - Vlog 1', 'BLACKBOX', 'music/blackbox_vlog1.mp3', 'POP', 0.91, 0.64, 0.48, 0.79, 0.92, 0.38, 130, NOW(), NOW()),
(24, 'Acoustic Indie Folk', 'Markjuly', 'music/indie_folk.mp3', 'POP', 0.85, 0.68, 0.51, 0.89, 0.88, 0.44, 131, NOW(), NOW()),
(25, 'Lazy Summer Days', 'HoggyArt', 'music/summer_days.mp3', 'POP', 0.81, 0.58, 0.62, 0.82, 0.86, 0.53, 127, NOW(), NOW()),
(26, 'Inspire Indie Acoustic', 'ONALDIN_Music', 'music/indie_acoustic.mp3', 'POP', 0.87, 0.73, 0.46, 0.86, 0.91, 0.52, 158, NOW(), NOW());

-- [2. music_theme 테이블 시드 데이터 다중 테마 매핑]
INSERT INTO music_theme (music_id, theme) VALUES
(1, 'STUDY_FOCUS'), (1, 'REST_HEALING'), (1, 'NIGHT_DREAMY'),
(2, 'STUDY_FOCUS'), (2, 'REST_HEALING'), (2, 'NIGHT_DREAMY'),
(3, 'NIGHT_DREAMY'), (3, 'RAINY_DAY'), (3, 'STUDY_FOCUS'),
(4, 'REST_HEALING'), (4, 'STUDY_FOCUS'), (4, 'RAINY_DAY'),
(5, 'STUDY_FOCUS'), (5, 'REST_HEALING'),
(6, 'STUDY_FOCUS'), (6, 'REST_HEALING'), (6, 'NIGHT_DREAMY'),
(7, 'STUDY_FOCUS'), (7, 'BRIGHT_DAY'),
(8, 'REST_HEALING'), (8, 'STUDY_FOCUS'), (8, 'BRIGHT_DAY'),
(9, 'STUDY_FOCUS'), (9, 'REST_HEALING'), (9, 'NIGHT_DREAMY'),
(10, 'RAINY_DAY'), (10, 'STUDY_FOCUS'), (10, 'REST_HEALING'),
(11, 'REST_HEALING'), (11, 'NIGHT_DREAMY'), (11, 'STUDY_FOCUS'),
(12, 'REST_HEALING'), (12, 'WALK_DRIVE_TRAVEL'), (12, 'NIGHT_DREAMY'),
(13, 'REST_HEALING'), (13, 'STUDY_FOCUS'), (13, 'BRIGHT_DAY'),
(14, 'REST_HEALING'), (14, 'WALK_DRIVE_TRAVEL'), (14, 'NIGHT_DREAMY'),
(15, 'REST_HEALING'), (15, 'WALK_DRIVE_TRAVEL'), (15, 'BRIGHT_DAY'),
(16, 'REST_HEALING'), (16, 'WALK_DRIVE_TRAVEL'), (16, 'BRIGHT_DAY'),
(17, 'REST_HEALING'), (17, 'WALK_DRIVE_TRAVEL'), (17, 'BRIGHT_DAY'),
(18, 'REST_HEALING'), (18, 'WALK_DRIVE_TRAVEL'), (18, 'BRIGHT_DAY'),
(19, 'WALK_DRIVE_TRAVEL'), (19, 'PARTY_ENERGY'), (19, 'BRIGHT_DAY'),
(20, 'WALK_DRIVE_TRAVEL'), (20, 'BRIGHT_DAY'), (20, 'PARTY_ENERGY'),
(21, 'WALK_DRIVE_TRAVEL'), (21, 'BRIGHT_DAY'), (21, 'REST_HEALING'),
(22, 'WALK_DRIVE_TRAVEL'), (22, 'BRIGHT_DAY'),
(23, 'WALK_DRIVE_TRAVEL'), (23, 'BRIGHT_DAY'), (23, 'REST_HEALING'),
(24, 'WALK_DRIVE_TRAVEL'), (24, 'BRIGHT_DAY'), (24, 'REST_HEALING'),
(25, 'WALK_DRIVE_TRAVEL'), (25, 'BRIGHT_DAY'), (25, 'REST_HEALING'),
(26, 'WALK_DRIVE_TRAVEL'), (26, 'BRIGHT_DAY'), (26, 'REST_HEALING');

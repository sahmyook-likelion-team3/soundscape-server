package com.soundscape.dto;

import com.soundscape.domain.Music;
import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;

import java.util.Set;

/** 곡 단건 응답. audioUrl 은 S3 Presigned URL(기본 60분). */
public record MusicResponse(
        Long musicId,
        String title,
        String artist,
        Genre genre,
        int durationSeconds,
        Set<PlaylistTheme> themes,
        String audioUrl,
        Double score,
        int position
) {
    public static MusicResponse of(Music music, String audioUrl, Double score, int position) {
        return new MusicResponse(
                music.getId(),
                music.getTitle(),
                music.getArtist(),
                music.getGenre(),
                music.getDurationSeconds(),
                music.getThemes(),
                audioUrl,
                score,
                position
        );
    }
}

package com.soundscape.dto;

import com.soundscape.domain.Playlist;
import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;

/** GET /playlists 목록 항목. coverImageUrl 은 S3 Presigned URL. */
public record PlaylistSummaryResponse(
        Long playlistId,
        String title,
        SourceType sourceType,
        Genre sourceGenre,
        String coverImageUrl,
        String createdAt
) {
    public static PlaylistSummaryResponse of(Playlist playlist, String coverImageUrl) {
        return new PlaylistSummaryResponse(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getSourceType(),
                playlist.getSourceGenre(),
                coverImageUrl,
                playlist.getCreatedAt().toString()
        );
    }
}

package com.soundscape.dto;

import com.soundscape.domain.Playlist;
import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;

import java.util.List;

/** GET /playlists/{playlistId} 상세. 조회 시점에 오디오 Presigned URL 을 재발급한다. */
public record PlaylistDetailResponse(
        Long playlistId,
        String title,
        SourceType sourceType,
        Genre sourceGenre,
        String coverImageUrl,
        String createdAt,
        List<MusicResponse> musicList
) {
    public static PlaylistDetailResponse of(Playlist playlist, String coverImageUrl, List<MusicResponse> musicList) {
        return new PlaylistDetailResponse(
                playlist.getId(),
                playlist.getTitle(),
                playlist.getSourceType(),
                playlist.getSourceGenre(),
                coverImageUrl,
                playlist.getCreatedAt().toString(),
                musicList
        );
    }
}

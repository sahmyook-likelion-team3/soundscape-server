package com.soundscape.dto;

import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.enums.SourceType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 추천 결과 응답. 저장 전 상태이므로 playlistId 는 없다.
 * 현재는 사진 기반 추천만 지원하므로 sourceType 은 항상 PHOTO_RECOMMENDATION, sourceGenre 는 항상 null 이다.
 */
public record RecommendationResponse(
        String title,
        SourceType sourceType,
        Genre sourceGenre,
        PhotoAnalysisResponse analysis,
        Set<PlaylistTheme> convertedThemes,
        List<MusicResponse> musicList
) {
    public static RecommendationResponse photo(PhotoAnalysisResult mood, List<PlaylistTheme> themes, List<MusicResponse> musics) {
        return new RecommendationResponse(
                mood.title(), SourceType.PHOTO_RECOMMENDATION, null,
                PhotoAnalysisResponse.of(mood), new LinkedHashSet<>(themes), musics
        );
    }
}

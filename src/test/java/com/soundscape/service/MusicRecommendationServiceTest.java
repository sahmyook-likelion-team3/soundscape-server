package com.soundscape.service;

import com.soundscape.enums.PlaylistTheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 정규화 수식 검증. 캐시 의존 없이 순수 계산만 테스트한다. */
class MusicRecommendationServiceTest {

    private final MusicRecommendationService service = new MusicRecommendationService(null);

    @Test
    @DisplayName("완전 일치 시 moodScore = 1.0")
    void moodScore_identical() {
        double[] v = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
        assertEquals(1.0, service.moodScore(v, v), 1e-9);
    }

    @Test
    @DisplayName("최대 거리(모든 축 0 vs 1) 시 moodScore = 0.0")
    void moodScore_maxDistance() {
        double[] zero = {0, 0, 0, 0, 0, 0};
        double[] one = {1, 1, 1, 1, 1, 1};
        assertEquals(0.0, service.moodScore(zero, one), 1e-9);
    }

    @Test
    @DisplayName("moodScore는 항상 0.0 ~ 1.0 범위")
    void moodScore_range() {
        double[] a = {0.1, 0.9, 0.2, 0.8, 0.3, 0.7};
        double[] b = {0.9, 0.1, 0.8, 0.2, 0.7, 0.3};
        double score = service.moodScore(a, b);
        assertTrue(score >= 0.0 && score <= 1.0);
    }

    @Test
    @DisplayName("themeScore = 교집합 수 / 사진 테마 수")
    void themeScore_ratio() {
        Set<PlaylistTheme> photo = Set.of(PlaylistTheme.RAINY_DAY, PlaylistTheme.REST_HEALING);
        Set<PlaylistTheme> music = Set.of(PlaylistTheme.RAINY_DAY, PlaylistTheme.STUDY_FOCUS);
        assertEquals(0.5, service.themeScore(photo, music), 1e-9);
    }

    @Test
    @DisplayName("테마가 비어 있으면 themeScore = 0")
    void themeScore_empty() {
        assertEquals(0.0, service.themeScore(Set.of(), Set.of(PlaylistTheme.RAINY_DAY)), 1e-9);
    }
}

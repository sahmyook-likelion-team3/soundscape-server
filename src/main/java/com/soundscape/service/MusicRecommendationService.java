package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.dto.PhotoAnalysisResult;
import com.soundscape.enums.PlaylistTheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 가중치 기반 추천 알고리즘.
 *
 *   Final Score = (moodScore * 0.70) + (themeScore * 0.30)
 *
 *   moodScore  : 사진 6대 감정 벡터와 곡 6대 감정 벡터의 유클리드 거리 d 를
 *                최대 거리 sqrt(6) 으로 나눠 정규화한 유사도. moodScore = 1 - (d / sqrt(6)).
 *                결과는 항상 0.0 ~ 1.0 이며 거리 0 에서의 무한대 폭주가 발생하지 않는다.
 *   themeScore : 사진 분석 테마와 곡 테마의 교집합 비율. (매칭 수 / 사진 테마 수)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicRecommendationService {

    /** 6차원 단위 초입방체의 최대 유클리드 거리 = sqrt(6) ≈ 2.4495 */
    private static final double MAX_DISTANCE = Math.sqrt(Music.MOOD_DIMENSION);

    private static final double MOOD_WEIGHT = 0.70;
    private static final double THEME_WEIGHT = 0.30;

    /** 추천 곡 수 */
    public static final int RESULT_SIZE = 5;

    /**
     * 동일 아티스트 최대 허용 곡 수. 현재는 RESULT_SIZE 와 동일해 사실상 비활성(가중치 2항 정책).
     * 음원 추가 수급 후 아티스트 편중이 보이면 이 값만 2로 낮추면 즉시 적용된다.
     */
    private static final int MAX_PER_ARTIST = RESULT_SIZE;

    private final MusicCacheService musicCache;

    /** 사진 분석 결과 + PhotoThemeScoringService 가 변환한 테마 기반 상위 5곡 선별. */
    public List<Scored> recommendByPhoto(PhotoAnalysisResult analysis, List<PlaylistTheme> photoThemeList) {
        double[] photoVector = analysis.moodVector();
        Set<PlaylistTheme> photoThemes = new LinkedHashSet<>(photoThemeList);

        List<Scored> ranked = musicCache.findAll().stream()
                .map(music -> new Scored(music, finalScore(photoVector, photoThemes, music)))
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(s -> s.music().getId()))
                .toList();

        return pickWithArtistCap(ranked);
    }

    private double finalScore(double[] photoVector, Set<PlaylistTheme> photoThemes, Music music) {
        return moodScore(photoVector, music.moodVector()) * MOOD_WEIGHT
                + themeScore(photoThemes, music.getThemes()) * THEME_WEIGHT;
    }

    /** moodScore = 1 - (d / sqrt(6)) */
    double moodScore(double[] photoVector, double[] musicVector) {
        double sumOfSquares = 0.0;
        for (int i = 0; i < Music.MOOD_DIMENSION; i++) {
            double diff = photoVector[i] - musicVector[i];
            sumOfSquares += diff * diff;
        }
        double distance = Math.sqrt(sumOfSquares);
        return 1.0 - (distance / MAX_DISTANCE);
    }

    /** themeScore = 교집합 수 / 사진 테마 수 */
    double themeScore(Set<PlaylistTheme> photoThemes, Set<PlaylistTheme> musicThemes) {
        if (photoThemes.isEmpty() || musicThemes.isEmpty()) {
            return 0.0;
        }
        long matched = photoThemes.stream().filter(musicThemes::contains).count();
        return (double) matched / photoThemes.size();
    }

    private List<Scored> pickWithArtistCap(List<Scored> ranked) {
        List<Scored> selected = new ArrayList<>(RESULT_SIZE);
        java.util.Map<String, Integer> artistCount = new java.util.HashMap<>();

        for (Scored candidate : ranked) {
            if (selected.size() == RESULT_SIZE) {
                break;
            }
            String artist = candidate.music().getArtist();
            int count = artistCount.getOrDefault(artist, 0);
            if (count >= MAX_PER_ARTIST) {
                continue;
            }
            selected.add(candidate);
            artistCount.put(artist, count + 1);
        }
        return selected;
    }

    /** 곡 + 최종 점수. */
    public record Scored(Music music, Double score) {
    }
}

package com.soundscape.service;

import com.soundscape.dto.PhotoAnalysisResult;
import com.soundscape.enums.ActivityType;
import com.soundscape.enums.PlaceType;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.enums.TimeType;
import com.soundscape.enums.WeatherType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 사진 분석 결과(활동/장소/날씨/시간대/분위기)를 고정 규칙표로 채점해 PlaylistTheme 최대 3개로 변환한다.
 * 점수 규칙과 선택 기준은 팀원이 사전에 정의한 변환 규칙표를 그대로 구현한 것이다.
 *
 * 동점 처리: PlaylistTheme enum 선언 순서(STUDY_FOCUS, REST_HEALING, WALK_DRIVE_TRAVEL,
 * RAINY_DAY, NIGHT_DREAMY, BRIGHT_DAY, PARTY_ENERGY)를 우선순위로 사용한다.
 * 0.5점 이상인 테마가 하나도 없으면, 최고점이 0보다 클 때만 그 테마 1개를 선택하고
 * 전부 0점이면 REST_HEALING 을 기본값으로 사용한다.
 */
@Service
public class PhotoThemeScoringService {

    private static final double SELECTION_THRESHOLD = 0.5;
    private static final int MAX_THEMES = 3;
    private static final double ACTIVITY_SCORE = 1.0;
    private static final double MOOD_BONUS = 0.2;

    private static final Map<ActivityType, PlaylistTheme> ACTIVITY_THEME = Map.of(
            ActivityType.STUDY_WORK, PlaylistTheme.STUDY_FOCUS,
            ActivityType.REST_HEALING, PlaylistTheme.REST_HEALING,
            ActivityType.WALK_DRIVE_TRAVEL, PlaylistTheme.WALK_DRIVE_TRAVEL,
            ActivityType.SOCIAL_PARTY, PlaylistTheme.PARTY_ENERGY
    );

    private static final Map<WeatherType, PlaylistTheme> WEATHER_THEME = Map.of(
            WeatherType.RAINY, PlaylistTheme.RAINY_DAY,
            WeatherType.SUNNY, PlaylistTheme.BRIGHT_DAY
    );
    private static final Map<WeatherType, Double> WEATHER_SCORE = Map.of(
            WeatherType.RAINY, 0.8,
            WeatherType.SUNNY, 0.5
    );

    private static final Map<TimeType, PlaylistTheme> TIME_THEME = Map.of(
            TimeType.NIGHT, PlaylistTheme.NIGHT_DREAMY,
            TimeType.DAY, PlaylistTheme.BRIGHT_DAY
    );
    private static final Map<TimeType, Double> TIME_SCORE = Map.of(
            TimeType.NIGHT, 0.7,
            TimeType.DAY, 0.3
    );

    private static final Map<PlaceType, PlaylistTheme> PLACE_THEME = Map.of(
            PlaceType.CAFE_LIBRARY, PlaylistTheme.STUDY_FOCUS,
            PlaceType.ROOM_HOME, PlaylistTheme.REST_HEALING,
            PlaceType.TRANSPORT, PlaylistTheme.WALK_DRIVE_TRAVEL,
            PlaceType.NATURE_TRAVEL, PlaylistTheme.WALK_DRIVE_TRAVEL,
            PlaceType.SOCIAL_SPACE, PlaylistTheme.PARTY_ENERGY
    );
    private static final Map<PlaceType, Double> PLACE_SCORE = Map.of(
            PlaceType.CAFE_LIBRARY, 0.2,
            PlaceType.ROOM_HOME, 0.2,
            PlaceType.TRANSPORT, 0.3,
            PlaceType.NATURE_TRAVEL, 0.3,
            PlaceType.SOCIAL_SPACE, 0.3
    );

    public List<PlaylistTheme> score(PhotoAnalysisResult analysis) {
        Map<PlaylistTheme, Double> scores = new EnumMap<>(PlaylistTheme.class);
        for (PlaylistTheme theme : PlaylistTheme.values()) {
            scores.put(theme, 0.0);
        }

        addActivityScore(scores, analysis.activity());
        addWeatherScore(scores, analysis.weather());
        addTimeScore(scores, analysis.time());
        addPlaceScore(scores, analysis.place());
        addMoodScore(scores, analysis);

        List<Map.Entry<PlaylistTheme, Double>> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<PlaylistTheme, Double>comparingByValue().reversed()
                        .thenComparing(e -> e.getKey().ordinal()))
                .toList();

        List<PlaylistTheme> selected = ranked.stream()
                .filter(e -> e.getValue() >= SELECTION_THRESHOLD)
                .limit(MAX_THEMES)
                .map(Map.Entry::getKey)
                .toList();

        if (!selected.isEmpty()) {
            return selected;
        }

        Map.Entry<PlaylistTheme, Double> top = ranked.get(0);
        return top.getValue() > 0.0 ? List.of(top.getKey()) : List.of(PlaylistTheme.REST_HEALING);
    }

    /** activity/weather/time/place 는 AI 응답이 비정상적으로 null 을 줄 가능성에 대비해 먼저 null 체크한다(Map.of()는 get(null)에서 NPE). */
    private void addActivityScore(Map<PlaylistTheme, Double> scores, ActivityType activity) {
        if (activity == null) {
            return;
        }
        PlaylistTheme theme = ACTIVITY_THEME.get(activity);
        if (theme != null) {
            scores.merge(theme, ACTIVITY_SCORE, Double::sum);
        }
    }

    private void addWeatherScore(Map<PlaylistTheme, Double> scores, WeatherType weather) {
        if (weather == null) {
            return;
        }
        PlaylistTheme theme = WEATHER_THEME.get(weather);
        if (theme != null) {
            scores.merge(theme, WEATHER_SCORE.get(weather), Double::sum);
        }
    }

    private void addTimeScore(Map<PlaylistTheme, Double> scores, TimeType time) {
        if (time == null) {
            return;
        }
        PlaylistTheme theme = TIME_THEME.get(time);
        if (theme != null) {
            scores.merge(theme, TIME_SCORE.get(time), Double::sum);
        }
    }

    private void addPlaceScore(Map<PlaylistTheme, Double> scores, PlaceType place) {
        if (place == null) {
            return;
        }
        PlaylistTheme theme = PLACE_THEME.get(place);
        if (theme != null) {
            scores.merge(theme, PLACE_SCORE.get(place), Double::sum);
        }
    }

    private void addMoodScore(Map<PlaylistTheme, Double> scores, PhotoAnalysisResult analysis) {
        if (analysis.calmness() >= 0.7) {
            scores.merge(PlaylistTheme.REST_HEALING, MOOD_BONUS, Double::sum);
        }
        if (analysis.dreaminess() >= 0.7) {
            scores.merge(PlaylistTheme.NIGHT_DREAMY, MOOD_BONUS, Double::sum);
        }
        if (analysis.energy() >= 0.7 && analysis.positivity() >= 0.6) {
            scores.merge(PlaylistTheme.PARTY_ENERGY, MOOD_BONUS, Double::sum);
        }
        if (analysis.brightness() >= 0.7 && analysis.positivity() >= 0.6) {
            scores.merge(PlaylistTheme.BRIGHT_DAY, MOOD_BONUS, Double::sum);
        }
    }
}

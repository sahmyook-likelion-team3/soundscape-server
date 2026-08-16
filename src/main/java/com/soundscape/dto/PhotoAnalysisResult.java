package com.soundscape.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.soundscape.enums.ActivityType;
import com.soundscape.enums.PlaceType;
import com.soundscape.enums.TimeType;
import com.soundscape.enums.WeatherType;

/**
 * GPT-4o Vision 이 반환하는 사진 태깅 결과.
 * place/activity/weather/time 은 PhotoThemeScoringService 가 PlaylistTheme 로 변환하는 데 사용되고,
 * mood 6개 수치는 그와 별개로 음악 mood 벡터와 직접 비교된다.
 *
 * [Jackson 3 주의] databind 는 tools.jackson 으로 이동했지만
 * 애노테이션은 그대로 com.fasterxml.jackson.annotation 을 사용한다. import 를 바꾸지 말 것.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PhotoAnalysisResult(
        String title,
        PlaceType place,
        ActivityType activity,
        WeatherType weather,
        TimeType time,
        double brightness,
        double energy,
        double calmness,
        double warmth,
        double positivity,
        double dreaminess
) {
    /** Music.moodVector() 와 인덱스 순서가 반드시 동일해야 한다. */
    public double[] moodVector() {
        return new double[]{brightness, energy, calmness, warmth, positivity, dreaminess};
    }

    /** 모델이 범위를 벗어난 값을 주는 경우를 대비한 0.0~1.0 클램핑. */
    public PhotoAnalysisResult clamped() {
        return new PhotoAnalysisResult(
                title, place, activity, weather, time,
                clamp(brightness), clamp(energy), clamp(calmness),
                clamp(warmth), clamp(positivity), clamp(dreaminess)
        );
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}

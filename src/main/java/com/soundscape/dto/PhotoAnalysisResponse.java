package com.soundscape.dto;

import com.soundscape.enums.ActivityType;
import com.soundscape.enums.PlaceType;
import com.soundscape.enums.TimeType;
import com.soundscape.enums.WeatherType;

/**
 * 사진 분석 API 응답 전용 구조. 내부 계산에 쓰이는 PhotoAnalysisResult(flat)와 달리,
 * 팀원 API 명세서의 {place, activity, weather, time, mood:{...}} 중첩 구조를 그대로 따른다.
 */
public record PhotoAnalysisResponse(
        PlaceType place,
        ActivityType activity,
        WeatherType weather,
        TimeType time,
        Mood mood
) {
    public record Mood(
            double brightness,
            double energy,
            double calmness,
            double warmth,
            double positivity,
            double dreaminess
    ) {
    }

    public static PhotoAnalysisResponse of(PhotoAnalysisResult r) {
        return new PhotoAnalysisResponse(
                r.place(), r.activity(), r.weather(), r.time(),
                new Mood(r.brightness(), r.energy(), r.calmness(), r.warmth(), r.positivity(), r.dreaminess())
        );
    }
}

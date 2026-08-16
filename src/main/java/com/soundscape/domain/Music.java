package com.soundscape.domain;

import com.soundscape.enums.Genre;
import com.soundscape.enums.PlaylistTheme;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 음원 엔티티. 6대 감정 수치(0.0~1.0)와 다중 테마를 보유한다.
 * music_theme 는 별도 Entity 대신 @ElementCollection 으로 매핑하여 복합키 Entity/Repository 작성을 제거했다.
 */
@Entity
@Table(name = "music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Music {

    /** 감정 벡터 차원 수. moodScore 정규화(sqrt(6))의 기준. */
    public static final int MOOD_DIMENSION = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String artist;

    @Column(name = "audio_object_key", nullable = false, length = 500)
    private String audioObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Genre genre;

    @Column(nullable = false)
    private double brightness;

    @Column(nullable = false)
    private double energy;

    @Column(nullable = false)
    private double calmness;

    @Column(nullable = false)
    private double warmth;

    @Column(nullable = false)
    private double positivity;

    @Column(nullable = false)
    private double dreaminess;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "music_theme", joinColumns = @JoinColumn(name = "music_id"))
    @Column(name = "theme", length = 40)
    @Enumerated(EnumType.STRING)
    private Set<PlaylistTheme> themes = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 6대 감정 벡터. 유클리드 거리 계산 시 photo 벡터와 인덱스 순서가 반드시 일치해야 한다. */
    public double[] moodVector() {
        return new double[]{brightness, energy, calmness, warmth, positivity, dreaminess};
    }
}

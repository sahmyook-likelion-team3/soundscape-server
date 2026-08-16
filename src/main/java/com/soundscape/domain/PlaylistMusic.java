package com.soundscape.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 플레이리스트 - 곡 매핑. music 은 엔티티 연관 대신 musicId(Long)만 저장한다.
 * 조회 시 MusicCacheService 인메모리 캐시에서 즉시 해석하므로 music 테이블 조인이 발생하지 않는다.
 */
@Entity
@Table(name = "playlist_music")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistMusic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "playlist_id", nullable = false)
    private Playlist playlist;

    @Column(name = "music_id", nullable = false)
    private Long musicId;

    @Column(name = "`position`", nullable = false)
    private int position;

    private PlaylistMusic(Playlist playlist, Long musicId, int position) {
        this.playlist = playlist;
        this.musicId = musicId;
        this.position = position;
    }

    static PlaylistMusic of(Playlist playlist, Long musicId, int position) {
        return new PlaylistMusic(playlist, musicId, position);
    }
}

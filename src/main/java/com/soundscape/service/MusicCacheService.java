package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.enums.Genre;
import com.soundscape.repository.MusicRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 전체 음원 인메모리 캐시.
 * 곡 수가 수십~수천 규모라 매 추천 요청마다 DB를 조회할 이유가 없다.
 * 기동 시 1회 전량 로드하고, 이후 추천/조회는 전부 메모리 연산으로 처리한다(응답 수 ms).
 * 음원 추가 수급 후에는 POST /api/v1/admin/music/reload 로 재적재하면 서버 재시작이 불필요하다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MusicCacheService {

    private final MusicRepository musicRepository;

    private volatile List<Music> allMusics = List.of();
    private volatile Map<Long, Music> musicById = Map.of();
    private volatile Map<Genre, List<Music>> musicByGenre = Map.of();

    @PostConstruct
    public void load() {
        List<Music> loaded = List.copyOf(musicRepository.findAllWithThemes());

        this.allMusics = loaded;
        this.musicById = loaded.stream()
                .collect(Collectors.toUnmodifiableMap(Music::getId, Function.identity()));

        Map<Genre, List<Music>> byGenre = new LinkedHashMap<>();
        for (Genre genre : Genre.values()) {
            byGenre.put(genre, loaded.stream()
                    .filter(m -> m.getGenre() == genre)
                    .toList());
        }
        this.musicByGenre = Map.copyOf(byGenre);

        log.info("[MusicCache] 음원 {}곡 적재 완료. 장르별 보유: {}", loaded.size(),
                musicByGenre.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue().size())
                        .collect(Collectors.joining(", ")));
    }

    /** 음원 INSERT 후 캐시 재적재. */
    public int reload() {
        load();
        return allMusics.size();
    }

    public List<Music> findAll() {
        return allMusics;
    }

    public Music findById(Long musicId) {
        return musicById.get(musicId);
    }

    public boolean existsAll(List<Long> musicIds) {
        return musicIds.stream().allMatch(musicById::containsKey);
    }
}

package com.soundscape.repository;

import com.soundscape.domain.Music;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MusicRepository extends JpaRepository<Music, Long> {

    /** 서버 기동 시 1회 호출. themes 를 fetch join 하여 N+1 없이 26곡 전량을 한 번에 로드한다. */
    @Query("select distinct m from Music m left join fetch m.themes order by m.id asc")
    List<Music> findAllWithThemes();
}

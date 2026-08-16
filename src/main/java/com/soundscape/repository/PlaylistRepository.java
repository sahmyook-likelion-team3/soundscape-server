package com.soundscape.repository;

import com.soundscape.domain.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    @Query("select distinct p from Playlist p left join fetch p.items where p.user.id = :userId order by p.createdAt desc")
    List<Playlist> findAllByUserIdWithItems(@Param("userId") Long userId);

    @Query("select distinct p from Playlist p left join fetch p.items where p.id = :playlistId and p.user.id = :userId")
    Optional<Playlist> findByIdAndUserIdWithItems(@Param("playlistId") Long playlistId, @Param("userId") Long userId);
}

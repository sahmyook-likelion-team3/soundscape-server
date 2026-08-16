package com.soundscape.domain;

import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "playlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_genre", length = 40)
    private Genre sourceGenre;

    @Column(name = "cover_object_key", length = 500)
    private String coverObjectKey;

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<PlaylistMusic> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Playlist(User user, String title, SourceType sourceType, Genre sourceGenre, String coverObjectKey) {
        this.user = user;
        this.title = title;
        this.sourceType = sourceType;
        this.sourceGenre = sourceGenre;
        this.coverObjectKey = coverObjectKey;
    }

    public static Playlist create(User user, String title, SourceType sourceType, Genre sourceGenre, String coverObjectKey) {
        return new Playlist(user, title, sourceType, sourceGenre, coverObjectKey);
    }

    /** musicIds 순서를 그대로 position(1..n)으로 저장한다. */
    public void replaceItems(List<Long> musicIds) {
        this.items.clear();
        for (int i = 0; i < musicIds.size(); i++) {
            this.items.add(PlaylistMusic.of(this, musicIds.get(i), i + 1));
        }
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

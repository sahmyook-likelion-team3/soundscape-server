package com.soundscape.service;

import com.soundscape.domain.Playlist;
import com.soundscape.domain.User;
import com.soundscape.enums.SourceType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.PlaylistRepository;
import com.soundscape.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceDeleteTest {

    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MusicCacheService musicCache;
    @Mock
    private MusicRecommendationService recommendationService;
    @Mock
    private OpenAiVisionService visionService;
    @Mock
    private PhotoThemeScoringService themeScoringService;
    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PlaylistService playlistService;

    @Test
    void 정상_삭제() {
        User user = User.create("testuser01", "hashed", "테스트유저");
        Playlist playlist = Playlist.create(user, "제목", SourceType.PHOTO_RECOMMENDATION, null, "covers/abc.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playlistRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(playlist));

        playlistService.delete(1L, 10L);

        verify(s3Service).deleteObject("covers/abc.jpg");
        verify(playlistRepository).delete(playlist);
    }

    @Test
    void 존재하지_않는_플레이리스트면_404() {
        User user = User.create("testuser01", "hashed", "테스트유저");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(playlistRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playlistService.delete(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PLAYLIST_NOT_FOUND));
    }
}

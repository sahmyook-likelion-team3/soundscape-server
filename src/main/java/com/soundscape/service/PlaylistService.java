package com.soundscape.service;

import com.soundscape.domain.Music;
import com.soundscape.domain.Playlist;
import com.soundscape.domain.PlaylistMusic;
import com.soundscape.domain.User;
import com.soundscape.dto.*;
import com.soundscape.enums.PlaylistTheme;
import com.soundscape.enums.SourceType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import com.soundscape.repository.PlaylistRepository;
import com.soundscape.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final MusicCacheService musicCache;
    private final MusicRecommendationService recommendationService;
    private final OpenAiVisionService visionService;
    private final PhotoThemeScoringService themeScoringService;
    private final S3Service s3Service;

    /** 사진 기반 추천. 저장은 하지 않고 5곡 Presigned URL 만 반환한다. */
    public RecommendationResponse recommendByPhoto(MultipartFile photo) {
        PhotoAnalysisResult analysis = visionService.analyze(photo);
        List<PlaylistTheme> themes = themeScoringService.score(analysis);
        List<MusicResponse> musics = toMusicResponses(recommendationService.recommendByPhoto(analysis, themes));
        return RecommendationResponse.photo(analysis, themes, musics);
    }

    /** 플리 저장: 원본 사진 S3 업로드 + 플리 + 곡 position 저장. */
    @Transactional
    public PlaylistDetailResponse save(Long userId, PlaylistSaveRequest request, MultipartFile photo) {
        User user = requireUser(userId);
        validateSaveRequest(request, photo);

        String coverObjectKey = s3Service.uploadCover(photo);

        Playlist playlist = Playlist.create(
                user,
                request.getTitle(),
                request.getSourceType(),
                null,
                coverObjectKey
        );
        playlist.replaceItems(request.getMusicIds());
        playlistRepository.save(playlist);

        return toDetail(playlist);
    }

    /** 내 보관함 목록. 커버 Presigned URL 포함. */
    @Transactional(readOnly = true)
    public List<PlaylistSummaryResponse> findMyPlaylists(Long userId) {
        requireUser(userId);
        return playlistRepository.findAllByUserIdWithItems(userId).stream()
                .map(p -> PlaylistSummaryResponse.of(p, s3Service.presignedUrl(p.getCoverObjectKey())))
                .toList();
    }

    /** 플리 상세. 조회 시점에 오디오 Presigned URL 을 재발급한다. */
    @Transactional(readOnly = true)
    public PlaylistDetailResponse findDetail(Long userId, Long playlistId) {
        requireUser(userId);
        Playlist playlist = playlistRepository.findByIdAndUserIdWithItems(playlistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));
        return toDetail(playlist);
    }

    /** 플리 삭제. 본인 소유가 아니거나 존재하지 않으면 404 (소유권 정보를 노출하지 않기 위해 403이 아닌 404로 통일). */
    @Transactional
    public void delete(Long userId, Long playlistId) {
        requireUser(userId);
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAYLIST_NOT_FOUND));

        s3Service.deleteObject(playlist.getCoverObjectKey());
        playlistRepository.delete(playlist);
    }

    /** 저장 요청 검증: 곡 수, 중복, 존재 여부, 사진 필수 여부. 현재 MVP는 사진 기반만 지원한다. */
    private void validateSaveRequest(PlaylistSaveRequest request, MultipartFile photo) {
        List<Long> musicIds = request.getMusicIds();

        if (musicIds.size() != MusicRecommendationService.RESULT_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_MUSIC_COUNT);
        }
        if (musicIds.stream().distinct().count() != musicIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_MUSIC);
        }
        if (!musicCache.existsAll(musicIds)) {
            throw new BusinessException(ErrorCode.MUSIC_NOT_FOUND);
        }
        if (request.getSourceType() != SourceType.PHOTO_RECOMMENDATION) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException(ErrorCode.IMAGE_REQUIRED);
        }
    }

    private PlaylistDetailResponse toDetail(Playlist playlist) {
        List<MusicResponse> musics = playlist.getItems().stream()
                .sorted(Comparator.comparingInt(PlaylistMusic::getPosition))
                .map(item -> {
                    Music music = musicCache.findById(item.getMusicId());
                    return music == null ? null
                            : MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), null, item.getPosition());
                })
                .filter(Objects::nonNull)
                .toList();

        return PlaylistDetailResponse.of(playlist, s3Service.presignedUrl(playlist.getCoverObjectKey()), musics);
    }

    private List<MusicResponse> toMusicResponses(List<MusicRecommendationService.Scored> scoredList) {
        List<MusicResponse> result = new java.util.ArrayList<>(scoredList.size());
        for (int i = 0; i < scoredList.size(); i++) {
            MusicRecommendationService.Scored scored = scoredList.get(i);
            Music music = scored.music();
            Double score = scored.score() == null
                    ? null
                    : Math.round(scored.score() * 10000) / 10000.0;
            result.add(MusicResponse.of(music, s3Service.presignedUrl(music.getAudioObjectKey()), score, i + 1));
        }
        return result;
    }

    /** 임시 인증: X-User-Id 헤더 검증. 미전송 401, 미존재 사용자 401. */
    private User requireUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.USER_ID_REQUIRED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USER));
    }
}

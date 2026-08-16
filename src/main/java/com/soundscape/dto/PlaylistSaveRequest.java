package com.soundscape.dto;

import com.soundscape.enums.Genre;
import com.soundscape.enums.SourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * POST /playlists (multipart/form-data) 의 "request" 파트(JSON)에 바인딩되는 DTO.
 * 프론트는 FormData 에 request(JSON Blob: title, sourceType, sourceGenre, musicIds)와
 * image(File, 필수 — 현재 MVP는 사진 기반만 지원) 두 파트를 담아 전송한다.
 */
@Getter
@Setter
public class PlaylistSaveRequest {

    @NotBlank(message = "title은 필수입니다.")
    @Size(max = 200, message = "title은 200자를 넘을 수 없습니다.")
    private String title;

    @NotNull(message = "sourceType은 필수입니다.")
    private SourceType sourceType;

    /** sourceType=GENRE_RECOMMENDATION 인 경우에만 사용. */
    private Genre sourceGenre;

    @NotEmpty(message = "musicIds는 1개 이상이어야 합니다.")
    private List<Long> musicIds;
}

package com.soundscape.dto;

import java.util.List;

/** GET /playlists 응답 래퍼. 명세서가 배열을 {"playlists": [...]} 로 감싸도록 정해서 그대로 따른다. */
public record PlaylistListResponse(List<PlaylistSummaryResponse> playlists) {
}

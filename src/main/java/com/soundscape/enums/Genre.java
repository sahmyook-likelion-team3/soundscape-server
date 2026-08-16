package com.soundscape.enums;

/**
 * 7대 장르 표준 Enum. 모든 음원은 저장 시 주장르 하나를 갖는다(Music.genre).
 * 현재 MVP는 사진 기반 추천만 제공하고 장르 추천 기능은 없어서, 이 값은 음원 메타데이터/표시 용도로만 쓰인다.
 * 현재 DB 보유 곡수: LOFI_INSTRUMENTAL(8), JAZZ_CLASSICAL_NEWAGE(10), POP(8).
 * HIPHOP_RAP / RNB_SOUL / KPOP / JPOP_ANIME_OST 는 음원 추가 수급 예정 (현재 0곡).
 */
public enum Genre {
    LOFI_INSTRUMENTAL,
    JAZZ_CLASSICAL_NEWAGE,
    POP,
    HIPHOP_RAP,
    RNB_SOUL,
    KPOP,
    JPOP_ANIME_OST
}

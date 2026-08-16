package com.soundscape.service;

import com.soundscape.dto.PhotoAnalysisResult;
import com.soundscape.enums.ActivityType;
import com.soundscape.enums.PlaceType;
import com.soundscape.enums.TimeType;
import com.soundscape.enums.WeatherType;
import com.soundscape.exception.BusinessException;
import com.soundscape.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GPT-4o Vision 사진 감성 분석.
 *
 * 응답 지연 단축 3종 적용:
 *  1) 서버에서 이미지 최대 변 512px 리사이즈 후 JPEG 재인코딩 -> 업로드 페이로드 대폭 축소
 *  2) image_url.detail = low  -> 타일 분할 없이 단일 저해상도 패스로 처리
 *  3) response_format = json_schema (strict) -> 파싱 실패/재시도 제거, 필드 누락 없음
 *
 * [Spring Boot 4 / Jackson 3 주의]
 *  - databind 패키지가 com.fasterxml.jackson -> tools.jackson 으로 변경되었다.
 *  - JsonNode 순회 대신 타입 안전한 record 역직렬화를 사용해 Jackson 3 의 메서드 리네임 영향을 받지 않게 했다.
 *  - 애노테이션(@JsonIgnoreProperties 등)은 여전히 com.fasterxml.jackson.annotation 이다.
 */
@Slf4j
@Service
public class OpenAiVisionService {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final String SYSTEM_PROMPT = """
            너는 사진의 장소, 활동, 날씨, 시간대와 시각적 분위기를 분석하는 엔진이다.
            주어진 사진을 보고 다음을 산출한다.
            title = 사진 분위기에 어울리는 한국어 플레이리스트 제목. 15자 내외의 짧고 감성적인 문구. 예: "비 오는 밤의 카페".
            place = 사진 속 장소 분류. 확실하지 않으면 OTHER.
            activity = 사진 속 활동/상황 분류. 확실하지 않으면 UNKNOWN.
            weather = 사진 속 날씨. 실내이거나 알 수 없으면 UNKNOWN.
            time = 사진 속 시간대. 확실하지 않으면 UNKNOWN.
            그리고 6개 감정 수치를 0.0 이상 1.0 이하의 소수로 산출한다.
            수치 정의:
            brightness = 화면의 밝기와 조도. 어두운 밤/실내면 낮고 햇살 가득한 낮이면 높다.
            energy = 활동성과 역동성. 정적인 풍경은 낮고 사람이 붐비거나 움직임이 큰 장면은 높다.
            calmness = 차분함과 고요함. 소란스러운 장면일수록 낮다.
            warmth = 색온도와 포근함. 주황/노랑 계열 따뜻한 톤이면 높고 푸른 한색 계열이면 낮다.
            positivity = 밝고 긍정적인 정서. 우울하거나 쓸쓸하면 낮다.
            dreaminess = 몽환적이고 아련한 느낌. 안개, 야경, 흐릿한 빛 번짐이 있으면 높다.
            반드시 지정된 JSON 스키마 형식으로만 응답한다.
            """;

    private final RestClient openAiRestClient;
    private final String model;
    private final String imageDetail;
    private final int imageMaxSize;

    public OpenAiVisionService(
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            @Value("${openai.model}") String model,
            @Value("${openai.image-detail}") String imageDetail,
            @Value("${openai.image-max-size}") int imageMaxSize) {
        this.openAiRestClient = openAiRestClient;
        this.model = model;
        this.imageDetail = imageDetail;
        this.imageMaxSize = imageMaxSize;
    }

    public PhotoAnalysisResult analyze(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_FILE);
        }

        String base64 = Base64.getEncoder().encodeToString(resizeToJpeg(photo));
        String requestBody = buildRequestBody(base64);

        try {
            // Boot 4 는 JSpecify 로 body() 를 nullable 로 명시한다. 반드시 null 검사할 것.
            String raw = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (raw == null || raw.isBlank()) {
                throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
            }

            String content = extractContent(MAPPER.readValue(raw, ChatResponse.class));
            return MAPPER.readValue(content, PhotoAnalysisResult.class).clamped();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Vision] 분석 실패", e);
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED, e);
        }
    }

    private String extractContent(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
        }
        Choice choice = response.choices().get(0);
        if (choice == null || choice.message() == null
                || choice.message().content() == null || choice.message().content().isBlank()) {
            throw new BusinessException(ErrorCode.VISION_ANALYSIS_FAILED);
        }
        return choice.message().content();
    }

    /** 최대 변 imageMaxSize(기본 512px)로 축소 후 JPEG 바이트 반환. */
    private byte[] resizeToJpeg(MultipartFile photo) {
        try {
            BufferedImage source = ImageIO.read(photo.getInputStream());
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_IMAGE);
            }

            int longestSide = Math.max(source.getWidth(), source.getHeight());
            double scale = longestSide > imageMaxSize ? (double) imageMaxSize / longestSide : 1.0;
            int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resized.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, "jpg", out);
            return out.toByteArray();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE, e);
        }
    }

    private String buildRequestBody(String base64Jpeg) {
        String template = """
                {
                  "model": "%s",
                  "temperature": 0.2,
                  "max_tokens": 300,
                  "messages": [
                    { "role": "system", "content": %s },
                    { "role": "user", "content": [
                        { "type": "text", "text": "이 사진의 제목, 장소/활동/날씨/시간대와 6대 감정 수치를 산출하라." },
                        { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,%s", "detail": "%s" } }
                      ]
                    }
                  ],
                  "response_format": {
                    "type": "json_schema",
                    "json_schema": {
                      "name": "photo_mood_analysis",
                      "strict": true,
                      "schema": {
                        "type": "object",
                        "properties": {
                          "title":      { "type": "string" },
                          "place":      { "type": "string", "enum": [%s] },
                          "activity":   { "type": "string", "enum": [%s] },
                          "weather":    { "type": "string", "enum": [%s] },
                          "time":       { "type": "string", "enum": [%s] },
                          "brightness": { "type": "number" },
                          "energy":     { "type": "number" },
                          "calmness":   { "type": "number" },
                          "warmth":     { "type": "number" },
                          "positivity": { "type": "number" },
                          "dreaminess": { "type": "number" }
                        },
                        "required": ["title","place","activity","weather","time","brightness","energy","calmness","warmth","positivity","dreaminess"],
                        "additionalProperties": false
                      }
                    }
                  }
                }
                """;

        return String.format(template,
                model,
                MAPPER.writeValueAsString(SYSTEM_PROMPT),
                base64Jpeg,
                imageDetail,
                enumJsonArray(PlaceType.values()),
                enumJsonArray(ActivityType.values()),
                enumJsonArray(WeatherType.values()),
                enumJsonArray(TimeType.values()));
    }

    private static String enumJsonArray(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(v -> "\"" + v.name() + "\"")
                .collect(Collectors.joining(","));
    }

    // ---------- OpenAI 응답 매핑 (JsonNode 순회 대신 타입 안전 역직렬화) ----------

    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    record Message(String content) {
    }
}

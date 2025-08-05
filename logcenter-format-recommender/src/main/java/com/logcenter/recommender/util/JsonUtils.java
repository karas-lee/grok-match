package com.logcenter.recommender.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * JSON 처리 유틸리티 클래스
 * GSON 라이브러리를 사용한 JSON 파싱 및 생성
 */
public class JsonUtils {
    
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .setLenient()  // 엄격하지 않은 파싱 모드
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    
    private static final Gson GSON_COMPACT = new GsonBuilder()
            .disableHtmlEscaping()
            .setLenient()  // 엄격하지 않은 파싱 모드
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    
    private static final JsonParser JSON_PARSER = new JsonParser();
    
    /**
     * 객체를 JSON 문자열로 변환 (Pretty Print)
     */
    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
    
    /**
     * 객체를 JSON 문자열로 변환 (Compact)
     */
    public static String toJsonCompact(Object obj) {
        return GSON_COMPACT.toJson(obj);
    }
    
    /**
     * JSON 문자열을 객체로 변환
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return GSON.fromJson(json, classOfT);
    }
    
    /**
     * JSON 문자열을 객체로 변환 (Generic Type)
     */
    public static <T> T fromJson(String json, Type typeOfT) {
        return GSON.fromJson(json, typeOfT);
    }
    
    /**
     * Reader에서 JSON을 읽어 객체로 변환
     */
    public static <T> T fromJson(Reader reader, Class<T> classOfT) {
        return GSON.fromJson(reader, classOfT);
    }
    
    /**
     * Reader에서 JSON을 읽어 객체로 변환 (Generic Type)
     */
    public static <T> T fromJson(Reader reader, Type typeOfT) {
        return GSON.fromJson(reader, typeOfT);
    }
    
    /**
     * 객체를 Writer에 JSON으로 작성
     */
    public static void toJson(Object obj, Writer writer) {
        GSON.toJson(obj, writer);
    }
    
    /**
     * JSON 문자열을 List로 변환
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        Type listType = TypeToken.getParameterized(List.class, elementClass).getType();
        return GSON.fromJson(json, listType);
    }
    
    /**
     * JSON 문자열을 Map으로 변환
     */
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        Type mapType = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
        return GSON.fromJson(json, mapType);
    }
    
    /**
     * JSON 문자열을 Map<String, Object>로 변환 (일반적인 사용)
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
        return GSON.fromJson(json, mapType);
    }
    
    /**
     * JSON 문자열 유효성 검사
     */
    public static boolean isValidJson(String json) {
        try {
            JSON_PARSER.parse(json);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
    
    /**
     * JSON 문자열을 JsonElement로 파싱
     */
    public static JsonElement parseJson(String json) {
        return JSON_PARSER.parse(json);
    }
    
    /**
     * 객체를 JsonElement로 변환
     */
    public static JsonElement toJsonElement(Object obj) {
        return GSON.toJsonTree(obj);
    }
    
    /**
     * JsonElement를 객체로 변환
     */
    public static <T> T fromJsonElement(JsonElement element, Class<T> classOfT) {
        return GSON.fromJson(element, classOfT);
    }
    
    /**
     * 객체 깊은 복사 (JSON 직렬화/역직렬화 이용)
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(T object) {
        if (object == null) {
            return null;
        }
        
        String json = toJson(object);
        return (T) fromJson(json, object.getClass());
    }
    
    /**
     * 두 JSON 문자열이 동일한지 비교 (포맷 무시)
     */
    public static boolean areJsonEquals(String json1, String json2) {
        try {
            JsonElement element1 = parseJson(json1);
            JsonElement element2 = parseJson(json2);
            return element1.equals(element2);
        } catch (JsonSyntaxException e) {
            return false;
        }
    }
    
    /**
     * JSON 문자열 포맷팅 (Pretty Print)
     */
    public static String formatJson(String json) {
        JsonElement element = parseJson(json);
        return GSON.toJson(element);
    }
    
    /**
     * JSON 문자열 압축 (공백 제거)
     */
    public static String compactJson(String json) {
        JsonElement element = parseJson(json);
        return GSON_COMPACT.toJson(element);
    }
    
    /**
     * 예외를 안전하게 처리하는 JSON 변환
     */
    public static <T> T fromJsonSafe(String json, Class<T> classOfT, T defaultValue) {
        try {
            return fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            return defaultValue;
        }
    }
    
    /**
     * GSON 인스턴스 반환 (커스텀 설정이 필요한 경우)
     */
    public static Gson getGson() {
        return GSON;
    }
    
    /**
     * 새로운 GSON 빌더 반환 (커스텀 설정용)
     */
    public static GsonBuilder newGsonBuilder() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping();
    }
}
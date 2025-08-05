package com.logcenter.recommender.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * Jackson을 사용한 JSON 처리 유틸리티 클래스
 */
public class JacksonJsonUtils {
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    
    private static final ObjectMapper MAPPER_COMPACT = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .configure(SerializationFeature.INDENT_OUTPUT, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    
    /**
     * 객체를 JSON 문자열로 변환 (Pretty Print)
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }
    
    /**
     * 객체를 JSON 문자열로 변환 (Compact)
     */
    public static String toJsonCompact(Object obj) {
        try {
            return MAPPER_COMPACT.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 객체로 변환
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        try {
            return MAPPER.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 객체로 변환 (TypeReference 사용)
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * Reader에서 JSON을 읽어 객체로 변환
     */
    public static <T> T fromJson(Reader reader, Class<T> classOfT) {
        try {
            return MAPPER.readValue(reader, classOfT);
        } catch (IOException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * Reader에서 JSON을 읽어 객체로 변환 (TypeReference 사용)
     */
    public static <T> T fromJson(Reader reader, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(reader, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * 객체를 Writer에 JSON으로 작성
     */
    public static void toJson(Object obj, Writer writer) {
        try {
            MAPPER.writeValue(writer, obj);
        } catch (IOException e) {
            throw new RuntimeException("JSON 작성 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 List로 변환
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> elementClass) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 Map으로 변환
     */
    public static <K, V> Map<K, V> fromJsonToMap(String json, Class<K> keyClass, Class<V> valueClass) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
    
    /**
     * JSON 문자열을 Map<String, Object>로 변환
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        return fromJsonToMap(json, String.class, Object.class);
    }
    
    /**
     * JSON 문자열 유효성 검사
     */
    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 객체 깊은 복사
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
     * 두 JSON 문자열이 동일한지 비교
     */
    public static boolean areJsonEquals(String json1, String json2) {
        try {
            return MAPPER.readTree(json1).equals(MAPPER.readTree(json2));
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * JSON 문자열 포맷팅 (Pretty Print)
     */
    public static String formatJson(String json) {
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 포맷팅 실패", e);
        }
    }
    
    /**
     * JSON 문자열 압축 (공백 제거)
     */
    public static String compactJson(String json) {
        try {
            Object obj = MAPPER.readValue(json, Object.class);
            return MAPPER_COMPACT.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 압축 실패", e);
        }
    }
    
    /**
     * ObjectMapper 인스턴스 반환 (커스텀 설정이 필요한 경우)
     */
    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}
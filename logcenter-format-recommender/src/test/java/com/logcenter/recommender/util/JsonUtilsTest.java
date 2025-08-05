package com.logcenter.recommender.util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import com.google.gson.JsonElement;

/**
 * JsonUtils 유틸리티 클래스 단위 테스트
 */
public class JsonUtilsTest {
    
    @Test
    public void testToJson() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        
        String json = JsonUtils.toJson(data);
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"test\""));
        assertTrue(json.contains("\"value\": 123"));
        assertTrue(json.contains("\n")); // Pretty print
    }
    
    @Test
    public void testToJsonCompact() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);
        
        String json = JsonUtils.toJsonCompact(data);
        assertNotNull(json);
        assertFalse(json.contains("\n")); // Compact
        assertTrue(json.contains("\"name\":\"test\""));
    }
    
    @Test
    public void testFromJson() {
        String json = "{\"name\":\"test\",\"value\":123}";
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = JsonUtils.fromJson(json, Map.class);
        
        assertEquals("test", data.get("name"));
        assertEquals(123.0, data.get("value")); // Gson은 숫자를 double로 파싱
    }
    
    @Test
    public void testFromJsonToList() {
        String json = "[{\"id\":1,\"name\":\"first\"},{\"id\":2,\"name\":\"second\"}]";
        
        List<TestObject> list = JsonUtils.fromJsonToList(json, TestObject.class);
        
        assertEquals(2, list.size());
        assertEquals(1, list.get(0).id);
        assertEquals("first", list.get(0).name);
        assertEquals(2, list.get(1).id);
        assertEquals("second", list.get(1).name);
    }
    
    @Test
    public void testFromJsonToMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        
        Map<String, String> map = JsonUtils.fromJsonToMap(json, String.class, String.class);
        
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }
    
    @Test
    public void testIsValidJson() {
        assertTrue(JsonUtils.isValidJson("{}"));
        assertTrue(JsonUtils.isValidJson("[]"));
        assertTrue(JsonUtils.isValidJson("{\"key\":\"value\"}"));
        assertFalse(JsonUtils.isValidJson("{invalid json}"));
        assertFalse(JsonUtils.isValidJson("not json at all"));
    }
    
    @Test
    public void testParseJson() {
        String json = "{\"name\":\"test\"}";
        JsonElement element = JsonUtils.parseJson(json);
        
        assertNotNull(element);
        assertTrue(element.isJsonObject());
        assertEquals("test", element.getAsJsonObject().get("name").getAsString());
    }
    
    @Test
    public void testDeepCopy() {
        TestObject original = new TestObject();
        original.id = 1;
        original.name = "original";
        original.nested = new TestObject();
        original.nested.id = 2;
        original.nested.name = "nested";
        
        TestObject copy = JsonUtils.deepCopy(original);
        
        assertNotSame(original, copy);
        assertNotSame(original.nested, copy.nested);
        assertEquals(original.id, copy.id);
        assertEquals(original.name, copy.name);
        assertEquals(original.nested.id, copy.nested.id);
        assertEquals(original.nested.name, copy.nested.name);
        
        // 변경 테스트
        copy.name = "modified";
        copy.nested.name = "modified nested";
        assertEquals("original", original.name);
        assertEquals("nested", original.nested.name);
    }
    
    @Test
    public void testAreJsonEquals() {
        String json1 = "{\"a\":1,\"b\":2}";
        String json2 = "{\n  \"b\": 2,\n  \"a\": 1\n}"; // 다른 형식, 같은 내용
        String json3 = "{\"a\":1,\"b\":3}"; // 다른 내용
        
        assertTrue(JsonUtils.areJsonEquals(json1, json2));
        assertFalse(JsonUtils.areJsonEquals(json1, json3));
        assertFalse(JsonUtils.areJsonEquals("invalid", json1));
    }
    
    @Test
    public void testFormatJson() {
        String compact = "{\"name\":\"test\",\"value\":123}";
        String formatted = JsonUtils.formatJson(compact);
        
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("  \"name\": \"test\""));
    }
    
    @Test
    public void testCompactJson() {
        String formatted = "{\n  \"name\": \"test\",\n  \"value\": 123\n}";
        String compact = JsonUtils.compactJson(formatted);
        
        assertFalse(compact.contains("\n"));
        assertFalse(compact.contains(" : "));
        assertTrue(compact.contains("{\"name\":\"test\",\"value\":123}"));
    }
    
    @Test
    public void testFromJsonSafe() {
        String validJson = "{\"id\":1,\"name\":\"test\"}";
        String invalidJson = "invalid json";
        
        TestObject obj = JsonUtils.fromJsonSafe(validJson, TestObject.class, null);
        assertNotNull(obj);
        assertEquals(1, obj.id);
        
        TestObject defaultObj = new TestObject();
        defaultObj.id = -1;
        TestObject result = JsonUtils.fromJsonSafe(invalidJson, TestObject.class, defaultObj);
        assertSame(defaultObj, result);
        assertEquals(-1, result.id);
    }
    
    // 테스트용 내부 클래스
    private static class TestObject {
        public int id;
        public String name;
        public TestObject nested;
    }
}
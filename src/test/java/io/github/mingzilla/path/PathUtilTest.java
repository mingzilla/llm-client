package io.github.mingzilla.path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilTest {

    @ParameterizedTest
    @MethodSource("getByPathTestCases")
    void getByPathShouldRetrieveValueAtPath(String path, Object expected) {
        Map<String, Object> map = createTestMap();
        assertEquals(expected, PathUtil.getByPath(map, path));
    }

    private static Stream<Arguments> getByPathTestCases() {
        return Stream.of(
                Arguments.of("simple", "value"),
                Arguments.of("'good choices'[0].name", "first choice"),
                Arguments.of("\"good choices\"[1].name", "second choice"),
                Arguments.of("'company.name'", "Acme Inc"),
                Arguments.of("nested.key", "nestedValue"),
                Arguments.of("nested.list[1]", 2),
                Arguments.of("nested.map.a", "a"),
                Arguments.of("array[0].name", "first"),
                Arguments.of("array[1].value", 2),
                Arguments.of("complex.data[0].items[1].id", "item2"),
                Arguments.of("complex.data[0].items[1].properties.color", "blue"),
                Arguments.of("nonexistent", null),
                Arguments.of("nested.nonexistent", null),
                Arguments.of("array[5]", null),
                Arguments.of("array[0].nonexistent", null),
                Arguments.of("", null)
        );
    }

    @Test
    void getByPathShouldHandleNullAndEmptyInputs() {
        assertNull(PathUtil.getByPath(null, "any.path"));
        assertNull(PathUtil.getByPath(new HashMap<>(), null));
        assertNull(PathUtil.getByPath(new HashMap<>(), ""));
    }

    @Test
    void getByPathShouldHandleTypeMismatchesGracefully() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "not a map");
        map.put("number", 123);
        map.put("list", List.of(1, 2, 3));

        assertNull(PathUtil.getByPath(map, "string.property"));
        assertNull(PathUtil.getByPath(map, "number.property"));
        assertNull(PathUtil.getByPath(map, "list.property"));
        assertNull(PathUtil.getByPath(map, "list[10]"));
    }

    @ParameterizedTest
    @MethodSource("setByPathTestCases")
    void setByPathShouldCreateAndSetValues(String path, Object value) {
        Map<String, Object> map = new HashMap<>();
        PathUtil.setByPath(map, path, value);
        assertEquals(value, PathUtil.getByPath(map, path));
    }

    private static Stream<Arguments> setByPathTestCases() {
        return Stream.of(
                Arguments.of("simple", "value"),
                Arguments.of("nested.key", "nestedValue"),
                Arguments.of("nested.list[1]", "listValue"),
                Arguments.of("array[0].name", "first"),
                Arguments.of("array[1].value", 2),
                Arguments.of("complex.data[0].items[1].id", "item2"),
                Arguments.of("complex.data[0].items[1].properties.color", "blue")
        );
    }

    @Test
    void setByPathShouldHandleLargeArrayIndices() {
        Map<String, Object> map = new HashMap<>();
        PathUtil.setByPath(map, "array[10].value", "found");

        assertEquals(11, ((List<?>) map.get("array")).size());
        assertEquals("found", PathUtil.getByPath(map, "array[10].value"));
        assertTrue(((List<?>) map.get("array")).get(0) instanceof Map);
        assertTrue(((List<?>) map.get("array")).get(9) instanceof Map);
    }

    @Test
    void setByPathShouldOverwriteExistingValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("existing", "oldValue");
        map.put("nested", new HashMap<>(Map.of("key", "oldNested")));
        
        List<Map<String, Object>> arrayList = new ArrayList<>();
        arrayList.add(new HashMap<>(Map.of("value", "oldArrayValue")));
        map.put("array", arrayList);

        PathUtil.setByPath(map, "existing", "newValue");
        PathUtil.setByPath(map, "nested.key", "newNested");
        PathUtil.setByPath(map, "array[0].value", "newArrayValue");

        assertEquals("newValue", map.get("existing"));
        assertEquals("newNested", ((Map<?, ?>) map.get("nested")).get("key"));
        assertEquals("newArrayValue", ((Map<?, ?>) ((List<?>) map.get("array")).get(0)).get("value"));
    }

    @Test
    void setByPathShouldHandleNullAndEmptyInputs() {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> originalMap = new HashMap<>();

        assertNull(PathUtil.setByPath(null, "any.path", "value"));
        assertEquals(map, PathUtil.setByPath(map, null, "value"));
        assertEquals(map, PathUtil.setByPath(map, "", "value"));
        assertEquals(originalMap, map);
    }

    @Test
    void setByPathShouldHandleTypeConversion() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "not a map");
        map.put("list", List.of("not", "a", "map"));

        PathUtil.setByPath(map, "string.property", "value");
        PathUtil.setByPath(map, "list.property", "value");

        assertTrue(map.get("string") instanceof Map);
        assertEquals("value", ((Map<?, ?>) map.get("string")).get("property"));
        assertTrue(map.get("list") instanceof Map);
        assertEquals("value", ((Map<?, ?>) map.get("list")).get("property"));
    }

    @Test
    void getByPathShouldHandleQuotedPathsWithSpecialCharacters() {
        Map<String, Object> map = new HashMap<>();
        map.put("key.with.dots", "value1");
        map.put("key with spaces", "value2");
        map.put("mixed.key with.both", "value3");

        assertEquals("value1", PathUtil.getByPath(map, "'key.with.dots'"));
        assertEquals("value2", PathUtil.getByPath(map, "'key with spaces'"));
        assertEquals("value3", PathUtil.getByPath(map, "'mixed.key with.both'"));
    }

    @Test
    void setByPathShouldHandleQuotedPaths() {
        Map<String, Object> map = new HashMap<>();

        PathUtil.setByPath(map, "'good choices'[0].name", "first");
        PathUtil.setByPath(map, "'company.name'", "Acme");

        assertEquals("first", ((Map<?, ?>) ((List<?>) map.get("good choices")).get(0)).get("name"));
        assertEquals("Acme", map.get("company.name"));
    }

    private static Map<String, Object> createTestMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("simple", "value");
        map.put("good choices", List.of(
                Map.of("name", "first choice"),
                Map.of("name", "second choice")
        ));
        map.put("company.name", "Acme Inc");
        map.put("nested", Map.of(
                "key", "nestedValue",
                "list", List.of(1, 2, 3),
                "map", Map.of("a", "a", "b", "b")
        ));
        map.put("array", List.of(
                Map.of("name", "first", "value", 1),
                Map.of("name", "second", "value", 2)
        ));
        map.put("complex", Map.of(
                "data", List.of(Map.of(
                        "items", List.of(
                                Map.of("id", "item1", "properties", Map.of("color", "red")),
                                Map.of("id", "item2", "properties", Map.of("color", "blue"))
                        )
                ))
        ));

        return map;
    }
}
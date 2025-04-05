package io.github.mingzilla.path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathUtil {

    /**
     * Retrieves a value from a nested Map structure using a dot-notation path with
     * optional array indices.
     * The path can contain both Map properties and array indices in the format:
     * - Simple property: "property"
     * - Array access: "property[index]"
     * - Nested access: "property.subproperty"
     * - Mixed access: "array[0].property"
     * - Quoted property: "'property.with.dots'" or "\"property with spaces\""
     *
     * @param map  The source Map to traverse.
     *             e.g. [choices: [[delta: [role: "assistant", content: "hi"]], []]]
     * @param path The path to the desired value using dot notation.
     *             e.g. "choices[0].delta.content" or "'property.with.dots'.value"
     * @return The value at the specified path, or null if:
     *         - The path doesn't exist
     *         - Any intermediate value is not of the expected type
     *         - Array index is out of bounds
     *         - Any other access error occurs
     */
    public static Object getByPath(Map<String, Object> map, String path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }

        Object current = map;
        String[] parts = splitPath(path);

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            try {
                // Handle array access
                if (part.contains("[") && part.contains("]")) {
                    String propertyName = part.substring(0, part.indexOf("["));
                    String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                    int index = Integer.parseInt(indexStr);

                    // Get the list/array by property name
                    if (!propertyName.isEmpty()) {
                        if (!(current instanceof Map))
                            return null;
                        current = ((Map<String, Object>) current).get(propertyName);
                    }

                    // Access the index
                    if (current instanceof List) {
                        List<Object> list = (List<Object>) current;
                        if (index >= 0 && index < list.size()) {
                            current = list.get(index);
                        } else {
                            return null;
                        }
                    } else {
                        return null;
                    }
                } else {
                    // Simple property access for both Map and List elements
                    if (current instanceof Map) {
                        current = ((Map<String, Object>) current).get(part);
                    } else if (current instanceof List) {
                        try {
                            int index = Integer.parseInt(part);
                            List<Object> list = (List<Object>) current;
                            if (index >= 0 && index < list.size()) {
                                current = list.get(index);
                            } else {
                                return null;
                            }
                        } catch (NumberFormatException e) {
                            // If not a number, treat as Map key
                            if (!(current instanceof Map))
                                return null;
                            current = ((Map<String, Object>) current).get(part);
                        }
                    } else {
                        return null;
                    }
                }
            } catch (Exception e) {
                // Any exception during path traversal returns null
                return null;
            }
        }

        return current;
    }

    /**
     * Sets a value in a nested Map structure using a dot-notation path with
     * optional array indices.
     * Creates all necessary intermediate Map, List structures and fills Lists with
     * empty items as needed.
     *
     * @param map   The target Map to modify. e.g. [:]
     * @param path  The path where to set the value using dot notation.
     *              e.g. "choices[0].delta.content" or "'property.with.dots'[0]"
     * @param value The value to set at the specified path. e.g. "hi"
     * @return The modified map with the new value set at the specified path
     */
    public static Map<String, Object> setByPath(Map<String, Object> map, String path, Object value) {
        if (map == null || path == null || path.isEmpty()) {
            return map;
        }

        Map<String, Object> current = map;
        String[] parts = splitPath(path);

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isLast = (i == parts.length - 1);

            try {
                if (part.contains("[") && part.contains("]")) {
                    String propertyName = part.substring(0, part.indexOf("["));
                    String indexStr = part.substring(part.indexOf("[") + 1, part.indexOf("]"));
                    int index = Integer.parseInt(indexStr);

                    // Handle array access with property name
                    if (!propertyName.isEmpty()) {
                        // Create List if it doesn't exist
                        current.putIfAbsent(propertyName, new ArrayList<>());
                        List<Object> list = (List<Object>) current.get(propertyName);

                        // Fill list with empty items up to index
                        while (list.size() <= index) {
                            list.add(isLast ? null : new HashMap<String, Object>());
                        }

                        if (isLast) {
                            list.set(index, value);
                        } else {
                            Object nextObj = list.get(index);
                            Map<String, Object> nextMap;
                            if (!(nextObj instanceof Map)) {
                                nextMap = new HashMap<>();
                                list.set(index, nextMap);
                            } else {
                                nextMap = (Map<String, Object>) nextObj;
                            }
                            current = nextMap;
                        }
                    }
                } else {
                    if (isLast) {
                        current.put(part, value);
                    } else {
                        current.putIfAbsent(part, new HashMap<String, Object>());
                        Object next = current.get(part);
                        if (!(next instanceof Map)) {
                            next = new HashMap<String, Object>();
                            current.put(part, next);
                        }
                        current = (Map<String, Object>) next;
                    }
                }
            } catch (Exception e) {
                // If any error occurs, stop processing
                return map;
            }
        }

        return map;
    }

    /**
     * Splits a path string into parts, respecting quoted sections.
     * Handles both single and double quotes for property names containing dots.
     *
     * @param path The path to split
     * @return Array of path segments with quotes removed
     */
    private static String[] splitPath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);

            if ((c == '"' || c == '\'') && (i == 0 || path.charAt(i - 1) != '\\')) {
                if (!inQuotes) {
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == '.' && !inQuotes && (i == 0 || path.charAt(i - 1) != '\\')) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            parts.add(current.toString());
        }

        return parts.stream()
                .map(PathUtil::unquote)
                .toArray(String[]::new);
    }

    /**
     * Removes surrounding quotes from a string if present.
     *
     * @param s The string to unquote
     * @return The string without surrounding quotes
     */
    private static String unquote(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}

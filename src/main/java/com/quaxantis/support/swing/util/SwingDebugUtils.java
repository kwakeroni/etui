package com.quaxantis.support.swing.util;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static javax.swing.JComponent.*;

public class SwingDebugUtils {

    private SwingDebugUtils() {
        throw new UnsupportedOperationException();
    }

    public static Stream<KeyMapping> getKeyMappings(JComponent component) {

        //        inputMap = component.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
//        for (int i = 0; inputMap != null; i++, inputMap = inputMap.getParent()) {
//            InputMap m = inputMap;
//            inputMapByLevel.put(i, Arrays.stream(inputMap.keys()).collect(Collectors.toUnmodifiableMap(k -> k, m::get)));
//        }
        return IntStream.of(WHEN_FOCUSED, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, WHEN_IN_FOCUSED_WINDOW)
                .boxed()
                .flatMap(condition -> keyMappings(component, condition))
                .sorted(comparing(km -> String.valueOf(km.name()), String.CASE_INSENSITIVE_ORDER));
    }

    private static Stream<KeyMapping> keyMappings(JComponent component, int condition) {
        String conditionName = switch (condition) {
            case WHEN_FOCUSED -> "WHEN_FOCUSED";
            case WHEN_ANCESTOR_OF_FOCUSED_COMPONENT -> "WHEN_ANCESTOR";
            case WHEN_IN_FOCUSED_WINDOW -> "WHEN_IN_WINDOW";
            default -> throw new IllegalArgumentException("Unexpected condition: " + condition);
        };

        Map<Integer, Map<KeyStroke, Object>> inputMapByLevel = new HashMap<>();
        ActionMap actionMap = component.getActionMap();
        InputMap inputMap = component.getInputMap(condition);
        for (int i = 0; inputMap != null; i++, inputMap = inputMap.getParent()) {
            InputMap m = inputMap;
            inputMapByLevel.put(i, Arrays.stream(Objects.requireNonNullElseGet(inputMap.keys(), () -> new KeyStroke[0])).collect(Collectors.toUnmodifiableMap(k -> k, m::get)));
        }

        Stream<KeyMapping> keyMappingsStream = inputMapByLevel.entrySet().stream()
                .flatMap(levelEntry -> levelEntry.getValue().entrySet().stream()
                        .map(keyEntry -> new KeyMapping(conditionName, levelEntry.getKey(), keyEntry.getKey(), keyEntry.getValue(), actionMap.get(keyEntry.getValue()))));
        return keyMappingsStream;
    }

    public record KeyMapping(String condition, int level, KeyStroke key, Object name, Object action) {
        @Override
        public String toString() {
            return "[%1d] %-36s [%32s] [%14s] %s".formatted(level, name, key, condition, action);
        }
    }
}

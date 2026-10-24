package com.portfolio.common.web.filter;

public record FieldRule(String name, Class<?> type, boolean filterable, boolean sortable) {

    public static FieldRule filterAndSort(String name, Class<?> type) {
        return new FieldRule(name, type, true, true);
    }

    public static FieldRule filterOnly(String name, Class<?> type) {
        return new FieldRule(name, type, true, false);
    }

    public static FieldRule sortOnly(String name, Class<?> type) {
        return new FieldRule(name, type, false, true);
    }
}

package com.portfolio.common.web.filter;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class FieldAllowList {

    private final Map<String, FieldRule> rules;

    private FieldAllowList(Map<String, FieldRule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public static FieldAllowList of(FieldRule... rules) {
        Map<String, FieldRule> map = new LinkedHashMap<>();
        for (FieldRule rule : rules) {
            map.put(rule.name(), rule);
        }
        return new FieldAllowList(map);
    }

    public static FieldAllowList of(Collection<FieldRule> rules) {
        Map<String, FieldRule> map = new LinkedHashMap<>();
        for (FieldRule rule : rules) {
            map.put(rule.name(), rule);
        }
        return new FieldAllowList(map);
    }

    public FieldRule requireFilterable(String field) {
        FieldRule rule = rules.get(field);
        if (rule == null || !rule.filterable()) {
            throw new FilterBadRequestException("Field is not filterable: " + field);
        }
        return rule;
    }

    public FieldRule requireSortable(String field) {
        FieldRule rule = rules.get(field);
        if (rule == null || !rule.sortable()) {
            throw new FilterBadRequestException("Field is not sortable: " + field);
        }
        return rule;
    }

    public Optional<FieldRule> find(String field) {
        return Optional.ofNullable(rules.get(field));
    }
}

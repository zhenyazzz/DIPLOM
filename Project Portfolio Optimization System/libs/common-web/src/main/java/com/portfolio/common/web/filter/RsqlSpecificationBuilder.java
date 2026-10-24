package com.portfolio.common.web.filter;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class RsqlSpecificationBuilder {

    public static final ComparisonOperator LIKE = new ComparisonOperator("=like=", true);
    public static final ComparisonOperator ILIKE = new ComparisonOperator("=ilike=", true);

    private final RSQLParser parser;

    public RsqlSpecificationBuilder() {
        Set<ComparisonOperator> operators = new HashSet<>(RSQLOperators.defaultOperators());
        operators.add(LIKE);
        operators.add(ILIKE);
        this.parser = new RSQLParser(operators);
    }

    public <T> Specification<T> build(String rsql, FieldAllowList allowList) {
        if (!StringUtils.hasText(rsql)) {
            return (root, query, cb) -> cb.conjunction();
        }
        try {
            Node rootNode = parser.parse(rsql.trim());
            return (root, query, cb) -> rootNode.accept(new SpecVisitor<>(allowList, root, cb));
        } catch (RSQLParserException ex) {
            throw new FilterBadRequestException("Invalid RSQL: " + ex.getMessage(), ex);
        } catch (FilterBadRequestException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new FilterBadRequestException("Invalid filter: " + ex.getMessage(), ex);
        }
    }

    private static final class SpecVisitor<T> implements RSQLVisitor<Predicate, Void> {

        private final FieldAllowList allowList;
        private final Root<T> root;
        private final CriteriaBuilder cb;

        private SpecVisitor(FieldAllowList allowList, Root<T> root, CriteriaBuilder cb) {
            this.allowList = allowList;
            this.root = root;
            this.cb = cb;
        }

        @Override
        public Predicate visit(AndNode node, Void param) {
            List<Predicate> predicates = new ArrayList<>();
            for (Node child : node.getChildren()) {
                predicates.add(child.accept(this));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }

        @Override
        public Predicate visit(OrNode node, Void param) {
            List<Predicate> predicates = new ArrayList<>();
            for (Node child : node.getChildren()) {
                predicates.add(child.accept(this));
            }
            return cb.or(predicates.toArray(Predicate[]::new));
        }

        @Override
        public Predicate visit(ComparisonNode node, Void param) {
            String selector = node.getSelector();
            if (selector.contains(".")) {
                throw new FilterBadRequestException("Nested filter paths are not supported: " + selector);
            }
            FieldRule rule = allowList.requireFilterable(selector);
            Path<?> path = root.get(selector);
            String op = node.getOperator().getSymbol();
            List<String> args = node.getArguments();

            return switch (op) {
                case "==" -> equal(path, rule, args);
                case "!=" -> notEqual(path, rule, args);
                case "=gt=" -> comparable(path, rule, args, true, false);
                case "=ge=" -> comparable(path, rule, args, true, true);
                case "=lt=" -> comparable(path, rule, args, false, false);
                case "=le=" -> comparable(path, rule, args, false, true);
                case "=in=" -> path.in(convertAll(rule, args));
                case "=out=" -> cb.not(path.in(convertAll(rule, args)));
                case "=like=" -> like(path, args, false);
                case "=ilike=" -> like(path, args, true);
                default -> throw new FilterBadRequestException("Unsupported operator: " + op);
            };
        }

        private Predicate equal(Path<?> path, FieldRule rule, List<String> args) {
            String raw = single(args);
            if ("null".equalsIgnoreCase(raw)) {
                return cb.isNull(path);
            }
            return cb.equal(path, convert(rule, raw));
        }

        private Predicate notEqual(Path<?> path, FieldRule rule, List<String> args) {
            String raw = single(args);
            if ("null".equalsIgnoreCase(raw)) {
                return cb.isNotNull(path);
            }
            return cb.notEqual(path, convert(rule, raw));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Predicate comparable(Path<?> path, FieldRule rule, List<String> args, boolean greater, boolean inclusive) {
            Object value = convert(rule, single(args));
            if (!(value instanceof Comparable<?> comparable)) {
                throw new FilterBadRequestException("Field does not support comparison: " + rule.name());
            }
            Expression<? extends Comparable> expr = (Expression<? extends Comparable>) path;
            if (greater) {
                return inclusive ? cb.greaterThanOrEqualTo(expr, (Comparable) comparable)
                        : cb.greaterThan(expr, (Comparable) comparable);
            }
            return inclusive ? cb.lessThanOrEqualTo(expr, (Comparable) comparable)
                    : cb.lessThan(expr, (Comparable) comparable);
        }

        private Predicate like(Path<?> path, List<String> args, boolean ignoreCase) {
            String pattern = single(args);
            Expression<String> expr = path.as(String.class);
            if (ignoreCase) {
                return cb.like(cb.lower(expr), pattern.toLowerCase(Locale.ROOT));
            }
            return cb.like(expr, pattern);
        }

        private String single(List<String> args) {
            if (args == null || args.isEmpty()) {
                throw new FilterBadRequestException("Missing filter argument");
            }
            return args.getFirst();
        }

        private List<Object> convertAll(FieldRule rule, List<String> args) {
            if (args == null || args.isEmpty()) {
                throw new FilterBadRequestException("Missing filter arguments for in/out");
            }
            return args.stream().map(a -> convert(rule, a)).toList();
        }

        private Object convert(FieldRule rule, String raw) {
            Class<?> type = rule.type();
            try {
                if (type == String.class) {
                    return raw;
                }
                if (type == UUID.class) {
                    return UUID.fromString(raw);
                }
                if (type == Boolean.class || type == boolean.class) {
                    return Boolean.valueOf(raw);
                }
                if (type == Integer.class || type == int.class) {
                    return Integer.valueOf(raw);
                }
                if (type == Long.class || type == long.class) {
                    return Long.valueOf(raw);
                }
                if (type == Double.class || type == double.class) {
                    return Double.valueOf(raw);
                }
                if (type == BigDecimal.class) {
                    return new BigDecimal(raw);
                }
                if (type == Instant.class) {
                    return Instant.parse(raw);
                }
                if (type == LocalDate.class) {
                    return LocalDate.parse(raw);
                }
                if (type == LocalDateTime.class) {
                    return LocalDateTime.parse(raw);
                }
                if (type.isEnum()) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object value = Enum.valueOf((Class<? extends Enum>) type, raw);
                    return value;
                }
            } catch (RuntimeException ex) {
                throw new FilterBadRequestException(
                        "Cannot convert '" + raw + "' to " + type.getSimpleName() + " for field " + rule.name(), ex);
            }
            throw new FilterBadRequestException("Unsupported filter type for field " + rule.name() + ": " + type.getName());
        }
    }
}

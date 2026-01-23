package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class CategoryRatingAssigner {

    private static final Logger log = LoggerFactory.getLogger(CategoryRatingAssigner.class);

    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+[\\.,]?\\d*)\\s*(مل|ml|ميللي|لتر|l|لت|كغم|كجم|كغ|kg|كيلو|جم|غ|g|حبة)", Pattern.CASE_INSENSITIVE);
    private static final double DEFAULT_JITTER = 1.0d;

    public List<Product> sortAndAssignRatings(List<Product> products, double minRating, double maxRating) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        List<AnnotatedProduct> annotated = annotate(products);
        List<AnnotatedProduct> ordered = orderProducts(annotated);
        assignRatings(ordered, minRating, maxRating);
        return ordered.stream().map(AnnotatedProduct::product).toList();
    }

    private List<AnnotatedProduct> annotate(List<Product> products) {
        List<AnnotatedProduct> annotated = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            ParsedName parsedName = parseName(p.getName());
            annotated.add(new AnnotatedProduct(p, parsedName.baseKey(), parsedName.sizeValue(), i));
        }
        return annotated;
    }

    private List<AnnotatedProduct> orderProducts(List<AnnotatedProduct> annotated) {
        Map<String, List<AnnotatedProduct>> byBase = annotated.stream()
                .collect(Collectors.groupingBy(AnnotatedProduct::baseKey, HashMap::new, Collectors.toList()));

        Comparator<AnnotatedProduct> inGroupComparator = (a, b) -> {
            if (a.sizeValue() != null && b.sizeValue() != null) {
                int cmp = Double.compare(a.sizeValue(), b.sizeValue());
                if (cmp != 0) {
                    return cmp;
                }
            } else if (a.sizeValue() == null || b.sizeValue() == null) {
                // If any size is missing, keep the original input order to avoid surprising shuffles.
                return Integer.compare(a.originalIndex(), b.originalIndex());
            }
            if (!Objects.equals(a.product().getSku(), b.product().getSku())) {
                return Optional.ofNullable(a.product().getSku()).orElse("")
                        .compareTo(Optional.ofNullable(b.product().getSku()).orElse(""));
            }
            return a.product().getName().compareToIgnoreCase(b.product().getName());
        };

        Comparator<Map.Entry<String, List<AnnotatedProduct>>> groupComparator = (e1, e2) -> {
            String parent1 = firstParent(e1.getValue());
            String parent2 = firstParent(e2.getValue());
            int cmpParent = Optional.ofNullable(parent1).orElse("").compareTo(Optional.ofNullable(parent2).orElse(""));
            if (cmpParent != 0) {
                return cmpParent;
            }
            return e1.getKey().compareTo(e2.getKey());
        };

        return byBase.entrySet().stream()
                .sorted(groupComparator)
                .flatMap(entry -> entry.getValue().stream().sorted(inGroupComparator))
                .toList();
    }

    private void assignRatings(List<AnnotatedProduct> ordered, double minRating, double maxRating) {
        if (ordered.isEmpty()) {
            return;
        }
        int total = ordered.size();
        if (total == 1) {
            ordered.get(0).product().setRating(maxRating);
            return;
        }
        double step = (maxRating - minRating) / (total - 1);
        if (step < 1.0d) {
            step = 1.0d;
        }
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < total; i++) {
            double target = maxRating - (i * step);
            double adjusted = ensureWithinBounds(target, minRating, maxRating);
            adjusted = ensureUnique(adjusted, minRating, maxRating, used);
            ordered.get(i).product().setRating(adjusted);
            used.add((int) adjusted);
        }
    }

    private double ensureWithinBounds(double value, double min, double max) {
        if (value > max) {
            return max;
        }
        if (value < min) {
            return min;
        }
        return value;
    }

    private double ensureUnique(double candidate, double min, double max, Set<Integer> used) {
        int rounded = roundToInt(candidate);
        if (!used.contains(rounded)) {
            return rounded;
        }
        double increment = DEFAULT_JITTER;
        int up = rounded;
        int down = rounded;
        for (int i = 1; i < 200; i++) {
            down = roundToInt(rounded - (i * increment));
            if (down >= min && !used.contains(down)) {
                return down;
            }
            up = roundToInt(rounded + (i * increment));
            if (up <= max && !used.contains(up)) {
                return up;
            }
        }
        log.warn("Unable to find unique rating within bounds; returning original candidate={}", rounded);
        return rounded;
    }

    private ParsedName parseName(String name) {
        if (name == null || name.isBlank()) {
            return new ParsedName("", null);
        }
        String normalized = normalizeArabicDigits(name).toLowerCase(Locale.ROOT);
        Matcher matcher = SIZE_PATTERN.matcher(normalized);
        Double sizeValue = null;
        String cleaned = normalized;
        if (matcher.find()) {
            cleaned = matcher.replaceFirst(" ").trim();
            sizeValue = convertToCanonical(matcher.group(1), matcher.group(2));
        }
        String baseKey = cleaned.replaceAll("[\\p{Punct}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (baseKey.isBlank()) {
            baseKey = normalized.trim();
        }
        return new ParsedName(baseKey, sizeValue);
    }

    private Double convertToCanonical(String numberPart, String unitPart) {
        try {
            double value = Double.parseDouble(numberPart.replace(',', '.'));
            String unit = unitPart.toLowerCase(Locale.ROOT);
            if (unit.contains("مل") || unit.equals("ml")) {
                return value; // milliliters already
            }
            if (unit.contains("لتر") || unit.equals("l") || unit.equals("لت")) {
                return value * 1000d; // liters to ml
            }
            if (unit.contains("كغم") || unit.contains("كجم") || unit.contains("كغ") || unit.equals("kg") || unit.contains("كيلو")) {
                return value * 1000d; // kilograms to grams
            }
            if (unit.contains("جم") || unit.equals("غ") || unit.equals("g")) {
                return value; // grams already
            }
            if (unit.contains("حبة")) {
                return value; // treat pieces as-is
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String normalizeArabicDigits(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(switch (c) {
                case '\u0660' -> '0';
                case '\u0661' -> '1';
                case '\u0662' -> '2';
                case '\u0663' -> '3';
                case '\u0664' -> '4';
                case '\u0665' -> '5';
                case '\u0666' -> '6';
                case '\u0667' -> '7';
                case '\u0668' -> '8';
                case '\u0669' -> '9';
                default -> c;
            });
        }
        return sb.toString();
    }

    private String firstParent(List<AnnotatedProduct> group) {
        return group.stream()
                .map(ap -> ap.product().getParentCategoryName())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private double round1(double value) {
        return Math.round(value * 10.0d) / 10.0d;
    }

    private int roundToInt(double value) {
        return (int) Math.round(value);
    }

    private record ParsedName(String baseKey, Double sizeValue) {
    }

    private record AnnotatedProduct(Product product, String baseKey, Double sizeValue, int originalIndex) {
    }
}

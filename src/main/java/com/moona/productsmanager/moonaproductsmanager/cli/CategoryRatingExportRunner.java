package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.CategoryProductsRatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CategoryRatingExportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryRatingExportRunner.class);

    private final CategoryProductsRatingService ratingService;

    public CategoryRatingExportRunner(CategoryProductsRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        int exitCode = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("categoryRatingExport")) {
                triggered = true;
                try {
                    Map<String, String> options = parseOptions(args, i, arg);
                    String categoryId = options.get("category");
                    double min = parseDouble(options.get("min"), 1.0d, "min");
                    double max = parseDouble(options.get("max"), 10.0d, "max");
                    String createdAfter = options.get("createdAfter");
                    String updatedBefore = options.get("updatedBefore");
                    Boolean published = parseBoolean(options.get("published"));
                    ratingService.exportCategoryWithRatings(categoryId, min, max, createdAfter, updatedBefore, published)
                        .doOnSuccess(msg -> log.info(msg))
                        .block();
                } catch (Exception ex) {
                    exitCode = 1;
                    log.error("categoryRatingExport failed", ex);
                }
                break;
            }
        }
        if (triggered) {
            log.info("categoryRatingExport task finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }

    private Map<String, String> parseOptions(String[] args, int commandIndex, String commandToken) {
        Map<String, String> options = new HashMap<>();
        extractInlineOptions(commandToken, options);
        for (int i = commandIndex + 1; i < args.length; i++) {
            String token = args[i];
            if (token.contains("Export") && !token.contains("=")) {
                // likely another task flag; stop parsing to avoid consuming next task's args
                break;
            }
            putIfPresent(token, options);
        }
        return options;
    }

    private void extractInlineOptions(String commandToken, Map<String, String> options) {
        String remainder = commandToken.replaceFirst("^categoryRatingExport", "");
        remainder = remainder.replaceFirst("^[,:]", "");
        if (remainder.isBlank()) {
            return;
        }
        for (String token : remainder.split(",")) {
            putIfPresent(token, options);
        }
    }

    private void putIfPresent(String token, Map<String, String> options) {
        if (token == null || !token.contains("=")) {
            return;
        }
        String cleaned = token.replaceFirst("^--", "");
        String[] parts = cleaned.split("=", 2);
        if (parts.length == 2 && !parts[0].isBlank()) {
            options.put(parts[0].trim(), parts[1].trim());
        }
    }

    private double parseDouble(String raw, double defaultValue, String key) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            log.warn("Invalid double for {}: {}, using default {}", key, raw, defaultValue);
            return defaultValue;
        }
    }

    private Boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(raw.trim());
    }
}

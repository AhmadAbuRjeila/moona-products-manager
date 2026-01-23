package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.ProductsExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProductsExportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductsExportRunner.class);
    private final ProductsExportService productsExportService;

    public ProductsExportRunner(ProductsExportService productsExportService) {
        this.productsExportService = productsExportService;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        int exitCode = 0;
        String categoryId = null;
        Boolean published = null;
        String createdAfter = null;
        String updatedBefore = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("productsExport")) {
                triggered = true;
                Map<String, String> options = parseOptions(args, i, arg);
                categoryId = options.get("category");
                published = parseBoolean(options.get("published"));
                createdAfter = options.get("createdAfter");
                updatedBefore = options.get("updatedBefore");
                try {
                    productsExportService.exportProductsToFile(createdAfter, updatedBefore, categoryId, published)
                        .doOnSuccess(msg -> log.info(msg))
                        .block();
                } catch (Exception ex) {
                    exitCode = 1;
                    log.error("productsExport failed", ex);
                }
                break;
            }
        }
        if (triggered) {
            log.info("productsExport task finished (categoryId={}, published={}, createdAfter={}, updatedBefore={}); exiting with code {}", categoryId, published, createdAfter, updatedBefore, exitCode);
            System.exit(exitCode);
        }
    }

    private Map<String, String> parseOptions(String[] args, int commandIndex, String commandToken) {
        Map<String, String> options = new HashMap<>();
        extractInlineOptions(commandToken, options);
        for (int i = commandIndex + 1; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) {
                if (token.contains("Export")) {
                    break; // likely next task
                }
                continue; // ignore non-flag tokens
            }
            putIfPresent(token, options);
        }
        return options;
    }

    private void extractInlineOptions(String commandToken, Map<String, String> options) {
        String remainder = commandToken.replaceFirst("^productsExport", "");
        remainder = remainder.replaceFirst("^[,:]", "");
        if (remainder.isBlank()) {
            return;
        }
        for (String token : remainder.split(",")) {
            if (!token.startsWith("--")) {
                token = "--" + token; // allow legacy inline usage
            }
            putIfPresent(token, options);
        }
    }

    private void putIfPresent(String token, Map<String, String> options) {
        if (token == null || !token.startsWith("--") || !token.contains("=")) {
            return;
        }
        String cleaned = token.replaceFirst("^--", "");
        String[] parts = cleaned.split("=", 2);
        if (parts.length == 2 && !parts[0].isBlank()) {
            options.put(parts[0].trim(), parts[1].trim());
        }
    }

    private Boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private String parseCategoryId(String arg, String... args) {
        if (arg.contains("=")) {
            String[] parts = arg.split("=", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        if (arg.contains(":")) {
            String[] parts = arg.split(":", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        if (args.length > 1) {
            int idx = Arrays.asList(args).indexOf(arg);
            if (idx >= 0 && idx + 1 < args.length) {
                String next = args[idx + 1];
                if (!next.contains("Export")) {
                    return next;
                }
            }
        }
        return null;
    }
}

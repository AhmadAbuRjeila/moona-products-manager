package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.ProductsExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

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
        for (String arg : args) {
            if (arg.startsWith("productsExport")) {
                triggered = true;
                categoryId = parseCategoryId(arg, args);
                try {
                    productsExportService.exportProductsToFile(null, null, categoryId)
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
            log.info("productsExport task finished (categoryId={}); exiting with code {}", categoryId, exitCode);
            System.exit(exitCode);
        }
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

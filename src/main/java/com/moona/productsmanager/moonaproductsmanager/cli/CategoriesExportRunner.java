package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.CategoriesExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CategoriesExportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoriesExportRunner.class);

    private final CategoriesExportService categoriesExportService;

    public CategoriesExportRunner(CategoriesExportService categoriesExportService) {
        this.categoriesExportService = categoriesExportService;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        int exitCode = 0;
        for (String arg : args) {
            if ("categoriesExport".equalsIgnoreCase(arg)) {
                triggered = true;
                try {
                    categoriesExportService.exportCategoriesToFile()
                        .doOnSuccess(msg -> log.info(msg))
                        .block();
                } catch (Exception ex) {
                    exitCode = 1;
                    log.error("categoriesExport failed", ex);
                }
                break;
            }
        }
        if (triggered) {
            log.info("categoriesExport task finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }
}


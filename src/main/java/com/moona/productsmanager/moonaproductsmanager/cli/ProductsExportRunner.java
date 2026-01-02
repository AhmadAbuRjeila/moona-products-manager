package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.ProductsExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
        for (String arg : args) {
            if ("productsExport".equalsIgnoreCase(arg)) {
                triggered = true;
                try {
                    productsExportService.exportProductsToFile()
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
            log.info("productsExport task finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }
}

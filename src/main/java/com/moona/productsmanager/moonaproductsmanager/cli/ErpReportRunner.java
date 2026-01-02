package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.ErpIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ErpReportRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ErpReportRunner.class);

    private final ErpIngestService erpIngestService;

    public ErpReportRunner(ErpIngestService erpIngestService) {
        this.erpIngestService = erpIngestService;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        boolean dryRun = false;
        String reportName = "mona3 Report";
        int exitCode = 0;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("erpScrape".equalsIgnoreCase(arg)) {
                triggered = true;
            } else if ("--report".equalsIgnoreCase(arg) && i + 1 < args.length) {
                reportName = args[i + 1];
            } else if ("--dry-run".equalsIgnoreCase(arg)) {
                dryRun = true;
            }
        }

        if (triggered) {
            try {
                erpIngestService.importFromErp(reportName, dryRun)
                    .doOnSuccess(msg -> log.info(msg))
                    .block();
            } catch (Exception ex) {
                exitCode = 1;
                log.error("erpScrape failed", ex);
            }
            log.info("erpScrape finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }
}


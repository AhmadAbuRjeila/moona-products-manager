package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.StaleOosUnpublishJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StaleOosUnpublishRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StaleOosUnpublishRunner.class);

    private final StaleOosUnpublishJob job;

    public StaleOosUnpublishRunner(StaleOosUnpublishJob job) {
        this.job = job;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        boolean dryRun = false;
        String updatedBefore = null;
        int pageSize = 0;
        int exitCode = 0;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("staleOosUnpublish".equalsIgnoreCase(arg)) {
                triggered = true;
            } else if ("--dry-run".equalsIgnoreCase(arg)) {
                dryRun = true;
            } else if ("--updated-before".equalsIgnoreCase(arg) && i + 1 < args.length) {
                updatedBefore = args[i + 1];
            } else if ("--page-size".equalsIgnoreCase(arg) && i + 1 < args.length) {
                try {
                    pageSize = Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException ignored) { }
            }
        }

        if (triggered) {
            try {
                job.run(updatedBefore, pageSize, dryRun)
                    .doOnSuccess(msg -> log.info(msg))
                    .block();
            } catch (Exception ex) {
                exitCode = 1;
                log.error("staleOosUnpublish failed", ex);
            }
            log.info("staleOosUnpublish finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }
}


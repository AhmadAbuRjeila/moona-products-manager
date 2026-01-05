package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.config.ExportProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class StaleOosUnpublishJob {
    private static final Logger log = LoggerFactory.getLogger(StaleOosUnpublishJob.class);

    private final ProductsExportService productsExportService;
    private final ProductsUpdateService productsUpdateService;
    private final ExportProperties exportProperties;

    public StaleOosUnpublishJob(ProductsExportService productsExportService,
                                ProductsUpdateService productsUpdateService,
                                ExportProperties exportProperties) {
        this.productsExportService = productsExportService;
        this.productsUpdateService = productsUpdateService;
        this.exportProperties = exportProperties;
    }

    public Mono<String> run(String updatedBeforeIso, int pageSize, boolean dryRun) {
        String cutoff = updatedBeforeIso != null && !updatedBeforeIso.isBlank()
            ? updatedBeforeIso
            : productsExportService.defaultUpdatedBeforeIso(14);
        int effectivePageSize = pageSize > 0 ? pageSize : exportProperties.getPageSize();

        log.info("Stale OOS unpublish starting (channel={}, cutoff={}, pageSize={}, dryRun={})",
            exportProperties.getChannel(), cutoff, effectivePageSize, dryRun);

        return productsExportService.fetchAllProducts(null, cutoff)
            .map(this::filterCandidates)
            .flatMap(candidates -> {
                log.info("Stale OOS unpublish candidates: {}", candidates.size());
                if (dryRun || candidates.isEmpty()) {
                    return Mono.just("Dry-run: would unpublish " + candidates.size() + " products");
                }
                List<Product> toUpdate = candidates.stream()
                    .peek(p -> p.setPublished(false))
                    .toList();
                return productsUpdateService.upsertProducts(toUpdate, ProductsUpdateService.UpdateMode.FULL)
                    .then(Mono.fromSupplier(() -> "Unpublished " + toUpdate.size() + " products"))
                    .doOnSuccess(msg -> log.info("Stale OOS unpublish finished: {}", msg))
                    .doOnError(ex -> log.error("Stale OOS unpublish failed", ex));
            });
    }

    private List<Product> filterCandidates(List<Product> products) {
        return products.stream()
            .filter(p -> p.getAvailableQuantity() != null && p.getAvailableQuantity() <= 0)
            .filter(p -> Boolean.TRUE.equals(p.getPublished()))
            .toList();
    }
}

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
        return run(updatedBeforeIso, pageSize, dryRun, null);
    }

    public Mono<String> run(String updatedBeforeIso, int pageSize, boolean dryRun, Boolean published) {
        String cutoff = updatedBeforeIso != null && !updatedBeforeIso.isBlank()
                ? updatedBeforeIso
                : productsExportService.defaultUpdatedBeforeIso(14);
        int effectivePageSize = pageSize > 0 ? pageSize : exportProperties.getPageSize();

        log.info("Stale OOS unpublish starting (channel={}, cutoff={}, pageSize={}, dryRun={}, published={})",
                exportProperties.getChannel(), cutoff, effectivePageSize, dryRun, published);

        return productsExportService.fetchAllProducts(null, cutoff, null, published)
                .map(this::summarizeAndFilter)
                .flatMap(summary -> {
                    log.info("staleOosUnpublish.summary fetched={} unpublishedAlready={} candidates={} dryRun={} published={}",
                            summary.totalFetched, summary.alreadyUnpublished, summary.candidates.size(), dryRun, published);
                    if (dryRun || summary.candidates.isEmpty()) {
                        return Mono.just("Dry-run: would unpublish " + summary.candidates.size() + " products (fetched=" + summary.totalFetched + ")");
                    }
                    List<Product> toUpdate = summary.candidates.stream()
                            .peek(p -> {
                                p.setPublished(false);
                                p.setChannelId("Q2hhbm5lbDo1"); // Ramallah
                            })
                            .toList();
                    return productsUpdateService.upsertProducts(toUpdate, ProductsUpdateService.UpdateMode.FULL)
                            .then(Mono.fromSupplier(() -> "Unpublished " + toUpdate.size() + " products (fetched=" + summary.totalFetched + ")"))
                            .doOnSuccess(msg -> {
                                List<String> skus = toUpdate.stream().map(Product::getSku).toList();
                                log.info("staleOosUnpublish.unpublished skus={} count={}", skus, skus.size());
                            })
                            .doOnError(ex -> log.error("staleOosUnpublish failed after fetched={} candidates={}", summary.totalFetched, summary.candidates.size(), ex));
                });
    }

    private Summary summarizeAndFilter(List<Product> products) {
        int total = products == null ? 0 : products.size();
        long alreadyUnpublished = products == null ? 0 : products.stream().filter(p -> Boolean.FALSE.equals(p.getPublished())).count();
        List<Product> candidates = filterCandidates(products);
        return new Summary(total, (int) alreadyUnpublished, candidates);
    }

    private List<Product> filterCandidates(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        return products.stream()
                .filter(p -> p.getAvailableQuantity() != null && p.getAvailableQuantity() <= 0)
                .filter(p -> Boolean.TRUE.equals(p.getPublished()))
                .toList();
    }

    private record Summary(int totalFetched, int alreadyUnpublished, List<Product> candidates) {
    }
}

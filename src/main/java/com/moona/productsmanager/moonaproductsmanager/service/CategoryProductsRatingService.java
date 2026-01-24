package com.moona.productsmanager.moonaproductsmanager.service;

import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Service
public class CategoryProductsRatingService {

    private static final Logger log = LoggerFactory.getLogger(CategoryProductsRatingService.class);

    private final ProductsExportService productsExportService;
    private final CategoryRatingAssigner categoryRatingAssigner;
    private final ExcelWriter excelWriter;
    private final ProductsUpdateService productsUpdateService;

    public CategoryProductsRatingService(ProductsExportService productsExportService,
                                         CategoryRatingAssigner categoryRatingAssigner,
                                         ExcelWriter excelWriter,
                                         ProductsUpdateService productsUpdateService) {
        this.productsExportService = productsExportService;
        this.categoryRatingAssigner = categoryRatingAssigner;
        this.excelWriter = excelWriter;
        this.productsUpdateService = productsUpdateService;
    }

    public Mono<String> exportCategoryWithRatings(String categoryId, double minRating, double maxRating,
                                                  String createdAfterIso, String updatedBeforeIso) {
        return exportCategoryWithRatings(categoryId, minRating, maxRating, createdAfterIso, updatedBeforeIso, null);
    }

    public Mono<String> exportCategoryWithRatings(String categoryId, double minRating, double maxRating,
                                                  String createdAfterIso, String updatedBeforeIso, Boolean published) {
        log.info("Starting category rating export (categoryId={}, minRating={}, maxRating={}, createdAfter={}, updatedBefore={}, published={})",
            categoryId, minRating, maxRating, createdAfterIso, updatedBeforeIso, published);
        return productsExportService.fetchAllProducts(createdAfterIso, updatedBeforeIso, categoryId, published)
            .map(products -> categoryRatingAssigner.sortAndAssignRatings(products, minRating, maxRating))
            .flatMap(sorted -> productsUpdateService.updateRatings(sorted).then(writeExport(sorted, categoryId)));
    }

    public Mono<String> exportCategoryWithRatings(String categoryId, double minRating, double maxRating) {
        return exportCategoryWithRatings(categoryId, minRating, maxRating, null, null, null);
    }

    private Mono<String> writeExport(List<Product> products, String categoryId) {
        try {
            excelWriter.exportProducts(products);
            log.info("Category export finished, wrote {} products for category {}", products.size(), categoryId);
            return Mono.just("Exported " + products.size() + " products for category " + categoryId);
        } catch (IOException e) {
            log.error("Failed to write category export for {}", categoryId, e);
            return Mono.error(e);
        }
    }
}

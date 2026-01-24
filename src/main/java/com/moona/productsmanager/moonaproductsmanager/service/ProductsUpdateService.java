package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import com.moona.productsmanager.moonaproductsmanager.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductsUpdateService {
    private static final Logger log = LoggerFactory.getLogger(ProductsUpdateService.class);

    private final ApiClient apiClient;
    private final Helper helper;
    private final ObjectMapper objectMapper;

    public ProductsUpdateService(ApiClient apiClient, Helper helper, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.helper = helper;
        this.objectMapper = objectMapper;
    }

    public enum UpdateMode {FULL, SKIP_PRODUCT_MASTER_DATA}

    public Mono<Void> upsertProducts(List<Product> products) {
        return upsertProducts(products, UpdateMode.FULL);
    }

    public Mono<Void> upsertProducts(List<Product> products, UpdateMode mode) {
        log.info("upsertProducts invoked with {} items", products == null ? 0 : products.size());
        return Mono.defer(() -> {
            if (products == null || products.isEmpty()) {
                return Mono.empty();
            }
            int safeStart = 0;
            int safeLimit = products.size();
            List<Product> slice = products.stream().skip(safeStart).limit(safeLimit).toList();
            AtomicInteger processed = new AtomicInteger();
            AtomicInteger updated = new AtomicInteger();
            AtomicInteger created = new AtomicInteger();
            int total = slice.size();
            log.info("Upserting {} products (start={} limit={} of {}) with concurrency=5", total, safeStart, safeLimit, products.size());
            return Flux.fromIterable(slice)
                    .flatMap(p -> upsertSingle(p, mode, processed, total, updated, created), 4)
                    .then()
                    .doFinally(sig -> log.info("ERP upsert finished: total={} updated={} created={}", total, updated.get(), created.get()));
        });
    }

    public record VariantInfo(String variantId, String productId, Boolean published) {
    }

    private Mono<Void> upsertSingle(Product product, UpdateMode mode, AtomicInteger processed, int total, AtomicInteger updated, AtomicInteger created) {
        log.info("Upsert starting for sku={} name={}", product.getSku(), product.getName());
        return findVariantBySku(product.getSku())
                .materialize()
                .flatMap(signal -> {
                    if (signal.hasValue()) {
                        VariantInfo info = signal.get();
                        boolean skipUnpublishedZeroQty = product.getAvailableQuantity() != null
                            && product.getAvailableQuantity() == 0
                            && Boolean.FALSE.equals(info.published());
                        if (skipUnpublishedZeroQty) {
                            return Mono.fromRunnable(() -> log.info("Skipping upsert for sku={} name={} (existing unpublished with zero quantity)", product.getSku(), product.getName()));
                        }
                        log.info("Existing variant found for sku={} variantId={} productId={} published={}", product.getSku(), info.variantId(), info.productId(), info.published());
                        return updateExisting(product, info, mode)
                                .then(Mono.fromRunnable(updated::incrementAndGet))
                                .then(Mono.fromRunnable(() -> log.info("Upserted existing sku={} variantId={} productId={}", product.getSku(), info.variantId(), info.productId())));
                    }
                    if (signal.isOnComplete()) {
                        log.info("No existing variant for sku={}, creating new", product.getSku());
                        return createNew(product)
                                .then(Mono.fromRunnable(created::incrementAndGet))
                                .then(Mono.fromRunnable(() -> log.info("Created new sku={} name={}", product.getSku(), product.getName())));
                    }
                    if (signal.isOnError()) {
                        return Mono.error(signal.getThrowable());
                    }
                    return Mono.empty();
                })
                .doFinally(sig -> {
                    int done = processed.incrementAndGet();
                    if (done % 5 == 0 || done == total) {
                        log.info("ERP upsert progress: {}/{}", done, total);
                    }
                })
                .doOnError(ex -> log.error("Upsert failed for sku={}", product.getSku(), ex))
                .then();
    }

    private Mono<VariantInfo> findVariantBySku(String sku) {
        String query = helper.buildProductVariantQuery();
        Map<String, Object> variables = Map.of("sku", sku);
        return apiClient.mutation(query, variables)
                .flatMap(body -> {
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        JsonNode variant = root.path("data").path("productVariant");
                        if (variant.isMissingNode() || variant.isNull()) {
                            return Mono.empty();
                        }
                        String variantId = variant.path("id").asText(null);
                        String productId = variant.path("product").path("id").asText(null);
                        Boolean published = null;
                        JsonNode listings = variant.path("product").path("channelListings");
                        if (listings.isArray()) {
                            for (JsonNode listing : listings) {
                                if (listing.path("isPublished").isBoolean() && listing.path("isPublished").asBoolean()) {
                                    published = true;
                                    break;
                                }
                                if (published == null && listing.path("isPublished").isBoolean()) {
                                    published = false;
                                }
                            }
                        }
                        if (variantId == null) {
                            return Mono.empty();
                        }
                        return Mono.just(new VariantInfo(variantId, productId, published));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<Void> updateExisting(Product product, VariantInfo info, UpdateMode mode) {
        String resolvedProductId = info.productId() != null ? info.productId() : product.getId();
        Mono<Void> chain;
        if (mode == UpdateMode.SKIP_PRODUCT_MASTER_DATA) {
            chain = Mono.empty();
        } else {
            chain = updateProduct(product, resolvedProductId);
        }
        return chain
                .then(updateProductChannelListing(product, resolvedProductId))
                .then(updateVariantChannelListing(product, info.variantId()))
                .then(updateVariantStocks(product, info.variantId()));
    }

    private Mono<Void> updateProduct(Product product, String productId) {
        String mutation = "mutation ProductUpdate($id: ID!, $input: ProductInput!) {" +
                "  productUpdate(id: $id, input: $input) { product { id } errors { field message } }" +
                "}";

        Map<String, Object> input = helper.buildProductInputObject(product);

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", productId);
        variables.put("input", input);

        return apiClient.mutation(mutation, variables).then();
    }

    private Mono<Void> updateProductChannelListing(Product product, String productId) {
        // Skip if channel not provided
        if (product.getChannelId() == null) {
            return Mono.empty();
        }
        String mutation = "mutation productChannelListingUpdate($id: ID!, $input: ProductChannelListingUpdateInput!) {" +
                "  productChannelListingUpdate(id: $id, input: $input) { product { id } errors { field message } }" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", productId);
        variables.put("input", helper.buildProductChannelListingUpdateInputObject(product));

        return apiClient.mutation(mutation, variables).then();
    }

    private Mono<Void> updateVariantChannelListing(Product product, String variantId) {
        if (product.getChannelId() == null || product.getPrice() == null) {
            return Mono.empty();
        }
        String mutation = "mutation productVariantChannelListingUpdate($id: ID!, $input: [ProductVariantChannelListingAddInput!]!) {" +
                "  productVariantChannelListingUpdate(id: $id, input: $input) { variant { id } errors { field message } }" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", variantId);
        variables.put("input", helper.buildProductVariantChannelListingAddInput(product));

        return apiClient.mutation(mutation, variables).then();
    }

    private Mono<Void> updateVariantStocks(Product product, String variantId) {
        if (product.getWarehouseId() == null) {
            return Mono.empty();
        }
        String mutation = "mutation productVariantStocksUpdate($variantId: ID!, $stocks: [StockInput!]!) {" +
                "  productVariantStocksUpdate(variantId: $variantId, stocks: $stocks) { productVariant { id } errors { field message } }" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("variantId", variantId);
        variables.put("stocks", helper.buildStocksInput(product));

        return apiClient.mutation(mutation, variables).then();
    }

    private Mono<Void> createNew(Product product) {
        return createProduct(product)
                .flatMap(productId -> updateProductChannelListing(product, productId)
                        .then(createVariant(product, productId)
                                .flatMap(variantId -> updateVariantChannelListing(product, variantId)
                                        .then(updateVariantStocks(product, variantId))
                                )
                        )
                )
                .doOnError(ex -> log.error("Create flow failed for sku={} reason={}", product.getSku(), ex.getMessage(), ex))
                .then();
    }

    private Mono<String> createProduct(Product product) {
        String mutation = "mutation ProductCreate($input: ProductCreateInput!) {" +
                "  productCreate(input: $input) { product { id variants { id } } errors { field message } }" +
                "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("input", helper.buildProductCreateInputObject(product));

        return apiClient.mutation(mutation, variables)
                .flatMap(body -> {
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        JsonNode productNode = root.path("data").path("productCreate").path("product");
                        String productId = productNode.path("id").asText(null);
                        if (productId == null) {
                            return Mono.error(new IllegalStateException("productCreate did not return product id"));
                        }
                        return Mono.just(productId);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    private Mono<String> createVariant(Product product, String productId) {
        String mutation = "mutation ProductVariantCreate($input: ProductVariantCreateInput!) {" +
                "  productVariantCreate(input: $input) { productVariant { id } errors { field message } }" +
                "}";

        Map<String, Object> variantInput = helper.buildProductVariantCreateInput(product);
        variantInput.put("product", productId);

        Map<String, Object> variables = new HashMap<>();
        variables.put("input", variantInput);

        return apiClient.mutation(mutation, variables)
                .flatMap(body -> {
                    try {
                        JsonNode root = objectMapper.readTree(body);
                        String variantId = root.path("data").path("productVariantCreate").path("productVariant").path("id").asText(null);
                        if (variantId == null) {
                            return Mono.error(new IllegalStateException("productVariantCreate did not return variant id"));
                        }
                        log.info("Variant id={} created for productSKU={}", variantId, product.getSku());
                        return Mono.just(variantId);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    public Mono<Void> updateRatings(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Mono.empty();
        }
        AtomicInteger processed = new AtomicInteger();
        int total = products.size();
        log.info("Starting rating-only update for {} products", total);
        return Flux.fromIterable(products)
            .flatMap(this::updateSingleRating, 4)
            .doOnNext(p -> {
                int done = processed.incrementAndGet();
                if (done % 10 == 0 || done == total) {
                    log.info("Rating update progress: {}/{} (last id={} sku={} rating={})", done, total, p.getId(), p.getSku(), p.getRating());
                }
            })
            .then()
            .doFinally(sig -> log.info("Rating-only update finished: total={} processed={} signal={}", total, processed.get(), sig));
    }

    private Mono<Product> updateSingleRating(Product product) {
        if (product.getId() == null || product.getRating() == null) {
            log.warn("Skipping rating update for product without id/rating sku={} id={} rating={}", product.getSku(), product.getId(), product.getRating());
            return Mono.just(product);
        }
        String mutation = "mutation ProductUpdateRating($id: ID!, $input: ProductInput!) {" +
            "  productUpdate(id: $id, input: $input) { product { id rating } errors { field message } }" +
            "}";
        Map<String, Object> variables = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put("rating", product.getRating());
        variables.put("id", product.getId());
        variables.put("input", input);
        return apiClient.mutation(mutation, variables)
            .then(Mono.just(product))
            .doOnError(ex -> log.error("Failed to update rating for id={} sku={} rating={}", product.getId(), product.getSku(), product.getRating(), ex));
    }
}

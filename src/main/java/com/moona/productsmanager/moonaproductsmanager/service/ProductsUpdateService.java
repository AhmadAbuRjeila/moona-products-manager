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

    public enum UpdateMode { FULL, ERP_SKIP_NAME_CATEGORY }

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
            int safeLimit = 1000;
            List<Product> slice = products.stream().skip(safeStart).limit(safeLimit).toList();
            AtomicInteger processed = new AtomicInteger();
            int total = slice.size();
            log.info("Upserting {} products (start={} limit={} of {}) with concurrency=5", total, safeStart, safeLimit, products.size());
            return Flux.fromIterable(slice)
                .flatMap(p -> upsertSingle(p, mode, processed, total), 5)
                .then();
        });
    }

    public record VariantInfo(String variantId, String productId) {}

    private Mono<Void> upsertSingle(Product product, UpdateMode mode, AtomicInteger processed, int total) {
        log.info("Upsert starting for sku={} name={}", product.getSku(), product.getName());
        return findVariantBySku(product.getSku())
            .doOnNext(info -> log.info("Existing variant found for sku={} variantId={} productId={}", product.getSku(), info.variantId(), info.productId()))
            .flatMap(info -> updateExisting(product, info, mode))
            .switchIfEmpty(Mono.defer(() -> {
                log.info("No existing variant for sku={}, create path is currently disabled; skipping", product.getSku());
                return Mono.empty();
            }))
            .doFinally(sig -> {
                int done = processed.incrementAndGet();
                if (done % 5 == 0 || done == total) {
                    log.info("ERP upsert progress: {}/{}", done, total);
                }
            })
            .onErrorResume(ex -> {
                log.error("Upsert failed for sku={}", product.getSku(), ex);
                return Mono.empty();
            });
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
                    if (variantId == null) {
                        return Mono.empty();
                    }
                    return Mono.just(new VariantInfo(variantId, productId));
                } catch (Exception e) {
                    return Mono.error(e);
                }
            });
    }

    private Mono<Void> updateExisting(Product product, VariantInfo info, UpdateMode mode) {
        String resolvedProductId = info.productId() != null ? info.productId() : product.getId();
        return updateProduct(product, resolvedProductId, mode)
            .then(updateProductChannelListing(product, resolvedProductId))
            .then(updateVariantChannelListing(product, info.variantId()))
            .then(updateVariantStocks(product, info.variantId()));
    }

    private Mono<Void> updateProduct(Product product, String productId, UpdateMode mode) {
        String mutation = "mutation ProductUpdate($id: ID!, $input: ProductInput!) {" +
            "  productUpdate(id: $id, input: $input) { product { id } errors { field message } }" +
            "}";

        Map<String, Object> input = helper.buildProductInputObject(product);
        if (mode == UpdateMode.ERP_SKIP_NAME_CATEGORY) {
            input.remove("name");
            input.remove("category");
        }

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
        String mutation = "mutation ProductCreate($input: ProductCreateInput!, $channelListing: ProductChannelListingUpdateInput!, $variantInput: ProductVariantCreateInput!, $variantChannel: [ProductVariantChannelListingAddInput!]!) {" +
            "  productCreate(input: $input, channelListing: $channelListing) { product { id } errors { field message } }" +
            "  productVariantCreate(input: $variantInput) { productVariant { id } errors { field message } }" +
            "  productVariantChannelListingUpdate(id: $variantInput.product, input: $variantChannel) { productVariant { id } errors { field message } }" +
            "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("input", helper.buildProductCreateInputObject(product));
        variables.put("channelListing", helper.buildProductChannelListingUpdateInputObject(product));

        // Variant create needs product id; we assume productCreate returns it. Keep payload simple by reusing variant input after creation.
        Map<String, Object> variantInput = helper.buildProductVariantCreateInput(product);
        variables.put("variantInput", variantInput);
        variables.put("variantChannel", helper.buildProductVariantChannelListingAddInput(product));

        return apiClient.mutation(mutation, variables)
            .doOnSuccess(body -> log.info("Created product sku={} response={}", product.getSku(), body))
            .then();
    }
}

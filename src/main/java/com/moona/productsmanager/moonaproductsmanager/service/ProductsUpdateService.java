package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import com.moona.productsmanager.moonaproductsmanager.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Mono<Void> upsertProducts(List<Product> products) {
        log.info("upsertProducts invoked with {} items", products == null ? 0 : products.size());
        return Mono.defer(() -> {
            if (products == null || products.isEmpty()) {
                return Mono.empty();
            }
            List<Product> slice = products.stream().limit(2).toList();
            log.info("Upserting {} products (first 5 of {})", slice.size(), products.size());
            return Mono.when(slice.stream().map(this::upsertSingle).toList());
        });
    }

    public record VariantInfo(String variantId, String productId) {}

    private Mono<Void> upsertSingle(Product product) {
        log.info("Upsert starting for sku={} name={}", product.getSku(), product.getName());
        return findVariantBySku(product.getSku())
            .doOnNext(info -> log.info("Existing variant found for sku={} variantId={} productId={}", product.getSku(), info.variantId(), info.productId()))
            .flatMap(info -> updateExisting(product, info))
            .switchIfEmpty(Mono.defer(() -> {
                log.info("No existing variant for sku={}, create path is currently disabled; skipping", product.getSku());
                return Mono.empty();
            }))
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

    private Mono<Void> updateExisting(Product product, VariantInfo info) {
        String resolvedProductId = info.productId() != null ? info.productId() : product.getId();
        return updateProduct(product, resolvedProductId)
            .then(updateProductChannelListing(product, resolvedProductId))
            .then(updateVariantChannelListing(product, info.variantId()))
            .then(updateVariantStocks(product, info.variantId()));
    }

    private Mono<Void> updateProduct(Product product, String productId) {
        String mutation = "mutation ProductUpdate($id: ID!, $input: ProductInput!) {" +
            "  productUpdate(id: $id, input: $input) { product { id } errors { field message } }" +
            "}";

        Map<String, Object> variables = new HashMap<>();
        variables.put("id", productId);
        variables.put("input", helper.buildProductInputObject(product));

        return apiClient.mutation(mutation, variables)
            .doOnSuccess(body -> log.info("productUpdate sku={} productId={} response={}", product.getSku(), productId, body))
            .then();
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

        return apiClient.mutation(mutation, variables)
            .doOnSuccess(body -> log.info("productChannelListingUpdate sku={} productId={} response={}", product.getSku(), productId, body))
            .then();
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

        return apiClient.mutation(mutation, variables)
            .doOnSuccess(body -> log.info("productVariantChannelListingUpdate sku={} variant={} response={}", product.getSku(), variantId, body))
            .then();
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

        return apiClient.mutation(mutation, variables)
            .doOnSuccess(body -> log.info("productVariantStocksUpdate sku={} variant={} response={}", product.getSku(), variantId, body))
            .then();
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

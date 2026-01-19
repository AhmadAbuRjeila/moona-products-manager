package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moona.productsmanager.moonaproductsmanager.config.ExportProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductsExportService {

    private static final Logger log = LoggerFactory.getLogger(ProductsExportService.class);

    private final ApiClient apiClient;
    private final ProductMapper productMapper;
    private final ExportProperties exportProperties;
    private final ExcelWriter excelWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductsExportService(ApiClient apiClient,
                                 ProductMapper productMapper,
                                 ExportProperties exportProperties,
                                 ExcelWriter excelWriter) {
        this.apiClient = apiClient;
        this.productMapper = productMapper;
        this.exportProperties = exportProperties;
        this.excelWriter = excelWriter;
    }

    public Mono<List<Product>> fetchAllProducts() {
        return fetchAllProducts(null);
    }

    public Mono<List<Product>> fetchAllProducts(String createdAfterIso) {
        return fetchAllProducts(createdAfterIso, null);
    }

    public Mono<List<Product>> fetchAllProducts(String createdAfterIso, String updatedBeforeIso) {
        return fetchPage(new ArrayList<>(), exportProperties.getPageSize(), null, createdAfterIso, updatedBeforeIso, null);
    }

    public Mono<List<Product>> fetchAllProducts(String createdAfterIso, String updatedBeforeIso, String categoryId) {
        return fetchPage(new ArrayList<>(), exportProperties.getPageSize(), null, createdAfterIso, updatedBeforeIso, categoryId);
    }

    public String defaultUpdatedBeforeIso(int stalenessDays) {
        return OffsetDateTime.now(java.time.ZoneOffset.UTC).minusDays(stalenessDays).toString();
    }

    public String defaultCreatedAfterIso() {
        return OffsetDateTime.of(2026, 1, 7, 0, 0, 0, 0, ZoneOffset.UTC).toString();
    }

    public Mono<String> exportProductsToFile() {
        return exportProductsToFile(null, null, null);
    }

    public Mono<String> exportProductsToFile(String createdAfterIso) {
        if (createdAfterIso == null || createdAfterIso.isBlank()) {
            createdAfterIso = defaultCreatedAfterIso();
        }
        return exportProductsToFile(createdAfterIso, null, null);
    }

    public Mono<String> exportProductsToFile(String createdAfterIso, String updatedBeforeIso) {
        return exportProductsToFile(createdAfterIso, updatedBeforeIso, null);
    }

    public Mono<String> exportProductsToFileByCategory(String categoryId) {
        return exportProductsToFile(null, null, categoryId);
    }

    public Mono<String> exportProductsToFile(String createdAfterIso, String updatedBeforeIso, String categoryId) {
        log.info("Starting products export (channel={}, pageSize={}, createdAfter={}, updatedBefore={}, categoryId={})", exportProperties.getChannel(), exportProperties.getPageSize(), createdAfterIso, updatedBeforeIso, categoryId);
        return fetchAllProducts(createdAfterIso, updatedBeforeIso, categoryId)
            .flatMap(products -> {
                try {
                    excelWriter.exportProducts(products);
                    log.info("Export finished, wrote {} products{}", products.size(), categoryId == null || categoryId.isBlank() ? "" : " (filtered)");
                    return Mono.just("Exported " + products.size() + " products");
                } catch (IOException e) {
                    log.error("Failed to write export file", e);
                    return Mono.error(e);
                }
            });
    }

    private Mono<List<Product>> fetchPage(List<Product> accumulator, int pageSize, String afterCursor) {
        return fetchPage(accumulator, pageSize, afterCursor, null, null, null);
    }

    private Mono<List<Product>> fetchPage(List<Product> accumulator, int pageSize, String afterCursor, String createdAfterIso) {
        return fetchPage(accumulator, pageSize, afterCursor, createdAfterIso, null, null);
    }

    private Mono<List<Product>> fetchPage(List<Product> accumulator, int pageSize, String afterCursor, String createdAfterIso, String updatedBeforeIso) {
        return fetchPage(accumulator, pageSize, afterCursor, createdAfterIso, updatedBeforeIso, null);
    }

    private Mono<List<Product>> fetchPage(List<Product> accumulator, int pageSize, String afterCursor, String createdAfterIso, String updatedBeforeIso, String categoryId) {
        log.info("Fetching products page (after={}, accumulated={}, createdAfter={}, updatedBefore={}, categoryId={})", afterCursor, accumulator.size(), createdAfterIso, updatedBeforeIso, categoryId);
        return fetchProducts(pageSize, afterCursor, updatedBeforeIso, categoryId)
            .flatMap(responseJson -> {
                JsonNode root;
                try {
                    root = objectMapper.readTree(responseJson);
                } catch (IOException e) {
                    return Mono.error(e);
                }
                JsonNode productsNode = root.path("data").path("products");
                JsonNode edges = productsNode.path("edges");
                int pageCount = 0;
                if (edges.isArray()) {
                    for (JsonNode edge : edges) {
                        JsonNode node = edge.path("node");
                        Product product = productMapper.fromGraphQlNode(node, exportProperties.getChannel());
                        accumulator.add(product);
                        pageCount++;
                    }
                }
                log.info("Fetched {} products on this page (total={})", pageCount, accumulator.size());

                boolean hasNextPage = productsNode.path("pageInfo").path("hasNextPage").asBoolean(false);
                String endCursor = productsNode.path("pageInfo").path("endCursor").asText(null);
                if (hasNextPage && endCursor != null) {
                    return fetchPage(accumulator, pageSize, endCursor, createdAfterIso, updatedBeforeIso, categoryId);
                }
                log.info("No more pages; total products collected={}.", accumulator.size());
                return Mono.just(accumulator);
            })
            .map(products -> filterByCreatedAfter(products, createdAfterIso));
    }

    private List<Product> filterByCreatedAfter(List<Product> products, String createdAfterIso) {
        if (createdAfterIso == null || createdAfterIso.isBlank()) {
            return products;
        }
        try {
            OffsetDateTime threshold = OffsetDateTime.parse(createdAfterIso);
            return products.stream()
                .filter(p -> p.getCreated() != null && !p.getCreated().isBefore(threshold))
                .toList();
        } catch (Exception ex) {
            log.warn("Invalid createdAfterIso '{}', returning unfiltered list", createdAfterIso);
            return products;
        }
    }

    private Mono<String> fetchProducts(int pageSize, String after, String updatedBeforeIso) {
        return fetchProducts(pageSize, after, updatedBeforeIso, null);
    }

    private Mono<String> fetchProducts(int pageSize, String after, String updatedBeforeIso, String categoryId) {
        String productQuery = buildProductQuery();
        Map<String, Object> variables = new HashMap<>();
        variables.put("first", pageSize);
        variables.put("after", after);
        variables.put("channel", exportProperties.getChannel());
        Map<String, Object> filter = new HashMap<>();
        filter.put("isPublished", true);
        if (updatedBeforeIso != null && !updatedBeforeIso.isBlank()) {
            Map<String, Object> updatedAtFilter = new HashMap<>();
            updatedAtFilter.put("lte", updatedBeforeIso);
            filter.put("updatedAt", updatedAtFilter);
        }
        if (categoryId != null && !categoryId.isBlank()) {
            filter.put("categories", List.of(categoryId));
        }
        variables.put("filter", filter);
        return apiClient.mutation(productQuery, variables);
    }

    private String buildProductQuery() {
        return "query lastXProducts($first: Int = 10, $after: String, $channel: String, $filter: ProductFilterInput,) {\n" +
            "      products(first: $first, after: $after, channel: $channel, filter: $filter) {\n" +
            "      edges {\n" +
            "        node {\n" +
            "          id\n" +
            "          name\n" +
            "          created\n" +
            "          updatedAt\n" +
            "          thumbnail{\n" +
            "            url\n" +
            "          }\n" +
            "          category{\n" +
            "            id\n" +
            "            name\n" +
            "            parent{\n" +
            "             name\n" +
            "            }\n" +
            "          }\n" +
            "          rating\n" +
            "          channelListings {\n" +
            "            channel {\n" +
            "              slug\n" +
            "            }\n" +
            "            isAvailableForPurchase\n" +
            "            isPublished\n" +
            "          }\n" +
            "          variants {\n" +
            "            id\n" +
            "            sku\n" +
            "            name\n" +
            "            trackInventory\n" +
            "            images {\n" +
            "              url\n" +
            "            }\n" +
            "            quantityAvailable\n" +
            "            stocks {\n" +
            "              quantity\n" +
            "              quantityAllocated\n" +
            "              warehouse {\n" +
            "                id\n" +
            "              }\n" +
            "            }\n" +
            "            channelListings {\n" +
            "             channel {\n" +
            "              slug\n" +
            "             }\n" +
            "             price {\n" +
            "               amount\n" +
            "             }\n" +
            "             costPrice{\n" +
            "               amount\n" +
            "             }\n" +
            "            }" +
            "          }\n" +
            "          attributes {\n" +
            "            attribute {\n" +
            "              id\n" +
            "              name\n" +
            "            }\n" +
            "            values {\n" +
            "              id\n" +
            "              name\n" +
            "            }\n" +
            "          }\n" +
            "          weight {\n" +
            "            unit\n" +
            "            value\n" +
            "          }\n" +
            "          metadata {\n" +
            "            key\n" +
            "            value\n" +
            "          }\n" +
            "          privateMetadata {\n" +
            "            key\n" +
            "            value\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "      pageInfo {\n" +
            "        hasNextPage\n" +
            "        endCursor\n" +
            "      }\n" +
            "    }\n" +
            "  }";
    }
}

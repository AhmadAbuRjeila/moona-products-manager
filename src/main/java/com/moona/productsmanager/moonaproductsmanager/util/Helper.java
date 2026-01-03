package com.moona.productsmanager.moonaproductsmanager.util;

import com.moona.productsmanager.moonaproductsmanager.config.CatalogProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class Helper {

    private final CatalogProperties catalogProperties;

    public Helper(CatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    public Map<String, Object> buildProductCreateInputObject(Product product) {
        Map<String, Object> productCreateInput = new HashMap<>();
        productCreateInput.put("name", product.getName());
        productCreateInput.put("productType", catalogProperties.getProductType().getGroceryId());
        productCreateInput.put("category", product.getCategoryId());
        productCreateInput.put("weight", product.getWeight());
        productCreateInput.put("attributes", buildProductAttributes(product));
        return productCreateInput;
    }

    public Map<String, Object> buildProductInputObject(Product product) {
        Map<String, Object> productCreateInput = new HashMap<>();
        if (product.getName() != null) {
            productCreateInput.put("name", product.getName());
        }
        if (product.getCategoryId() != null) {
            productCreateInput.put("category", product.getCategoryId());
        }
        if (product.getWeight() != null) {
            productCreateInput.put("weight", product.getWeight());
        }
        if (product.getRating() != null) {
            productCreateInput.put("rating", product.getRating());
        }
        productCreateInput.put("attributes", buildProductAttributes(product));
        return productCreateInput;
    }

    private List<Map<String, Object>> buildProductAttributes(Product product) {
        List<Map<String, Object>> productAttributes = new ArrayList<>();

        if (product.getMinAmount() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getProductMinAmountId(), List.of(product.getMinAmount())));
        }
        if (product.getWeighted() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getProductWeightedId(), List.of(product.getWeighted())));
        }
        if (product.getBoycott() != null) {
            Map<String, Object> boycottAttribute = new HashMap<>();
            boycottAttribute.put("id", catalogProperties.getAttributes().getBoycottId());
            boycottAttribute.put("boolean", product.getBoycott());
            productAttributes.add(boycottAttribute);
        }
        if (product.getBoxSize() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getBoxSizeId(), List.of(product.getBoxSize())));
        }
        if (product.getMinOrderQuantity() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getMinOrderQuantityId(), List.of(product.getMinOrderQuantity())));
        }
        if (product.getProvider() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getProviderId(), List.of(product.getProvider())));
        }
        if (product.getProviders() != null && !product.getProviders().isEmpty()) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getProvidersId(), new ArrayList<>(product.getProviders())));
        }
        if (product.getMinStockQuantity() != null) {
            productAttributes.add(attributeWithValues(catalogProperties.getAttributes().getMinStockQuantityId(), List.of(product.getMinStockQuantity())));
        }
        return productAttributes;
    }

    private Map<String, Object> attributeWithValues(String id, Object values) {
        List<Object> rawValues;
        if (values instanceof List<?>) {
            rawValues = (List<Object>) values;
        } else {
            rawValues = List.of(values);
        }
        List<String> stringValues = rawValues.stream()
            .map(v -> v == null ? "" : v.toString())
            .toList();

        Map<String, Object> attribute = new HashMap<>();
        attribute.put("id", id);
        attribute.put("values", stringValues);
        return attribute;
    }

    public Map<String, Object> buildProductChannelListingUpdateInputObject(Product product) {
        Map<String, Object> productChannelListingAddInput = new HashMap<>();
        productChannelListingAddInput.put("channelId", product.getChannelId());
        productChannelListingAddInput.put("isPublished", product.getPublished());

        String dateString = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        productChannelListingAddInput.put("publicationDate", dateString);
        productChannelListingAddInput.put("visibleInListings", true);
        productChannelListingAddInput.put("isAvailableForPurchase", true);

        Map<String, Object> productChannelListingUpdateInput = new HashMap<>();
        productChannelListingUpdateInput.put("updateChannels", List.of(productChannelListingAddInput));
        return productChannelListingUpdateInput;
    }

    public Map<String, Object> buildProductVariantUpdateInput(Product product) {
        Map<String, Object> productVariantUpdateInput = new HashMap<>();
        productVariantUpdateInput.put("sku", product.getSku());
        productVariantUpdateInput.put("trackInventory", product.getTrackInventory());
        productVariantUpdateInput.put("attributes", new ArrayList<>()); // keep empty list
        return productVariantUpdateInput;
    }

    public Map<String, Object> buildProductVariantCreateInput(Product product) {
        Map<String, Object> productVariantCreateInputObject = new HashMap<>();
        productVariantCreateInputObject.put("product", product.getId());
        productVariantCreateInputObject.put("sku", product.getSku());
        productVariantCreateInputObject.put("trackInventory", product.getTrackInventory());
        productVariantCreateInputObject.put("stocks", buildStocksInput(product));
        productVariantCreateInputObject.put("attributes", new ArrayList<>()); // required empty list
        return productVariantCreateInputObject;
    }

    public List<Map<String, Object>> buildStocksInput(Product product) {
        Map<String, Object> stockInputObject = new HashMap<>();
        stockInputObject.put("warehouse", product.getWarehouseId());
        stockInputObject.put("quantity", product.getAvailableQuantity());
        return List.of(stockInputObject);
    }

    public List<Map<String, Object>> buildProductVariantChannelListingAddInput(Product product) {
        Map<String, Object> productVariantChannelListingAddInput = new HashMap<>();
        productVariantChannelListingAddInput.put("channelId", product.getChannelId());
        productVariantChannelListingAddInput.put("price", product.getPrice());
        productVariantChannelListingAddInput.put("costPrice", product.getCostPrice());
        return List.of(productVariantChannelListingAddInput);
    }

    public Map<String, Object> buildMapJsonObject() {
        Map<String, Object> mapJsonObject = new HashMap<>();
        mapJsonObject.put("0", "[\"variables.image\"]");
        return mapJsonObject;
    }

    public Map<String, Object> buildImageCreateOperations(String productId) {
        Map<String, Object> variablesJsonObject = new HashMap<>();
        variablesJsonObject.put("product", productId);
        variablesJsonObject.put("image", "0");
        variablesJsonObject.put("alt", "");

        Map<String, Object> operationsJsonObject = new HashMap<>();
        operationsJsonObject.put("query", "mutation ProductMediaCreate($product: ID!, $image: Upload!, $alt: String) {productMediaCreate(input: {alt: $alt, image: $image, product: $product}) {media{ id } productErrors { field \n message \n} \n} \n}");
        operationsJsonObject.put("variables", variablesJsonObject);
        return operationsJsonObject;
    }

    public List<Map<String, Object>> buildBoxLinkageMetadataInput(Product product) {
        Map<String, Object> boxSizeMetadataItem = new HashMap<>();
        boxSizeMetadataItem.put("key", "PACKAGE_SIZE");
        boxSizeMetadataItem.put("value", product.getBoxSize());

        Map<String, Object> boxBarcodeMetadataItem = new HashMap<>();
        boxBarcodeMetadataItem.put("key", "RELATED_PRODUCT");
        boxBarcodeMetadataItem.put("value", product.getSku());

        Map<String, Object> boxPriceMetadataItem = new HashMap<>();
        boxPriceMetadataItem.put("key", "RELATED_PRODUCT_PRICE");
        boxPriceMetadataItem.put("value", product.getPrice());

        return List.of(boxSizeMetadataItem, boxBarcodeMetadataItem, boxPriceMetadataItem);
    }

    public String buildProductVariantQuery() {
        return "query productVariant ($sku: String) {\n" +
            "  productVariant(sku: $sku) {\n" +
            "    id\n" +
            "    product {\n" +
            "      id\n" +
            "      name\n" +
            "      category {\n" +
            "        name\n" +
            "      }\n" +
            "      channelListings {\n" +
            "        channel {\n" +
            "          slug\n" +
            "        }\n" +
            "        isPublished\n" +
            "      }\n" +
            "      attributes {\n" +
            "        attribute {\n" +
            "          id\n" +
            "          slug\n" +
            "          name\n" +
            "        }\n" +
            "        values {\n" +
            "          id\n" +
            "          slug\n" +
            "          name\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";
    }
}

package com.moona.productsmanager.moonaproductsmanager.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.moona.productsmanager.moonaproductsmanager.config.CatalogProperties;
import com.moona.productsmanager.moonaproductsmanager.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private final CatalogProperties catalogProperties;

    public ProductMapper(CatalogProperties catalogProperties) {
        this.catalogProperties = catalogProperties;
    }

    public Product fromGraphQlNode(JsonNode node, String channel) {
        Product product = new Product();
        product.setId(node.path("id").asText(null));
        product.setName(node.path("name").asText(null));
        product.setRating(node.path("rating").isNumber() ? node.path("rating").asDouble() : null);

        JsonNode category = node.path("category");
        if (!category.isMissingNode()) {
            product.setCategoryId(category.path("id").asText(null));
            product.setCategoryName(category.path("name").asText(null));
            JsonNode parent = category.path("parent");
            if (!parent.isMissingNode()) {
                product.setParentCategoryName(parent.path("name").asText(null));
            }
        }

        JsonNode thumbnail = node.path("thumbnail");
        if (!thumbnail.isMissingNode()) {
            product.setImageUrl(thumbnail.path("url").asText(null));
        }

        JsonNode channelListings = node.path("channelListings");
        if (channelListings.isArray()) {
            channelListings.forEach(cl -> {
                String slug = cl.path("channel").path("slug").asText();
                if (channel.equalsIgnoreCase(slug)) {
                    product.setPublished(cl.path("isPublished").asBoolean());
                }
            });
        }

        JsonNode variants = node.path("variants");
        if (variants.isArray()) {
            variants.forEach(variant -> {
                if (product.getSku() == null) {
                    product.setSku(variant.path("sku").asText(null));
                }
                if (product.getAvailableQuantity() == null) {
                    product.setAvailableQuantity(variant.path("quantityAvailable").isNumber()
                        ? variant.path("quantityAvailable").asInt() : null);
                }
                if (product.getTrackInventory() == null) {
                    product.setTrackInventory(variant.path("trackInventory").isMissingNode() ? null : variant.path("trackInventory").asBoolean());
                }

                JsonNode variantChannelListings = variant.path("channelListings");
                if (variantChannelListings.isArray()) {
                    variantChannelListings.forEach(vcl -> {
                        String slug = vcl.path("channel").path("slug").asText();
                        if (channel.equalsIgnoreCase(slug)) {
                            if (vcl.path("price").path("amount").isNumber()) {
                                product.setPrice(vcl.path("price").path("amount").asDouble());
                            }
                            if (vcl.path("costPrice").path("amount").isNumber()) {
                                product.setCostPrice(vcl.path("costPrice").path("amount").asDouble());
                            }
                        }
                    });
                }

                if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
                    JsonNode variantImages = variant.path("images");
                    if (variantImages.isArray() && variantImages.size() > 0) {
                        product.setImageUrl(variantImages.get(0).path("url").asText(null));
                    }
                }
            });
        }

        JsonNode attributes = node.path("attributes");
        if (attributes.isArray()) {
            attributes.forEach(attribute -> {
                String attributeId = attribute.path("attribute").path("id").asText();
                JsonNode values = attribute.path("values");
                if (!values.isArray() || values.size() == 0) {
                    return;
                }
                String attributeValue = values.get(0).path("name").asText(null);
                if (attributeValue == null || attributeValue.isBlank()) {
                    return;
                }
                if (catalogProperties.getAttributes().getProductMinAmountId().equals(attributeId)) {
                    product.setMinAmount(attributeValue);
                } else if (catalogProperties.getAttributes().getProductWeightedId().equals(attributeId)) {
                    product.setWeighted(attributeValue);
                } else if (catalogProperties.getAttributes().getBoxSizeId().equals(attributeId)) {
                    try {
                        product.setBoxSize(Integer.parseInt(attributeValue));
                    } catch (NumberFormatException ignored) { }
                }
            });
        }

        JsonNode weight = node.path("weight");
        if (!weight.isMissingNode() && weight.path("value").isNumber()) {
            product.setWeight(weight.path("value").asDouble());
        }
        return product;
    }
}


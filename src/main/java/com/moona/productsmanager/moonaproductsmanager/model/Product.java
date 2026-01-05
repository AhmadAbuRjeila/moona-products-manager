package com.moona.productsmanager.moonaproductsmanager.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private String id;
    private String sku;
    private String name;
    private Double price;
    private Double costPrice;
    private Integer availableQuantity;
    private String categoryId;
    private String categoryName;
    private String parentCategoryName;
    private String weighted;
    private Double weight;
    private String minAmount;
    private Boolean trackInventory;
    private Boolean isPublished;
    private String imageUrl;
    private String channelId;
    private String warehouseId;
    private Double rating;
    private String boxItemBarcode;
    private Integer boxSize;
    private Integer neededQuantity;
    private Boolean isBoycott;
    private Integer minOrderQuantity;
    private String provider;
    private List<String> providers = new ArrayList<>();
    private Integer minStockQuantity;
    private OffsetDateTime created;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(Double costPrice) {
        this.costPrice = costPrice;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getParentCategoryName() {
        return parentCategoryName;
    }

    public void setParentCategoryName(String parentCategoryName) {
        this.parentCategoryName = parentCategoryName;
    }

    public String getWeighted() {
        return weighted;
    }

    public void setWeighted(String weighted) {
        this.weighted = weighted;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(String minAmount) {
        this.minAmount = minAmount;
    }

    public Boolean getTrackInventory() {
        return trackInventory;
    }

    public void setTrackInventory(Boolean trackInventory) {
        this.trackInventory = trackInventory;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public void setPublished(Boolean published) {
        isPublished = published;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getBoxItemBarcode() {
        return boxItemBarcode;
    }

    public void setBoxItemBarcode(String boxItemBarcode) {
        this.boxItemBarcode = boxItemBarcode;
    }

    public Integer getBoxSize() {
        return boxSize;
    }

    public void setBoxSize(Integer boxSize) {
        this.boxSize = boxSize;
    }

    public Integer getNeededQuantity() {
        return neededQuantity;
    }

    public void setNeededQuantity(Integer neededQuantity) {
        this.neededQuantity = neededQuantity;
    }

    public Boolean getBoycott() {
        return isBoycott;
    }

    public void setBoycott(Boolean boycott) {
        isBoycott = boycott;
    }

    public Integer getMinOrderQuantity() {
        return minOrderQuantity;
    }

    public void setMinOrderQuantity(Integer minOrderQuantity) {
        this.minOrderQuantity = minOrderQuantity;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<String> getProviders() {
        return providers;
    }

    public void setProviders(List<String> providers) {
        this.providers = providers != null ? providers : new ArrayList<>();
    }

    public Integer getMinStockQuantity() {
        return minStockQuantity;
    }

    public void setMinStockQuantity(Integer minStockQuantity) {
        this.minStockQuantity = minStockQuantity;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public void addProvider(String providerToAdd) {
        if (providerToAdd == null || providerToAdd.isBlank()) {
            return;
        }
        if (providers == null) {
            providers = new ArrayList<>();
        }
        if (!providers.contains(providerToAdd)) {
            providers.add(providerToAdd);
        }
    }
}

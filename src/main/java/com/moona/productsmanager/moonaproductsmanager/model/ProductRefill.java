package com.moona.productsmanager.moonaproductsmanager.model;

public class ProductRefill {
    private String sku;
    private String name;
    private Double costPrice;
    private Integer availableQuantity;
    private Integer neededQuantity;

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

    public Integer getNeededQuantity() {
        return neededQuantity;
    }

    public void setNeededQuantity(Integer neededQuantity) {
        this.neededQuantity = neededQuantity;
    }
}


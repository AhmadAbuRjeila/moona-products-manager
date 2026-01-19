package com.moona.productsmanager.moonaproductsmanager.model;

public class CategoryExportRow {
    private String parentId;
    private String parentName;
    private String childId;
    private String childName;

    public CategoryExportRow() {
    }

    public CategoryExportRow(String parentId, String parentName, String childId, String childName) {
        this.parentId = parentId;
        this.parentName = parentName;
        this.childId = childId;
        this.childName = childName;
    }

    public static CategoryExportRow parentOnly(String parentId, String parentName) {
        return new CategoryExportRow(parentId, parentName, null, null);
    }

    public static CategoryExportRow parentAndChild(String parentId, String parentName, String childId, String childName) {
        return new CategoryExportRow(parentId, parentName, childId, childName);
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getChildName() {
        return childName;
    }

    public void setChildName(String childName) {
        this.childName = childName;
    }
}


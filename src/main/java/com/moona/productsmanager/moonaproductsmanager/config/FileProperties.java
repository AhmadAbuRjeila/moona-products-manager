package com.moona.productsmanager.moonaproductsmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "files")
public class FileProperties {
    private String imageDefaultContentType = "image/png";

    public String getImageDefaultContentType() {
        return imageDefaultContentType;
    }

    public void setImageDefaultContentType(String imageDefaultContentType) {
        this.imageDefaultContentType = imageDefaultContentType;
    }
}


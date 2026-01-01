package com.moona.productsmanager.moonaproductsmanager.util;

import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class FileUtils {

    public Map<String, String> readFolderItems(String path) {
        Map<String, String> itemsNames = new HashMap<>();
        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files == null) {
            return itemsNames;
        }
        for (File fileEntry : Objects.requireNonNull(files)) {
            if (!fileEntry.isFile()) {
                continue;
            }
            String[] imageNameParts = fileEntry.getName().split("\\.");
            if (imageNameParts.length >= 2) {
                itemsNames.put(imageNameParts[0], imageNameParts[1]);
            }
        }
        return itemsNames;
    }
}


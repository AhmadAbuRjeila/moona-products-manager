package com.moona.productsmanager.moonaproductsmanager.cli;

import com.moona.productsmanager.moonaproductsmanager.service.CategoryProductsRatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Component
public class CategoryRatingExportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryRatingExportRunner.class);

    private static final List<CategoryRequest> CATEGORY_REQUESTS = List.of(
            new CategoryRequest("مواد تموينية ومعلبات", "Q2F0ZWdvcnk6MTMw", 980000d, 999000d),
            new CategoryRequest("الارز والمعكرونة", "Q2F0ZWdvcnk6MTI=", 960000d, 979000d),
            new CategoryRequest("الزيوت والسمنة", "Q2F0ZWdvcnk6MTk=", 940000d, 959000d),
            new CategoryRequest("اللحوم والأسماك", "Q2F0ZWdvcnk6Mw==", 920000d, 939000d),
            new CategoryRequest("سلطات", "Q2F0ZWdvcnk6MzQ=", 900000d, 919000d),
            new CategoryRequest("الالبان والاجبان والبيض", "Q2F0ZWdvcnk6Mg==", 880000d, 899000d),
            new CategoryRequest("الخبز والمعجنات", "Q2F0ZWdvcnk6NA==", 860000d, 879000d),
            new CategoryRequest("المياه المعدنية", "Q2F0ZWdvcnk6NQ==", 840000d, 859000d),
            new CategoryRequest("مجمدات", "Q2F0ZWdvcnk6MjE=", 820000d, 839000d),
            new CategoryRequest("المكسرات والمجففات والبذور", "Q2F0ZWdvcnk6OA==", 800000d, 819000d),
            new CategoryRequest("البوظة والمثلجات", "Q2F0ZWdvcnk6OQ==", 780000d, 799000d),
            new CategoryRequest("الشوكلاتة والسكاكر", "Q2F0ZWdvcnk6Ng==", 760000d, 779000d),
            new CategoryRequest("المشروبات الباردة", "Q2F0ZWdvcnk6MTE=", 740000d, 759000d),
            new CategoryRequest("شيبسات وتسالي", "Q2F0ZWdvcnk6MTg=", 720000d, 739000d),
            new CategoryRequest("صوصات", "Q2F0ZWdvcnk6MTE0", 700000d, 719000d),
            new CategoryRequest("بهارات", "Q2F0ZWdvcnk6MzI=", 680000d, 699000d),
            new CategoryRequest("القهوة والشاي", "Q2F0ZWdvcnk6MTU=", 660000d, 679000d),
            new CategoryRequest("صحي وحبوب الافطار", "Q2F0ZWdvcnk6MzY=", 640000d, 659000d),
            new CategoryRequest("مناديل ورقية", "Q2F0ZWdvcnk6MjA=", 620000d, 639000d),
            new CategoryRequest("منظفات وعناية منزلية", "Q2F0ZWdvcnk6MTQ=", 600000d, 619000d),
            new CategoryRequest("مستلزمات الاطفال", "Q2F0ZWdvcnk6MTA=", 580000d, 599000d),
            new CategoryRequest("بلاستيك وقصدير", "Q2F0ZWdvcnk6MjI=", 560000d, 579000d),
            new CategoryRequest("العناية الشخصية", "Q2F0ZWdvcnk6Nw==", 540000d, 559000d),
            new CategoryRequest("صيدلية", "Q2F0ZWdvcnk6MTU4", 520000d, 539000d),
            new CategoryRequest("اكل حيوانات", "Q2F0ZWdvcnk6MzU=", 500000d, 519000d),
            new CategoryRequest("اكسسوارات", "Q2F0ZWdvcnk6Mzc=", 480000d, 499000d),
            new CategoryRequest("ادوات منزلية", "Q2F0ZWdvcnk6OTY=", 460000d, 479000d),
            new CategoryRequest("منتجات متنوعة", "Q2F0ZWdvcnk6MTc=", 440000d, 459000d)
//            new CategoryRequest("خضراوات وفواكه", "Q2F0ZWdvcnk6MzE=", 420000d, 439000d)
    );

    private final CategoryProductsRatingService ratingService;

    public CategoryRatingExportRunner(CategoryProductsRatingService ratingService) {
        this.ratingService = ratingService;
    }

    @Override
    public void run(String... args) {
        boolean triggered = false;
        int exitCode = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("categoryRatingExport")) {
                triggered = true;
                try {
                    if (CATEGORY_REQUESTS.isEmpty()) {
                        throw new IllegalStateException("No category requests configured; update CATEGORY_REQUESTS list");
                    }
                    Map<String, String> options = parseOptions(args, i, arg);
                    String createdAfter = options.get("createdAfter");
                    String updatedBefore = options.get("updatedBefore");
                    Boolean published = parseBoolean(options.get("published"));
                    for (CategoryRequest request : CATEGORY_REQUESTS) {
                        ratingService.exportCategoryWithRatings(request.categoryId(), request.min(), request.max(),
                                        createdAfter, updatedBefore, published)
                                .doOnSuccess(msg -> log.info(msg))
                                .block();
                    }
                } catch (Exception ex) {
                    exitCode = 1;
                    log.error("categoryRatingExport failed", ex);
                }
                break;
            }
        }
        if (triggered) {
            log.info("categoryRatingExport task finished; exiting with code {}", exitCode);
            System.exit(exitCode);
        }
    }

    private Map<String, String> parseOptions(String[] args, int commandIndex, String commandToken) {
        Map<String, String> options = new HashMap<>();
        extractInlineOptions(commandToken, options);
        for (int i = commandIndex + 1; i < args.length; i++) {
            String token = args[i];
            if (token.contains("Export") && !token.contains("=")) {
                // likely another task flag; stop parsing to avoid consuming next task's args
                break;
            }
            putIfPresent(token, options);
        }
        return options;
    }

    private void extractInlineOptions(String commandToken, Map<String, String> options) {
        String remainder = commandToken.replaceFirst("^categoryRatingExport", "");
        remainder = remainder.replaceFirst("^[,:]", "");
        if (remainder.isBlank()) {
            return;
        }
        for (String token : remainder.split(",")) {
            putIfPresent(token, options);
        }
    }

    private void putIfPresent(String token, Map<String, String> options) {
        if (token == null || !token.contains("=")) {
            return;
        }
        String cleaned = token.replaceFirst("^--", "");
        String[] parts = cleaned.split("=", 2);
        if (parts.length == 2 && !parts[0].isBlank()) {
            options.put(parts[0].trim(), parts[1].trim());
        }
    }

    private Boolean parseBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private record CategoryRequest(String categoryName, String categoryId, double min, double max) {
    }
}

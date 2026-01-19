# Category Products Rating & Sorting Plan

## Goals
- Add a service to fetch products for a specific category, assign ratings so adjacent-size items stay near each other, and export in the same Excel format used today.

## Tasks
1) **Understand existing flow**: Review `ProductsExportService`, `ExcelWriter`, and models to confirm current export shape and how ratings are mapped. **Status:** done
2) **Define rating strategy**: Specify algorithm to group adjacent products (Arabic names, size-only differences) and assign unique ratings within the category’s allowed range. **Status:** done
3) **Service design**: Add a dedicated service that reuses `ProductsExportService` fetching + `ExcelWriter` export; expose method(s) that accept a category and rating range.
4) **Implement sorting + rating logic**: Build the grouping/comparator that clusters similar names and orders by size; ensure no duplicate ratings are assigned.
5) **Wire integration**: Provide entry point (e.g., CLI/REST if present) to trigger the category export using the new service.
6) **Testing**: Add/extend unit tests for rating assignment, ordering, and export invocation; run the test suite.
7) **Docs & usage**: Document how to run the new category-specific export and the required inputs (category ID, rating bounds, size units heuristics).

## Task 1 findings
- `ProductsExportService` fetches Saleor products via GraphQL (paged), filters by optional createdAfter/updatedBefore/categoryId, maps with `ProductMapper`, and exports with `ExcelWriter.exportProducts`; output file `MoonaProducts-<date>.xlsx`.
- GraphQL fields captured: id, name, created, updatedAt, category (id/name/parent), rating, thumbnail, variants (sku, quantityAvailable, trackInventory, channel prices/costs, images), attributes (min amount, weighted, box size), weight, metadata.
- `ProductMapper` only keeps the first variant’s sku/quantity/trackInventory and first image if no product thumbnail; sets rating from product rating numeric field.
- `ExcelWriter` writes columns: ID, SKU, NAME, COST_PRICE, PRICE, CATEGORY_ID, CATEGORY, PARENT_CATEGORY, RATING, AVAILABLE_QUANTITY, MIN_AMOUNT, WEIGHTED, WEIGHT, TRACK_INVENTORY, PUBLISHED, IMAGE.
- CLI `ProductsExportRunner` already supports `productsExport` with optional categoryId argument, delegating to `ProductsExportService.exportProductsToFile(createdAfter, updatedBefore, categoryId)`.

## Task 2 rating strategy
- **Inputs**: categoryId; rating range `[minRating, maxRating]` (inclusive, integer or 0.5 steps); products fetched via existing `ProductsExportService.fetchAllProducts(createdAfter, updatedBefore, categoryId)`.
- **Normalization & grouping**: For each product name (Arabic), strip whitespace/punctuation, normalize Arabic digits/letters, extract size tokens using regex for numbers + units (e.g., `مل`, `لتر`, `جم`, `غ`, `كغ`, `كجم`, `كغم`, `حبة`), convert to canonical unit (ml/g) when possible; base key = name minus size token(s). Group products by base key to keep adjacent sizes together.
- **Ordering inside group**: Sort by normalized size value (ascending); when no size parsed, keep original name order; secondary sort by SKU for determinism.
- **Group ordering**: Order groups by parent category name then base key (lexicographically) to keep stable output.
- **Rating assignment**: Determine step = `(maxRating - minRating) / (N-1)` where N is total products in category; if step < 0.5, clamp to 0.5 and adjust range consumption; assign ratings in the sorted sequence ensuring strictly decreasing or increasing (choose descending from maxRating), rounding to 1 decimal to avoid duplicates; if range too narrow for unique ratings, fall back to incremental jitter of +0.1 until unique.
- **No duplicates**: Keep a `Set<Double>` of assigned ratings; if collision occurs after rounding, nudge by smallest increment (0.1) within bounds; if bounds exceeded, log/warn and re-run distribution with smaller step.
- **Output**: Sorted list with updated `Product.rating`; hand off to `ExcelWriter.exportProducts` to generate the same export format.
- **Fallbacks**: If size cannot be parsed for any product in a group, maintain original order and still allocate unique ratings by position; if rating range missing, use defaults per category config (to be added) or wide default (e.g., 10..1).

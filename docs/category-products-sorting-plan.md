# Category Products Rating & Sorting Plan

## Goals
- Add a service to fetch products for a specific category, assign ratings so adjacent-size items stay near each other, and export in the same Excel format used today.

## Tasks
1) **Understand existing flow**: Review `ProductsExportService`, `ExcelWriter`, and models to confirm current export shape and how ratings are mapped. **Status:** done
2) **Define rating strategy**: Specify algorithm to group adjacent products (Arabic names, size-only differences) and assign unique ratings within the category’s allowed range.
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

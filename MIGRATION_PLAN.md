# Migration Plan: deprecated Vert.x `products-manager` into Spring Boot `Moona-Products-Manager`

## Scope & Current State
- Host app: Gradle + Spring Boot 4.0.1, Java 21 (`src/main/java/com/moona/productsmanager/moonaproductsmanager`).
- Source app: Maven Vert.x 4.0.2, Java 8 (`deprecated-products-manager/products-manager`), CLI/verticles + Excel/HTTP utilities, resources (`resources/products/`, SQL scripts, backup Excel/SQL dumps).
- Key Java packages: `com.moona.productsmanager.starter` (MainVerticle, ProductsExport/Update*, UploadImages, ErpReportScraper, Add/RemoveToCollection, ReadFolderFiles), DTOs, utils (ApiClient, ExcelReader/Writer, JsonParser, FileUtils, Helper, Constants).
- Dependencies to port: Vert.x core/web-client, Apache POI (poi/poi-ooxml), commons-io, Apache httpclient/httpmime, logging (slf4j-simple).

## Target Architecture (Spring Boot)
- Replace Vert.x bootstrapping with Spring Boot application + command runners for CLI-style tasks.
- HTTP client: Spring WebClient (reactive) or RestTemplate/HttpClient adapter; preserve timeouts/retries.
- Scheduling: Spring `@Scheduled` or `ApplicationRunner` for batch jobs (export/import).
- Config: `application.yml` with profiles; map existing constants/URLs/auth to properties bound via `@ConfigurationProperties`.
- Packaging: Single Gradle module; fat jar via Spring Boot plugin.

## Phases & Steps
1) **Inventory & Gap Analysis**
   - List all entrypoints in `starter/` and their dependencies; document required configs (URLs, tokens, file paths) currently hard-coded in `Constants`/helpers.
   - Review resource files (`resources/products/*.xlsx`, SQL scripts, backup-scripts) to decide which remain, move to external storage, or document as samples.
   - Confirm external systems: GraphQL endpoints, auth headers, file system expectations.

2) **Build & Dependency Alignment**
   - Add to `build.gradle`: `spring-boot-starter-web` (or webflux), `spring-boot-configuration-processor`, Apache POI, commons-io, Apache httpclient/httpmime, and optional `reactor-netty` if using WebClient.
   - Remove Maven-specific plugins; ensure Java toolchain stays at 21 or adjust if backward compatibility needed.
   - Drop Vert.x entirely; no temporary client dependency.

3) **Configuration Migration** *(DONE: catalog/API properties scaffolded in application.yml)*
   - Convert constants to `application.yml` keys (API base URL, tokens, channel slug, paths, timeouts, pagination size, collection/category IDs).
   - Create `@ConfigurationProperties` class to supply these values; add profile overrides for dev/stage/prod.
   - Externalize secrets (env vars or config server); avoid committing creds.

4) **Code Porting**
   - Create Spring `@SpringBootApplication` runners/`@Service` equivalents for each CLI/task (ProductsExport, Update, UploadImages, ErpReportScraper, Add/RemoveToCollection, ReadFolderFiles).
   - Replace Vert.x `WebClient` calls with Spring WebClient/RestTemplate; map async handling (Futures/Promises) to `Mono`/`Flux` or `CompletableFuture`.
   - Replace Vert.x JSON usage with Jackson (ObjectMapper); reuse DTOs after adjusting annotations if needed.
   - Move utility classes into `src/main/java/com/moona/productsmanager/.../utils`; refactor for Spring beans where stateful (ApiClient), keep static helpers where appropriate. **(PARTIAL: ExcelReader/Writer, JsonParser, Helper, FileUtils, ImageUploadClient ported)**
   - Introduce scheduling for recurring tasks; for one-off exports/imports, expose `CommandLineRunner` beans with task selection via args or Spring Shell.
   - Handle file IO paths via properties; decide on working directories for Excel import/export.

5) **Resources & Data Files**
   - Keep `resources/products/*.xlsx` intact; treat them as-is for imports (do not reshape/rename during migration).
   - Decide which Excel/SQL files stay in repo vs. move to `docs/examples/` or external storage; update `.gitignore` to avoid large/binary churn.
   - Migrate SQL scripts under `src/main/resources/scripts/` if still used; add README notes for DB usage.

6) **Testing Strategy**
   - Port any existing tests (none present) by adding unit tests for DTO mappers, Excel reader/writer, and HTTP client wrapper.
   - Add integration tests with mock HTTP (MockWebServer/WebTestClient) to assert GraphQL payloads and error handling mirrors Vert.x behavior.
   - Add regression tests for exported Excel formats (schema validation).

7) **Verification & Cutover**
   - Run `./gradlew test` and targeted task runs via `--args="taskName"` or dedicated Spring Shell commands.
   - Smoke test critical flows: product export/import, image upload, ERP scraper, collection add/remove.
   - Compare outputs between old Vert.x jar and new Spring Boot app for a sample dataset; document deltas.
   - Remove temporary Vert.x deps once parity confirmed; archive or delete `deprecated-products-manager/` after final sign-off.

## Suggested Work Sequence (Incremental)
1. Align Gradle deps and add config scaffolding (`application.yml`, properties class). **(DONE: deps updated)**
2. Port `ApiClient` to Spring HTTP client; ensure auth and headers configurable. **(DONE: WebClient config + ApiProperties + ApiClient service)**
3. Port DTOs/utilities (ExcelReader/Writer, JsonParser) and validate via unit tests. **(DONE)**
4. Port `ProductsExport` as a `CommandLineRunner`; verify output matches legacy. **(DONE: ProductsExportService + runner)**
5. Port `ProductsUpdate` and `ProductsUpdateRank` runners; confirm update semantics and error handling.
6. Port `ProductsImages` and `ProductsImageUpdate` (image upload/update flows); validate file handling and retries.
7. Port `UploadImages` for bulk operations; ensure path configuration is externalized.
8. Port `AddToCollection` and `RemoveFromCollection`; validate collection IDs and responses.
9. Port `ReadFolderFiles` utility; verify filesystem expectations and filtering.
10. Port `ErpReportScraper` and related tasks; align scheduling if needed. **(IN PROGRESS: ErpProperties, ErpReportClient, ErpProductMapper, ErpIngestService, ErpReportRunner)**
11. Add scheduling/CLI switches as needed; wire args parsing or Spring Shell.
12. Clean up resources, docs, and remove Vert.x remnants; finalize cutover checklist.

## Commands (after porting)
```bash
# run tests
./gradlew test

# run a specific task implemented as CommandLineRunner (example)
./gradlew bootRun --args="productsExport --channel ramallah"
```

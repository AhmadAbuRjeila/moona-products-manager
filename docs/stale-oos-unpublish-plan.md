# Task Tracker: Unpublish Stale Out-of-Stock Products

## Progress Snapshot
- [x] Define scope, rules, and execution mode (manual CLI only; channel = `exportProperties.channel`; staleness = updatedAt <= now-14d; OOS = availableQuantity <= 0; skip already-unpublished or trackInventory=false)
- [x] Wire data path (export filter, mapper updatedAt) and job orchestration
- [ ] Expose CLI runner and defaults
- [ ] Add logging/metrics and safeguards
- [ ] Add tests and runbook steps; verify via dry-run

## Tasks (ordered)
1) **Model & Export Support**
   - [x] Ensure `Product` + export mapper capture `updatedAt` from API responses.
   - [x] Extend `ProductsExportService` to accept `updatedBefore` (ISO) and apply `updatedAt: { lte: <value> }` while honoring configured channel.
   - [x] Add default `updatedBefore = now - 14d` computation helper.

2) **Job Orchestration** (`StaleOosUnpublishJob`)
   - [ ] Call export with `updatedBefore` (default or overridden) and page size from config.
   - [ ] Filter in-memory: `availableQuantity <= 0`, `trackInventory == true`, `published == true`.
   - [ ] Map updates using existing update DTOs: set `published=false` only (no other fields touched).
   - [ ] Batch flag-only updates through `ProductsUpdateService` with existing retry/backoff.
   - [ ] Support `--limit` to cap processed items per run.

3) **CLI Entry Point**
   - [ ] Add `CommandLineRunner` task key `staleOosUnpublish`.
   - [ ] Flags: `--dry-run`, `--updated-before <iso>`, `--page-size <n>`, `--limit <n>`.
   - [ ] Exit non-zero on failures; print summary counts.

4) **Config & Observability**
   - [ ] New properties `stale-oos-unpublish.page-size`, `stale-oos-unpublish.limit`, `stale-oos-unpublish.default-staleness-days` (14), optional backoff/retry knobs; env overrides allowed.
   - [ ] Structured logs per batch: exported, filtered, skipped-unpublished, updated; error summary.
   - [ ] Optional Micrometer counters (e.g., `stale_oos_unpublish.exported`, `filtered`, `updated`, `skipped_unpublished`, `errors`).

5) **Safety & Edge Cases**
   - [ ] Idempotence: skip `published=false` or `trackInventory=false` items.
   - [ ] Rate limiting: reuse batching; small delay/backoff on update errors if not already present.
   - [ ] Partial failures: log failed SKUs, continue, return non-zero on any failures.
   - [ ] Allow overriding `updatedBefore` via CLI for re-runs.

6) **Testing & Dry-Run Validation**
   - [ ] Unit: mapper captures `updatedAt`; filter selects `availableQuantity <= 0`; skips already-unpublished/trackInventory=false.
   - [ ] Service: mock export/update to assert dry-run vs real mode, batching, counts, limit enforcement.
   - [ ] CLI: arg parsing for flags produces expected parameters and summary output.
   - [ ] Run dry-run in real env; review logs and candidate SKUs before first live run.

## Rollout Notes
- First run must be `--dry-run`; review logs for candidate count and SKUs.
- Live run via CLI off-peak; monitor update errors/duration.
- If automation is desired later, add cron on top of `StaleOosUnpublishJob` (not in scope now).

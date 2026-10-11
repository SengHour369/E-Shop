# Runtime catalog seed

`catalog.seed.CatalogDataSeeder` runs once per application startup when `CATALOG_SEED_ENABLED=true`. Local Compose enables it; the default is false.

The seed supplies 100 categories across 10 departments, 100 category icons, and 100 product brand entries with bundled SVG demo wordmarks. Brand tiles are placeholders, not official brand logos. No product SKUs, stock, prices, payments, or user accounts are created.

Review the editable source lists in `catalog-service/src/main/resources/seed/`. Vector assets live in `seed-assets/`. Category and brand image URLs use the existing gateway routes and work without external image hosting.

All inserts run in one transaction under a PostgreSQL advisory lock. Matching names (case insensitive, including deleted categories) are skipped. Existing descriptions, icons and brand images are never overwritten. Repeated startup adds zero duplicates. Disabling the flag does not remove previously seeded data.

Run local services with `compose.local.yaml` included. For production, leave the flag false unless starter data is explicitly wanted.

# Product request processing

The product controller keeps the existing URLs and JSON structure. Product reads now
separate filtering, database loading and response mapping:

```text
ProductController
  -> ProductServiceImpl: validate pagination / select filter / query page
  -> ProductResponseService: batch-load data for that page
  -> ProductMapper: map already-loaded data to response DTOs
```

Previously, the mapper queried SKUs for every product, attributes and inventory for
every SKU, and values for every attribute. Lazy image access added more queries.
The number of database round trips grew with every product and variant returned.

The new read path performs these operations for a full page:

1. Select the product page, ordered by descending ID.
2. Count matching products for pagination metadata when needed by Spring Data.
3. Fetch product images for the selected IDs.
4. Fetch SKUs with their product and image in one query.
5. Fetch attributes for all selected SKU IDs.
6. Fetch values for all selected attribute IDs.
7. Fetch inventory for all selected SKU IDs.
8. Fetch eligible promotions for all selected SKU IDs in one batch.

Empty pages skip related-data queries; pages without SKUs or attributes also skip
unneeded queries. Images are fetched **after** pagination to avoid paginating a
collection join in memory. Inventory is loaded fresh for each request; no product or
stock response cache is introduced.

`ProductDetails` holds lookup maps only for the current request. `ProductMapper` has
no repository dependencies. All product page methods use the batch response service,
including category, subcategory and search filters. Single-product and write responses
use the same mapping path. Reads run in the product service's read-only transaction;
write responses remain in their write transaction.

Additional changes:

- Missing `criteria_type` lists products instead of throwing a null-unboxing error.
- `criteria_type: 4` filters active products without needing `criteria_value`.
- HTTP pagination requires `page >= 1` and `1 <= size <= 100`.
- Multipart product requests reuse Spring's configured ObjectMapper.
- SQL printing is disabled by default; set `JPA_SHOW_SQL=true` for diagnostics.
- Product status updates map the response inside a transaction.

## Verification

```powershell
mvn -pl catalog-service -am test
```

`ProductReadQueryTest` uses an isolated H2 database and Hibernate statement statistics.
It compares full pages of 2 and 20 products, each with images, 2 SKUs, attributes,
values and inventory. It asserts 8 statements for either size and checks the nested
response data. `ProductFilterTest` covers omitted criteria, active filtering and page
bounds. These are query-count checks, not a production latency benchmark.

Image uploads still depend on Cloudinary response time. Database search speed also
depends on production data volume and indexes. The changes here remove repeated reads
while constructing product responses; they do not measure or remove external upload latency.

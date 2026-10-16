package com.example.eshop.catalog.service;

import com.example.eshop.common.imports.ImportContracts.*;
import com.example.eshop.common.imports.ImportTool;
import com.example.eshop.common.security.LivePermissionService;
import com.example.eshop.common.security.CurrentActor;
import com.example.eshop.catalog.repository.*;
import com.example.eshop.catalog.dto.request.*;
import com.example.eshop.catalog.service.impl.ProductSkuServiceImpl;
import com.example.eshop.catalog.model.ImportReceipt;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Owns all business writes and the receipt in ONE catalog transaction. */
@Service
@RequiredArgsConstructor
public class CatalogImportService {

    private final LivePermissionService permissions;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final InventoryRepository inventory;
    private final CategoryService categoryService;
    private final ProductSkuServiceImpl skuService;
    private final InventoryService inventoryService;
    private final com.example.eshop.catalog.controller.AiCatalogController promotions;
    private final com.example.eshop.catalog.util.SkuGeneratorUtil skuGenerator;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final EntityManager em;
    private final com.example.eshop.common.imports.ImportApprovalProof proof;
    private final PromotionRepository promotionRepository;
    private final BranchService branchService;
    private final BranchRepository branches;
    private final SubCategoryRepository subcategories;
    private final ProductService productService;

    private void authorize(Row row, boolean execute) {
        if (row == null
                || row.tool() == null
                || !row.tool().enabled
                || !row.tool().domainTool()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported import tool"
            );
        }

        authorize(row, execute, permissions.current());
    }

    private void authorize(
            Row row,
            boolean execute,
            com.example.eshop.common.security.PermissionSnapshot actor
    ) {
        if (row == null
                || row.tool() == null
                || !row.tool().enabled
                || !row.tool().domainTool()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported import tool"
            );
        }

        if (!actor.permits(row.tool().permission)
                || (execute
                ? !actor.permits("IMPORT_EXECUTE")
                || !actor.permits("IMPORT_APPROVE")
                : !actor.permits("IMPORT_UPLOAD")
                && !actor.permits("IMPORT_REVIEW"))) {
            throw new AccessDeniedException(
                    "Import domain permission required"
            );
        }
    }

    @Transactional(readOnly = true)
    public Validation validate(Row row) {
        authorize(row, false);
        return check(row);
    }

    @Transactional(readOnly = true)
    public List<Validation> validateBulk(List<Row> rows) {
        if (rows == null || rows.isEmpty() || rows.size() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Validate between 1 and 500 rows"
            );
        }

        var validationActor = permissions.current();
        Set<ImportTool> authorized = new HashSet<>();

        for (Row row : rows) {
            if (row == null || row.tool() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid import row"
                );
            }

            if (authorized.add(row.tool())) {
                authorize(row, false, validationActor);
            }
        }

        return rows.stream()
                .map(row -> check(row, validationActor))
                .toList();
    }

    private Validation check(Row row) {
        return check(row, permissions.current());
    }

    private Validation check(
            Row row,
            com.example.eshop.common.security.PermissionSnapshot validationActor
    ) {
        List<String> errors = new ArrayList<>(
                row.tool().validate(row.values())
        );

        if (!errors.isEmpty()) {
            return new Validation(errors, null, null);
        }

        Map<String, String> v = row.values();
        String duplicate = null, affected = null;

        switch (row.tool()) {
            case CATEGORY_BULK_UPDATE -> {
                long id=Long.parseLong(v.get("id")); affected=Long.toString(id); duplicate="CATEGORY_UPDATE:"+id;
                var entity=categories.findByCategoryId(id);
                if(entity.isEmpty())errors.add("UNKNOWN_CATEGORY");
                else if(entity.get().getVersion()!=Long.parseLong(v.get("expectedVersion")))errors.add("STALE_RESOURCE_VERSION");
                if(categories.existsByNameIgnoreCaseAndDeletedFalseAndIdNot(v.get("name"),id))errors.add("DUPLICATE_CATEGORY");
            }
            case BRANCH_BULK_UPDATE -> {
                long id=Long.parseLong(v.get("id")); affected=Long.toString(id); duplicate="BRANCH_UPDATE:"+id;
                var entity=branches.findById(id);
                if(entity.isEmpty())errors.add("UNKNOWN_BRANCH");
                else if(entity.get().getVersion()!=Long.parseLong(v.get("expectedVersion")))errors.add("STALE_RESOURCE_VERSION");
                if(branches.existsByNameKeyAndIdNot(BranchService.key(v.get("name")),id))errors.add("DUPLICATE_BRANCH");
            }
            case PRODUCT_BULK_UPDATE -> {
                long id=Long.parseLong(v.get("id")); affected=Long.toString(id); duplicate="PRODUCT_UPDATE:"+id;
                var entity=products.findByIdNotDeleted(id);
                if(entity.isEmpty())errors.add("UNKNOWN_PRODUCT");
                else if(entity.get().getVersion()!=Long.parseLong(v.get("expectedVersion")))errors.add("STALE_RESOURCE_VERSION");
                long category=Long.parseLong(v.get("subCategoryId"));
                if(subcategories.findById(category).isEmpty())errors.add("MISSING_CATEGORY");
                if(products.existsImportDuplicateExcept(v.get("name").trim(),category,id))errors.add("DUPLICATE_PRODUCT");
            }
            case BRANCH_BULK_CREATE -> {
                duplicate = "BRANCH:" + BranchService.key(v.get("name"));

                if (branches.existsByNameKey(BranchService.key(v.get("name")))) {
                    errors.add("DUPLICATE_BRANCH");
                }
            }
            case PRODUCT_BULK_CREATE -> {
                duplicate = "PRODUCT:"
                        + v.get("subCategoryId")
                        + ":"
                        + v.get("name").trim().toLowerCase(Locale.ROOT);

                if (subcategories.findById(
                        Long.parseLong(v.get("subCategoryId"))
                ).isEmpty()) {
                    errors.add("MISSING_CATEGORY");
                }

                if (products.existsImportDuplicate(
                        v.get("name").trim(),
                        Long.parseLong(v.get("subCategoryId"))
                )) {
                    errors.add("DUPLICATE_PRODUCT");
                }
            }
            case CATEGORY_BULK_CREATE -> {
                duplicate = "CATEGORY:"
                        + v.get("name").toLowerCase(Locale.ROOT);

                if (categories.existsByNameIgnoreCaseAndDeletedFalse(
                        v.get("name")
                )) {
                    errors.add("DUPLICATE_CATEGORY");
                }
            }
            case PRODUCT_SKU_BULK_CREATE -> {
                var product = products.findByIdNotDeleted(
                        Long.parseLong(v.get("productId"))
                );

                if (product.isEmpty()) {
                    errors.add("UNKNOWN_PRODUCT");
                } else {
                    duplicate = "SKU:"
                            + skuGenerator.generateSku(
                            product.get(),
                            skuRequest(v)
                    );

                    if (skus.existsBySku(duplicate.substring(4))) {
                        errors.add("DUPLICATE_SKU");
                    }
                }

                if (v.containsKey("barcode")
                        && !v.get("barcode").isBlank()
                        && skus.existsByBarcode(v.get("barcode"))) {
                    errors.add("DUPLICATE_BARCODE");
                }

                if (!"0".equals(v.get("quantity"))) {
                    errors.add(
                            "SKU_INITIAL_STOCK_MUST_BE_ZERO: "
                                    + "import stock separately"
                    );
                }
            }
            case INVENTORY_STOCK_IN_BULK, INVENTORY_STOCK_ADJUST_BULK -> {
                affected = v.get("inventoryId");
                duplicate = "INVENTORY:" + affected;
                var item = inventory.findById(Long.parseLong(affected));

                if (item.isEmpty()) {
                    errors.add("UNKNOWN_INVENTORY");
                } else {
                    try {
                        Long stored = item.get().getBranchId();

                        if (v.containsKey("branchId")
                                && !v.get("branchId").isBlank()) {
                            Long requested = Long.valueOf(v.get("branchId"));
                            branchService.scope(requested, validationActor);

                            if (!requested.equals(stored)) {
                                errors.add("BRANCH_INVENTORY_MISMATCH");
                            }
                        }

                        if (stored != null) {
                            branchService.scope(stored, validationActor);
                        }

                        if (errors.isEmpty()
                                && row.tool()
                                == ImportTool.INVENTORY_STOCK_ADJUST_BULK
                                && Long.parseLong(v.get("quantity"))
                                < item.get().getReservedQuantity()) {
                            errors.add("BELOW_RESERVED_STOCK");
                        }
                    } catch (AccessDeniedException denied) {
                        errors.add("BRANCH_ACCESS_DENIED");
                    } catch (ResponseStatusException missing) {
                        errors.add("UNKNOWN_BRANCH");
                    }
                }
            }
            case PROMOTION_BULK_CREATE -> {
                affected = v.get("skuId");
                duplicate = "PROMOTION:"
                        + v.get("name").toLowerCase(Locale.ROOT)
                        + ":"
                        + affected;

                if (skus.findById(Long.parseLong(affected)).isEmpty()) {
                    errors.add("UNKNOWN_SKU");
                }

                if (promotionRepository.existsImportDuplicate(
                        v.get("name"),
                        Long.parseLong(affected)
                )) {
                    errors.add("DUPLICATE_PROMOTION");
                }
            }
            default -> errors.add("UNSUPPORTED_IMPORT");
        }

        return new Validation(errors, duplicate, affected);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result execute(String key, String approval, Row row) throws Exception {
        return execute(key, approval, row, List.of());
    }

    @Transactional(rollbackFor = Exception.class)
    public Result execute(
            String key,
            String approval,
            Row row,
            List<org.springframework.web.multipart.MultipartFile> files
    ) throws Exception {
        authorize(row, true);
        proof.verify(approval, key, row, CurrentActor.userId());

        if (key == null || !key.matches("[a-f0-9-]{36}:[a-f0-9-]{36}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid idempotency key"
            );
        }

        String hash = HexFormat.of().formatHex(
                java.security.MessageDigest
                        .getInstance("SHA-256")
                        .digest(
                                mapper.writeValueAsBytes(
                                        List.of(
                                                row.tool().name(),
                                                new TreeMap<>(row.values())
                                        )
                                )
                        )
        );

        // PostgreSQL ON CONFLICT waits for a concurrent receipt transaction, then the lock reads its result.
        jdbc.update(
                "insert into ai_import_receipt(id,actor_id,checksum,created_at) "
                        + "values (?,?,?,CURRENT_TIMESTAMP) on conflict (id) do nothing",
                key,
                CurrentActor.userId(),
                hash
        );

        ImportReceipt receipt = em.find(
                ImportReceipt.class,
                key,
                jakarta.persistence.LockModeType.PESSIMISTIC_WRITE
        );

        if (!receipt.getChecksum().equals(hash)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency payload mismatch"
            );
        }

        if (receipt.getResourceId() != null) {
            return new Result(receipt.getResourceId());
        }

        if (!row.tool().validate(row.values()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Invalid row schema"
            );
        }

        // Serialize imports of the same natural key across batches before authoritative validation.
        List<String> keys = new ArrayList<>();
        Map<String, String> data = row.values();

        switch (row.tool()) {
            case CATEGORY_BULK_UPDATE -> { keys.add("CATEGORY_ID:"+data.get("id")); keys.add("CATEGORY:"+data.get("name").toLowerCase(Locale.ROOT)); }
            case BRANCH_BULK_UPDATE -> { keys.add("BRANCH_ID:"+data.get("id")); keys.add("BRANCH:"+BranchService.key(data.get("name"))); }
            case PRODUCT_BULK_UPDATE -> { keys.add("PRODUCT_ID:"+data.get("id")); keys.add("PRODUCT:"+data.get("subCategoryId")+":"+data.get("name").trim().toLowerCase(Locale.ROOT)); }
            case BRANCH_BULK_CREATE ->
                    keys.add("BRANCH:" + BranchService.key(data.get("name")));
            case PRODUCT_BULK_CREATE ->
                    keys.add(
                            "PRODUCT:"
                                    + data.get("subCategoryId")
                                    + ":"
                                    + data.get("name")
                                    .trim()
                                    .toLowerCase(Locale.ROOT)
                    );
            case CATEGORY_BULK_CREATE ->
                    keys.add(
                            "CATEGORY:"
                                    + data.get("name").toLowerCase(Locale.ROOT)
                    );
            case PRODUCT_SKU_BULK_CREATE -> {
                var product = products.findByIdNotDeleted(
                        Long.parseLong(data.get("productId"))
                ).orElseThrow();
                keys.add("SKU:" + skuGenerator.generateSku(
                        product,
                        skuRequest(data)
                ));

                if (data.get("barcode") != null
                        && !data.get("barcode").isBlank()) {
                    keys.add("BARCODE:" + data.get("barcode"));
                }
            }
            case PROMOTION_BULK_CREATE ->
                    keys.add(
                            "PROMOTION:"
                                    + data.get("name").toLowerCase(Locale.ROOT)
                                    + ":"
                                    + data.get("skuId")
                    );
            default -> {
            }
        }

        keys.stream()
                .sorted()
                .forEach(naturalKey -> jdbc.queryForObject(
                        "select pg_advisory_xact_lock(hashtextextended(?, 0))",
                        Object.class,
                        naturalKey
                ));

        Validation validation = check(row);

        if (!validation.errors().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    String.join(", ", validation.errors())
            );
        }

        Map<String, String> v = row.values();
        String id;

        switch (row.tool()) {
            case CATEGORY_BULK_UPDATE -> {
                id=v.get("id");
                categoryService.updateCategory(Long.valueOf(id),CategoryRequest.builder().name(v.get("name")).description(v.get("description")).status(Boolean.valueOf(v.get("status"))).build());
            }
            case BRANCH_BULK_UPDATE -> id=branchService.update(Long.parseLong(v.get("id")),Long.parseLong(v.get("expectedVersion")),v.get("name"),v.get("address")).getId().toString();
            case PRODUCT_BULK_UPDATE -> {
                id=v.get("id");
                productService.updateProduct(Long.valueOf(id),ProductRequest.builder().name(v.get("name").trim()).description(v.get("description"))
                        .subCategoryId(Long.valueOf(v.get("subCategoryId"))).isActive(Boolean.valueOf(v.get("isActive"))).build(),List.of(),List.of());
            }
            case BRANCH_BULK_CREATE ->
                    id = branchService
                            .create(v.get("name"), v.get("address"))
                            .getId()
                            .toString();
            case PRODUCT_BULK_CREATE -> {
                List<org.springframework.web.multipart.MultipartFile> approvedFiles =
                        verifiedImages(row, files);
                ProductRequest request = ProductRequest.builder()
                        .name(v.get("name").trim())
                        .description(v.get("description"))
                        .subCategoryId(Long.valueOf(v.get("subCategoryId")))
                        .isActive(Boolean.valueOf(v.get("isActive")))
                        .build();
                id = mapper.valueToTree(
                                productService.createProduct(
                                        request,
                                        approvedFiles,
                                        List.of()
                                ).object()
                        )
                        .path("id")
                        .asText();
            }
            case CATEGORY_BULK_CREATE -> {
                var request = CategoryRequest.builder()
                        .name(v.get("name"))
                        .description(v.get("description"))
                        .status(Boolean.valueOf(v.get("status")))
                        .build();
                id = mapper.valueToTree(
                                categoryService.createCategory(request).object()
                        )
                        .path("id")
                        .asText();
            }
            case PRODUCT_SKU_BULK_CREATE ->
                    id = skuService.createSku(
                                    Long.valueOf(v.get("productId")),
                                    skuRequest(v),
                                    null
                            )
                            .getId()
                            .toString();
            case INVENTORY_STOCK_IN_BULK, INVENTORY_STOCK_ADJUST_BULK -> {
                Long inventoryId = Long.valueOf(v.get("inventoryId"));
                var item = inventory.lockById(inventoryId).orElseThrow();

                if (item.getBranchId() != null) {
                    branchService.scope(item.getBranchId());
                }

                long quantity = Long.parseLong(v.get("quantity"));

                if (row.tool() == ImportTool.INVENTORY_STOCK_IN_BULK) {
                    Math.addExact(item.getQuantity(), quantity);
                    inventoryService.restock(
                            inventoryId,
                            new RestockRequest(quantity)
                    );
                } else {
                    InventoryRequest request = new InventoryRequest();
                    request.setQuantity(quantity);
                    inventoryService.adjustQuantity(inventoryId, request);
                }

                id = inventoryId.toString();
            }
            case PROMOTION_BULK_CREATE ->
                    id = mapper.valueToTree(
                                    promotions.create(
                                            new com.example.eshop.catalog.controller.AiCatalogController.DraftPromotion(
                                                    Long.parseLong(v.get("skuId")),
                                                    v.get("name"),
                                                    new BigDecimal(v.get("discount")),
                                                    LocalDateTime.parse(v.get("startAt")),
                                                    LocalDateTime.parse(v.get("endAt"))
                                            )
                                    )
                            )
                            .path("id")
                            .asText();
            default -> throw new IllegalArgumentException(
                    "Unsupported import"
            );
        }

        if (id == null || !id.matches("[1-9][0-9]*")) {
            throw new IllegalStateException(
                    "Domain did not return a resource ID"
            );
        }

        receipt.setResourceId(id);
        em.flush();
        return new Result(id);
    }

    private List<org.springframework.web.multipart.MultipartFile> verifiedImages(
            Row row,
            List<org.springframework.web.multipart.MultipartFile> files
    ) throws Exception {
        String[] hashes = row.values()
                .getOrDefault("imageChecksums", "")
                .split(",");

        if (files == null
                || files.isEmpty()
                || files.size() > 4
                || files.size() != hashes.length) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approved image attachments required"
            );
        }

        List<org.springframework.web.multipart.MultipartFile> clean =
                new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            var file = files.get(i);

            if (file.getSize() > 5_242_880) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Image too large"
                );
            }

            var approved = new com.example.eshop.common.imports.ImportImageFile(
                    "product.png",
                    file.getBytes()
            );

            if (!approved.checksum().equals(hashes[i])) {
                throw new AccessDeniedException(
                        "Image differs from approved revision"
                );
            }

            var sanitized = com.example.eshop.common.imports.ImportImageFile
                    .sanitize(approved);
            String imageId = row.values().get("imageIds").split(",")[i];
            clean.add(
                    new com.example.eshop.common.imports.ImportImageFile(
                            "import-" + imageId + "-" + hashes[i] + ".png",
                            sanitized.getBytes()
                    )
            );
        }

        return clean;
    }

    private ProductSkuRequest skuRequest(Map<String, String> v) {
        ProductSkuRequest request = new ProductSkuRequest();
        request.setDescription(v.get("description"));
        request.setBarcode(v.get("barcode"));
        request.setPrice(new BigDecimal(v.get("price")));
        request.setQuantity(0L);
        return request;
    }
}
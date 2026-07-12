package com.example.learning_spring_security.utils;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupRepository groupRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final ApiPermissionRepository apiPermissionRepository;
    private final GroupPermissionRepository groupPermissionRepository;
    private final UserGroupRepository userGroupRepository;

    @Override
    public void run(String... args) {
        // 1. Create roles
        List<String> roles = List.of("ADMIN", "MANAGER", "STAFF", "SALES", "USER");
        roles.forEach(this::createRoleIfNotFound);

        // 2. Seed groups
        seedGroups();

        // 3. Seed function permissions
        seedFunctionPermissions();

        // 4. Assign all permissions to ADMIN group
        seedAdminGroupPermissions();

        // 4.5 Seed SAL group permissions (view only)
        seedSalesGroupPermissions();

        // 4.6 Seed USR group permissions (create + view)  <-- NEW
        seedUserGroupPermissions();

        // 5. Create admin user
        createAdminIfNotFound();

        // 6. Assign admin to group
        userRepository.findByUsername("admin")
                .ifPresent(this::assignAdminToGroup);

        // 7. Seed API permissions
        seedApiPermissions();
    }

    // ---------- Existing methods ----------
    private void createRoleIfNotFound(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = Role.builder().name(roleName).build();
            roleRepository.save(role);
        }
    }

    private void seedFunctionPermissions() {
        List<FunctionPermission> functions = List.of(
                // ── USER GROUP ──────────────────────────────────────────
                buildFunction(101L, "USER_GROUP_VIEW",   "View User Group",   "View user group list and detail",        "USER_GROUP"),
                buildFunction(102L, "USER_GROUP_CREATE", "Create User Group", "Create a new user group",                "USER_GROUP"),
                buildFunction(103L, "USER_GROUP_UPDATE", "Update User Group", "Update existing user group",             "USER_GROUP"),
                buildFunction(104L, "USER_GROUP_DELETE", "Delete User Group", "Soft delete a user group",               "USER_GROUP"),
                // ── USER PERMISSION ─────────────────────────────────────
                buildFunction(201L, "USER_PERM_CREATE",  "Create User Permission",  "Grant a function to a user",        "USER_PERMISSION"),
                buildFunction(202L, "USER_PERM_UPDATE",  "Update User Permission",  "Enable or disable user permission", "USER_PERMISSION"),
                buildFunction(203L, "USER_PERM_DELETE",  "Delete User Permission",  "Remove user permission record",     "USER_PERMISSION"),
                // ── GROUP PERMISSION ────────────────────────────────────
                buildFunction(301L, "GRP_PERM_CREATE",   "Create Group Permission", "Assign a function to a group",        "GROUP_PERMISSION"),
                buildFunction(302L, "GRP_PERM_UPDATE",   "Update Group Permission", "Enable or disable group permission",  "GROUP_PERMISSION"),
                buildFunction(303L, "GRP_PERM_DELETE",   "Delete Group Permission", "Remove group permission record",      "GROUP_PERMISSION"),
                // ── FUNCTION PERMISSION ─────────────────────────────────
                buildFunction(401L, "FUNC_CREATE",       "Create Function", "Register a new function",    "FUNCTION"),
                buildFunction(402L, "FUNC_UPDATE",       "Update Function", "Edit or disable a function", "FUNCTION"),
                buildFunction(403L, "FUNC_DELETE",       "Delete Function", "Remove a function record",   "FUNCTION"),
                // ── CART ────────────────────────────────────────────────
                buildFunction(501L, "CART_ADD_ITEM",    "Add item to cart",    "Add a product to user's cart", "CART"),
                buildFunction(502L, "CART_VIEW",       "View cart",           "View user's cart items",       "CART"),
                buildFunction(503L, "CART_UPDATE_ITEM","Update cart item",    "Update quantity of cart item", "CART"),
                buildFunction(504L, "CART_REMOVE_ITEM","Remove cart item",    "Remove item from cart",        "CART"),
                buildFunction(505L, "CART_CLEAR",      "Clear cart",          "Clear all items from cart",    "CART"),
                // ── USER MANAGEMENT ────────────────────────────────────
                buildFunction(601L, "USER_MANAGE",      "Manage Users",        "Create/update/delete users",   "USER_MANAGEMENT"),
                // ── ORDER ──────────────────────────────────────────────
                buildFunction(701L, "ORDER_CREATE", "Create Order", "Create order from cart", "ORDER"),
                buildFunction(702L, "ORDER_VIEW",   "View Order",   "View order details",      "ORDER"),
                // ── BAKONG ──────────────────────────────────────────────
                buildFunction(801L, "BAKONG_QR", "Get Bakong QR", "Generate Bakong payment QR code", "PAYMENT"),
                // ── ADDRESS ─────────────────────────────────────────────
                buildFunction(901L, "ADDRESS_CREATE", "Create Address", "Add a new address for user", "ADDRESS"),
                buildFunction(902L, "ADDRESS_VIEW",   "View Address",   "View user addresses",          "ADDRESS"),
                buildFunction(903L, "ADDRESS_UPDATE", "Update Address", "Update existing address",      "ADDRESS"),
                buildFunction(904L, "ADDRESS_DELETE", "Delete Address", "Delete user address",          "ADDRESS"),
                // ── PRODUCT & CATEGORY (Admin/Manager only) ────────────────
                buildFunction(1001L, "CATEGORY_CREATE", "Create Category", "Create product category", "PRODUCT"),
                buildFunction(1002L, "SUBCATEGORY_CREATE", "Create Subcategory", "Create product subcategory", "PRODUCT"),
                buildFunction(1003L, "PRODUCT_CREATE", "Create Product", "Create new product", "PRODUCT"),
                buildFunction(1004L, "PRODUCT_VIEW", "View Products", "View product list", "PRODUCT"),
                buildFunction(1005L, "PRODUCT_UPDATE", "Update Product", "Edit product details", "PRODUCT"),
                buildFunction(1006L, "PRODUCT_DELETE", "Delete Product", "Remove product", "PRODUCT"),
                // ── VIEW ONLY (for SAL users) ──────────────────────────────
                buildFunction(1007L, "CATEGORY_VIEW", "View Categories", "View category list", "PRODUCT"),
                buildFunction(1008L, "SUBCATEGORY_VIEW", "View Subcategories", "View subcategory list", "PRODUCT")
        );

        for (FunctionPermission fn : functions) {
            if (!functionPermissionRepository.existsByFuncCode(fn.getFuncCode())) {
                functionPermissionRepository.save(fn);
                System.out.println("Function seeded: [" + fn.getFuncId() + "] " + fn.getFuncCode());
            }
        }
    }

    private void seedGroups() {
        List<Group> groups = List.of(
                buildGroup("ADM", "Administrator", "System administrator group"),
                buildGroup("MNG", "Manager", "Manager group"),
                buildGroup("STF", "Staff", "Staff group"),
                buildGroup("SAL", "Sales", "Sales group"),
                buildGroup("USR", "Default User Group", "Default group for regular users")
        );
        groups.forEach(group -> {
            if (!groupRepository.existsByGroupCode(group.getGroupCode())) {
                groupRepository.save(group);
                System.out.println("Group seeded: " + group.getGroupCode());
            }
        });
    }

    private Group buildGroup(String groupCode, String name, String description) {
        Group group = Group.builder()
                .groupCode(groupCode)
                .name(name)
                .description(description)
                .status(Constant.ACT)
                .isActive(true)
                .isDelete(false)
                .build();
        group.setCreatedAt(LocalDateTime.now());
        return group;
    }

    private FunctionPermission buildFunction(Long funcId, String funcCode, String funcName,
                                             String description, String module) {
        FunctionPermission fn = FunctionPermission.builder()
                .funcId(funcId)
                .funcCode(funcCode)
                .funcName(funcName)
                .description(description)
                .module(module)
                .isActive(true)
                .build();
        fn.setCreatedAt(LocalDateTime.now());
        return fn;
    }

    private void createAdminIfNotFound() {
        if (!userRepository.existsByUsernameAndDeletedFalse("admin")) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Admin role not found"));
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            User admin = User.builder()
                    .username("admin")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .status(Constant.ACT)
                    .enabled(true)
                    .phone("097328636")
                    .roles(roles)
                    .attempt(0)
                    .deleted(false)
                    .build();
            admin.setCreatedAt(LocalDateTime.now());
            User savedAdmin = userRepository.save(admin);
            System.out.println("Admin user created with ID: " + savedAdmin.getId() + ", email: " + savedAdmin.getEmail());
        } else {
            System.out.println("Admin user already exists");
        }
    }

    private void seedAdminGroupPermissions() {
        Group adminGroup = groupRepository.findByGroupCode("ADM")
                .orElseThrow(() -> new RuntimeException("Administrator group not found"));
        List<FunctionPermission> functions = functionPermissionRepository.findAll();
        for (FunctionPermission function : functions) {
            boolean exists = groupPermissionRepository.existsByGroupIdAndFuncId(
                    adminGroup.getId(), function.getFuncId());
            if (!exists) {
                GroupPermission permission = GroupPermission.builder()
                        .groupId(adminGroup.getId())
                        .funcId(function.getFuncId())
                        .isActive(true)
                        .isDelete(false)
                        .build();
                permission.setCreatedAt(LocalDateTime.now());
                groupPermissionRepository.save(permission);
                System.out.println("Assigned " + function.getFuncCode() + " -> ADM");
            }
        }
    }

    private void seedSalesGroupPermissions() {
        Group salesGroup = groupRepository.findByGroupCode("SAL")
                .orElseThrow(() -> new RuntimeException("Sales group not found"));

        // Only view permissions – no create/update/delete for products/categories/subcategories
        List<Long> funcIds = List.of(
                501L, 502L,          // Cart (add & view)
                701L,                 // Order create
                801L,                 // Bakong QR
                901L, 902L, 903L, 904L, // Address (all operations)
                1004L,                // Product VIEW
                1007L,                // Category VIEW
                1008L                 // Subcategory VIEW
        );
        for (Long funcId : funcIds) {
            if (!groupPermissionRepository.existsByGroupIdAndFuncId(salesGroup.getId(), funcId)) {
                GroupPermission gp = GroupPermission.builder()
                        .groupId(salesGroup.getId())
                        .funcId(funcId)
                        .isActive(true)
                        .isDelete(false)
                        .build();
                gp.setCreatedAt(LocalDateTime.now());
                groupPermissionRepository.save(gp);
                System.out.println("Assigned funcId=" + funcId + " -> SAL group");
            }
        }
    }

    // ---------- NEW: Seed USR group permissions ----------
    private void seedUserGroupPermissions() {
        Group userGroup = groupRepository.findByGroupCode("USR")
                .orElseThrow(() -> new RuntimeException("USR group not found"));

        // Grant create and view permissions for categories, subcategories, products
        List<Long> funcIds = List.of(
                1001L,  // CATEGORY_CREATE
                1002L,  // SUBCATEGORY_CREATE
                1003L,  // PRODUCT_CREATE
                1004L,  // PRODUCT_VIEW
                1007L,  // CATEGORY_VIEW
                1008L   // SUBCATEGORY_VIEW
        );

        for (Long funcId : funcIds) {
            if (!groupPermissionRepository.existsByGroupIdAndFuncId(userGroup.getId(), funcId)) {
                GroupPermission gp = GroupPermission.builder()
                        .groupId(userGroup.getId())
                        .funcId(funcId)
                        .isActive(true)
                        .isDelete(false)
                        .build();
                gp.setCreatedAt(LocalDateTime.now());
                groupPermissionRepository.save(gp);
                System.out.println("Assigned funcId=" + funcId + " -> USR group");
            }
        }
    }

    private void assignAdminToGroup(User admin) {
        Group adminGroup = groupRepository.findByGroupCode("ADM")
                .orElseThrow(() -> new RuntimeException("Administrator group not found"));
        if (!userGroupRepository.existsByUserIdAndGroupId(admin.getId(), adminGroup.getId())) {
            UserGroup userGroup = UserGroup.builder()
                    .userId(admin.getId())
                    .groupId(adminGroup.getId())
                    .isActive(true)
                    .isDelete(false)
                    .build();
            userGroup.setCreatedAt(LocalDateTime.now());
            userGroupRepository.save(userGroup);
            System.out.println("Admin assigned to ADM group.");
        }
    }

    // ---------- Seed API permissions ----------
    private void seedApiPermissions() {
        // ── CART ──────────────────────────────────────────────────────────
        String cartAddPattern = "/api/v1/cart/user/**/items";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", cartAddPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(cartAddPattern)
                    .funcId(501L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + cartAddPattern + " -> funcId=501");
        }

        String cartViewPattern = "/api/v1/cart/user/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", cartViewPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(cartViewPattern)
                    .funcId(502L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + cartViewPattern + " -> funcId=502");
        }

        // ── ADMIN USER MANAGEMENT ──────────────────────────────────────
        String adminCreatePattern = "/api/v1/admin/users";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", adminCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(adminCreatePattern)
                    .funcId(601L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + adminCreatePattern + " -> funcId=601");
        }

        // ── ORDER ────────────────────────────────────────────────────────
        String orderCreatePattern = "/api/v1/orders/user/from-cart/bakong";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", orderCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(orderCreatePattern)
                    .funcId(701L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + orderCreatePattern + " -> funcId=701");
        }

        String orderViewPattern = "/api/v1/orders/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", orderViewPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(orderViewPattern)
                    .funcId(702L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + orderViewPattern + " -> funcId=702");
        }

        // ── BAKONG ──────────────────────────────────────────────────────
        String bakongPattern = "/api/v1/bakong/get-qr-image";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", bakongPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(bakongPattern)
                    .funcId(801L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + bakongPattern + " -> funcId=801");
        }

        // ── ADDRESS ──────────────────────────────────────────────────────
        String addressCreatePattern = "/api/v1/addresses/user/**";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", addressCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(addressCreatePattern)
                    .funcId(901L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + addressCreatePattern + " -> funcId=901");
        }

        String addressViewPattern = "/api/v1/addresses/user/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", addressViewPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(addressViewPattern)
                    .funcId(902L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + addressViewPattern + " -> funcId=902");
        }

        String addressUpdatePattern = "/api/v1/addresses/**";
        if (!apiPermissionRepository.existsByMethodAndApi("PUT", addressUpdatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("PUT")
                    .api(addressUpdatePattern)
                    .funcId(903L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: PUT " + addressUpdatePattern + " -> funcId=903");
        }

        String addressDeletePattern = "/api/v1/addresses/**";
        if (!apiPermissionRepository.existsByMethodAndApi("DELETE", addressDeletePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("DELETE")
                    .api(addressDeletePattern)
                    .funcId(904L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: DELETE " + addressDeletePattern + " -> funcId=904");
        }

        // ── PRODUCT / CATEGORY / SUBCATEGORY ────────────────────────────
        // Create (POST) – assigned to ADM, SAL, and USR groups
        String categoryCreatePattern = "/api/v1/categories/**";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", categoryCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(categoryCreatePattern)
                    .funcId(1001L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + categoryCreatePattern + " -> funcId=1001");
        }

        String subcategoryCreatePattern = "/api/v1/subcategories/**";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", subcategoryCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(subcategoryCreatePattern)
                    .funcId(1002L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + subcategoryCreatePattern + " -> funcId=1002");
        }

        String productCreatePattern = "/api/v1/products/**";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", productCreatePattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(productCreatePattern)
                    .funcId(1003L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + productCreatePattern + " -> funcId=1003");
        }

        // ── VIEW ENDPOINTS (GET + POST queries) ──────────────────────────
        // Product View (funcId=1004)
        String productGetPattern = "/api/v1/products/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", productGetPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(productGetPattern)
                    .funcId(1004L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + productGetPattern + " -> funcId=1004");
        }
        // POST /api/v1/products/get/all
        String productListPattern = "/api/v1/products/get/all";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", productListPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(productListPattern)
                    .funcId(1004L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + productListPattern + " -> funcId=1004");
        }

        // Category View (funcId=1007)
        String categoryGetPattern = "/api/v1/categories/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", categoryGetPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(categoryGetPattern)
                    .funcId(1007L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + categoryGetPattern + " -> funcId=1007");
        }
        // POST /api/v1/categories/get/all
        String categoryListPattern = "/api/v1/categories/get/all";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", categoryListPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(categoryListPattern)
                    .funcId(1007L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + categoryListPattern + " -> funcId=1007");
        }
        // POST /api/v1/categories/id/get/
        String categoryByIdPattern = "/api/v1/categories/id/get/";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", categoryByIdPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(categoryByIdPattern)
                    .funcId(1007L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + categoryByIdPattern + " -> funcId=1007");
        }

        // Subcategory View (funcId=1008)
        String subcategoryGetPattern = "/api/v1/subcategories/**";
        if (!apiPermissionRepository.existsByMethodAndApi("GET", subcategoryGetPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("GET")
                    .api(subcategoryGetPattern)
                    .funcId(1008L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: GET " + subcategoryGetPattern + " -> funcId=1008");
        }
        // POST /api/v1/subcategories/get/all
        String subcategoryListPattern = "/api/v1/subcategories/get/all";
        if (!apiPermissionRepository.existsByMethodAndApi("POST", subcategoryListPattern)) {
            ApiPermission p = ApiPermission.builder()
                    .method("POST")
                    .api(subcategoryListPattern)
                    .funcId(1008L)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            System.out.println(" Seeded API Permission: POST " + subcategoryListPattern + " -> funcId=1008");
        }
    }
}
package com.example.learning_spring_security.utils;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.*;
import com.example.learning_spring_security.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupRepository groupRepository;
    private final FunctionPermissionRepository functionPermissionRepository;
    private final ApiPermissionRepository apiPermissionRepository;
    private final GroupPermissionRepository groupPermissionRepository;
    private final UserGroupRepository userGroupRepository;
    private final CategoryIconRepository categoryIconRepository;

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

        // 4.7 Seed STF/MNG group permissions (user-group read only)
        seedStaffUserGroupViewPermissions();

        // 4.8 Seed STF/MNG group permissions for Return Management
        seedReturnGroupPermissions();

        // 4.9 Seed STF/MNG group permissions for Refund Management
        seedRefundGroupPermissions();

        // 4.10 Seed STF/MNG group permissions for Cancelation Management
        seedCancelationGroupPermissions();

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
                buildFunction(204L, "USER_PERM_VIEW",    "View User Permissions",   "View user permission list and detail", "USER_PERMISSION"),
                // ── GROUP PERMISSION ────────────────────────────────────
                buildFunction(301L, "GRP_PERM_CREATE",   "Create Group Permission", "Assign a function to a group",        "GROUP_PERMISSION"),
                buildFunction(302L, "GRP_PERM_UPDATE",   "Update Group Permission", "Enable or disable group permission",  "GROUP_PERMISSION"),
                buildFunction(303L, "GRP_PERM_DELETE",   "Delete Group Permission", "Remove group permission record",      "GROUP_PERMISSION"),
                buildFunction(304L, "GRP_PERM_VIEW",     "View Group Permissions",  "View group permission list and detail", "GROUP_PERMISSION"),
                // ── FUNCTION PERMISSION ─────────────────────────────────
                buildFunction(401L, "FUNC_CREATE",       "Create Function", "Register a new function",    "FUNCTION"),
                buildFunction(402L, "FUNC_UPDATE",       "Update Function", "Edit or disable a function", "FUNCTION"),
                buildFunction(403L, "FUNC_DELETE",       "Delete Function", "Remove a function record",   "FUNCTION"),
                buildFunction(404L, "FUNC_VIEW",         "View Functions",   "View function list and detail", "FUNCTION"),
                // ── CART ────────────────────────────────────────────────
                buildFunction(501L, "CART_ADD_ITEM",    "Add item to cart",    "Add a product to user's cart", "CART"),
                buildFunction(502L, "CART_VIEW",       "View cart",           "View user's cart items",       "CART"),
                buildFunction(503L, "CART_UPDATE_ITEM","Update cart item",    "Update quantity of cart item", "CART"),
                buildFunction(504L, "CART_REMOVE_ITEM","Remove cart item",    "Remove item from cart",        "CART"),
                buildFunction(505L, "CART_CLEAR",      "Clear cart",          "Clear all items from cart",    "CART"),
                // ── USER MANAGEMENT ────────────────────────────────────
                buildFunction(601L, "USER_MANAGE",      "Manage Users",        "Create/update/delete users",   "USER_MANAGEMENT"),
                // ── ORDER ──────────────────────────────────────────────
                buildFunction(701L, "ORDER_CREATE",   "Create Order",     "Create order from cart, cancel own order",      "ORDER"),
                buildFunction(702L, "ORDER_VIEW",     "View Order",       "View own order details",                        "ORDER"),
                buildFunction(703L, "ORDER_MANAGE",   "Manage Order",     "Update order status (admin/staff)",             "ORDER"),
                buildFunction(706L, "ORDER_VIEW_ALL", "View All Orders",  "View/list orders across all customers (admin)", "ORDER"),
                // ── BAKONG ──────────────────────────────────────────────
                buildFunction(801L, "BAKONG_QR", "Get Bakong QR", "Generate Bakong payment QR code", "PAYMENT"),
                // ── ADDRESS ─────────────────────────────────────────────
                buildFunction(901L, "ADDRESS_CREATE", "Create Address", "Add a new address for user", "ADDRESS"),
                buildFunction(902L, "ADDRESS_VIEW",   "View Address",   "View user addresses",          "ADDRESS"),
                buildFunction(903L, "ADDRESS_UPDATE", "Update Address", "Update existing address",      "ADDRESS"),
                buildFunction(904L, "ADDRESS_DELETE", "Delete Address", "Delete user address",          "ADDRESS"),
                // ── PAYMENT ─────────────────────────────────────────────
                buildFunction(705L, "PAYMENT_VIEW",     "View Payments",     "View own payment detail/history",       "PAYMENT"),
                buildFunction(707L, "PAYMENT_VIEW_ALL", "View All Payments", "View/list payments across all customers (admin)", "PAYMENT"),
                buildFunction(1301L, "PAYMENT_TXN_VIEW",   "View Payment Transactions",   "View payment gateway transaction records", "PAYMENT"),
                buildFunction(1302L, "PAYMENT_TXN_MANAGE", "Manage Payment Transactions", "Create/update payment gateway transactions", "PAYMENT"),
                // ── CANCELATION ─────────────────────────────────────────
                buildFunction(1401L, "CANCELATION_VIEW", "View Cancelations", "View order cancelation summary, list and detail", "CANCELATION"),
                // ── PRODUCT & CATEGORY (Admin/Manager only) ────────────────
                buildFunction(1001L, "CATEGORY_CREATE", "Create Category", "Create product category", "PRODUCT"),
                buildFunction(1002L, "SUBCATEGORY_CREATE", "Create Subcategory", "Create product subcategory", "PRODUCT"),
                buildFunction(1003L, "PRODUCT_CREATE", "Create Product", "Create new product", "PRODUCT"),
                buildFunction(1004L, "PRODUCT_VIEW", "View Products", "View product list", "PRODUCT"),
                buildFunction(1005L, "PRODUCT_UPDATE", "Update Product", "Edit product details", "PRODUCT"),
                buildFunction(1006L, "PRODUCT_DELETE", "Delete Product", "Remove product", "PRODUCT"),
                // ── VIEW ONLY (for SAL users) ──────────────────────────────
                buildFunction(1007L, "CATEGORY_VIEW", "View Categories", "View category list", "PRODUCT"),
                buildFunction(1008L, "SUBCATEGORY_VIEW", "View Subcategories", "View subcategory list", "PRODUCT"),
                // ── RETURN MANAGEMENT ───────────────────────────────────
                buildFunction(1101L, "RETURN_VIEW",    "View Returns",    "View return summary, list and detail", "RETURN"),
                buildFunction(1102L, "RETURN_APPROVE", "Approve Return",  "Approve a return request",             "RETURN"),
                buildFunction(1103L, "RETURN_REJECT",  "Reject Return",  "Reject a return request",              "RETURN"),
                buildFunction(1104L, "RETURN_REFUND",  "Refund Return",  "Process refund for a return",          "RETURN"),
                buildFunction(1105L, "RETURN_EXPORT",  "Export Returns", "Export return records",                "RETURN"),
                buildFunction(1106L, "RETURN_RECEIVE", "Receive Return", "Confirm warehouse receipt of a return", "RETURN"),
                buildFunction(1107L, "RETURN_INSPECT", "Inspect Return", "Complete warehouse inspection of a return", "RETURN"),
                // ── REFUND MANAGEMENT ───────────────────────────────────
                buildFunction(1201L, "REFUND_VIEW",    "View Refunds",   "View refund summary, list and detail", "REFUND"),
                buildFunction(1202L, "REFUND_PROCESS", "Process Refund", "Process a pending refund",             "REFUND"),
                buildFunction(1203L, "REFUND_CANCEL",  "Cancel Refund",  "Cancel a refund request",              "REFUND"),
                buildFunction(1204L, "REFUND_EXPORT",  "Export Refunds", "Export refund records",                "REFUND")
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
                501L, 502L, 503L, 504L, 505L, // Cart (add, view, update, remove, clear)
                701L, 702L,           // Order create/cancel, view own orders
                705L,                 // Payment view
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

        // Grant view permissions for categories, subcategories, products, plus the
        // standard customer self-service functions (cart, checkout, own orders/payments, addresses)
        List<Long> funcIds = List.of(
                1004L,  // PRODUCT_VIEW
                1007L,  // CATEGORY_VIEW
                1008L,  // SUBCATEGORY_VIEW
                501L, 502L, 503L, 504L, 505L, // Cart (add, view, update, remove, clear)
                701L, 702L,           // Order create/cancel, view own orders
                705L,                 // Payment view
                801L,                 // Bakong QR
                901L, 902L, 903L, 904L,601L // Address (all operations)
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

    // ---------- NEW: Grant read-only user-group access to staff/manager groups ----------
    private void seedStaffUserGroupViewPermissions() {
        grantFuncIdsToGroup("STF", List.of(101L)); // USER_GROUP_VIEW
        grantFuncIdsToGroup("MNG", List.of(101L)); // USER_GROUP_VIEW
    }

    // ---------- NEW: Return Management permissions (STF view-only, MNG view+approve+reject) ----------
    private void seedReturnGroupPermissions() {
        grantFuncIdsToGroup("STF", List.of(1101L));
        grantFuncIdsToGroup("MNG", List.of(1101L, 1102L, 1103L, 1106L, 1107L));
    }

    // ---------- NEW: Refund Management permissions (STF view-only, MNG view+process) ----------
    private void seedRefundGroupPermissions() {
        grantFuncIdsToGroup("STF", List.of(1201L));
        grantFuncIdsToGroup("MNG", List.of(1201L, 1202L, 1203L));
    }

    // ---------- NEW: Cancelation Management permissions (STF/MNG view-only, reporting) ----------
    private void seedCancelationGroupPermissions() {
        grantFuncIdsToGroup("STF", List.of(1401L));
        grantFuncIdsToGroup("MNG", List.of(1401L));
    }

    private void grantFuncIdsToGroup(String groupCode, List<Long> funcIds) {
        Group group = groupRepository.findByGroupCode(groupCode)
                .orElseThrow(() -> new RuntimeException(groupCode + " group not found"));
        for (Long funcId : funcIds) {
            if (!groupPermissionRepository.existsByGroupIdAndFuncId(group.getId(), funcId)) {
                GroupPermission gp = GroupPermission.builder()
                        .groupId(group.getId())
                        .funcId(funcId)
                        .isActive(true)
                        .isDelete(false)
                        .build();
                gp.setCreatedAt(LocalDateTime.now());
                groupPermissionRepository.save(gp);
                System.out.println("Assigned funcId=" + funcId + " -> " + groupCode + " group");
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

    // ---------- Modified seedApiPermissions with helper ----------
    private void seedApiPermissions() {
        // Use helper to reduce duplication
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/**/items", 501L);
        createApiPermissionIfNotExists("GET", "/api/v1/cart/user/**", 502L);
        createApiPermissionIfNotExists("POST", "/api/v1/admin/users", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/from-cart/bakong", 701L);
        createApiPermissionIfNotExists("GET", "/api/v1/orders/**", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/bakong/get-qr-image", 801L);
        createApiPermissionIfNotExists("POST", "/api/v1/addresses/user/**", 901L);
        createApiPermissionIfNotExists("GET", "/api/v1/addresses/user/**", 902L);
        createApiPermissionIfNotExists("PUT", "/api/v1/addresses/**", 903L);
        createApiPermissionIfNotExists("DELETE", "/api/v1/addresses/**", 904L);
        createApiPermissionIfNotExists("POST", "/api/v1/categories/**", 1001L);
        createApiPermissionIfNotExists("PUT", "/api/v1/categories/**", 1001L);
        createApiPermissionIfNotExists("DELETE", "/api/v1/categories/**", 1001L);
        createApiPermissionIfNotExists("POST", "/api/v1/subcategories/**", 1002L);
        createApiPermissionIfNotExists("POST", "/api/v1/products/**", 1003L);
        createApiPermissionIfNotExists("GET", "/api/v1/products/**", 1004L);
        createApiPermissionIfNotExists("POST", "/api/v1/products/get/all", 1004L);
        createApiPermissionIfNotExists("PUT", "/api/v1/products/**", 1005L);
        createApiPermissionIfNotExists("DELETE", "/api/v1/products/**", 1006L);
        createApiPermissionIfNotExists("GET", "/api/v1/categories/**", 1007L);
        createApiPermissionIfNotExists("POST", "/api/v1/categories/get/all", 1007L);
        createApiPermissionIfNotExists("POST", "/api/v1/categories/id/get/", 1007L);
        createApiPermissionIfNotExists("GET", "/api/v1/subcategories/**", 1008L);
        createApiPermissionIfNotExists("POST", "/api/v1/subcategories/get/all", 1008L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-groups/get/all", 101L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-groups/get/id/", 101L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-groups/create/", 102L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-groups/update/", 103L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-groups/delete/", 104L);
        createApiPermissionIfNotExists("GET", "/admin/returns/summary", 1101L);
        createApiPermissionIfNotExists("POST", "/admin/returns/list", 1101L);
        createApiPermissionIfNotExists("GET", "/admin/returns/*", 1101L);
        createApiPermissionIfNotExists("POST", "/admin/returns/*/approve", 1102L);
        createApiPermissionIfNotExists("POST", "/admin/returns/*/reject", 1103L);
        createApiPermissionIfNotExists("GET", "/admin/refunds/summary", 1201L);
        createApiPermissionIfNotExists("POST", "/admin/refunds/list", 1201L);
        createApiPermissionIfNotExists("GET", "/admin/refunds/*", 1201L);
        createApiPermissionIfNotExists("POST", "/admin/refunds/*/process", 1202L);
        createApiPermissionIfNotExists("POST", "/admin/refunds/*/cancel", 1203L);
        createApiPermissionIfNotExists("GET", "/admin/refunds/*/history", 1201L);
        createApiPermissionIfNotExists("GET", "/admin/returns/*/history", 1101L);
        createApiPermissionIfNotExists("POST", "/admin/returns/*/receive", 1106L);
        createApiPermissionIfNotExists("POST", "/admin/returns/*/inspect/start", 1107L);
        createApiPermissionIfNotExists("POST", "/admin/returns/*/inspect/complete", 1107L);

        // ── CART (remaining endpoints) ──────────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/id", 502L);
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/id/get-or-create", 502L);
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/id/items/cartItemId", 503L);
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/userId/items/cartItemId", 504L);
        createApiPermissionIfNotExists("POST", "/api/v1/cart/user/userId/clear", 505L);

        // ── ORDERS (remaining endpoints) ────────────────────────────
        createApiPermissionIfNotExists("GET", "/api/v1/orders", 706L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/get/all", 706L);
        createApiPermissionIfNotExists("GET", "/api/v1/orders/summary", 706L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/id/", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/id/", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/number/", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/detail", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/history", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/items", 702L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/from-cart", 701L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/user/cancel", 701L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/bakong/initiate", 701L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/bakong/verify", 701L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/bakong/callback", 701L);
        createApiPermissionIfNotExists("POST", "/api/v1/orders/status/", 703L);

        // ── PAYMENTS (customer-facing) ───────────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/payments/user/id", 705L);
        createApiPermissionIfNotExists("POST", "/api/v1/payments/user/detail", 705L);
        createApiPermissionIfNotExists("POST", "/api/v1/payments/order/", 705L);
        createApiPermissionIfNotExists("POST", "/api/v1/payments/transaction/", 705L);
        createApiPermissionIfNotExists("POST", "/api/v1/payments/user/history", 705L);
        createApiPermissionIfNotExists("POST", "/api/v1/payments/get/all", 707L);

        // ── PAYMENT TRANSACTIONS (admin gateway audit trail) ─────────
        createApiPermissionIfNotExists("POST", "/api/v1/payment-transactions/get/all", 1301L);
        createApiPermissionIfNotExists("GET", "/api/v1/payment-transactions/**", 1301L);
        createApiPermissionIfNotExists("POST", "/api/v1/payment-transactions", 1302L);
        createApiPermissionIfNotExists("PUT", "/api/v1/payment-transactions/**", 1302L);

        // ── CANCELATIONS (admin) ─────────────────────────────────────
        createApiPermissionIfNotExists("GET", "/admin/cancelations/summary", 1401L);
        createApiPermissionIfNotExists("POST", "/admin/cancelations/list", 1401L);
        createApiPermissionIfNotExists("GET", "/admin/cancelations/*", 1401L);

        // ── CATEGORY ICONS (reuse category view/manage functions) ────
        createApiPermissionIfNotExists("GET", "/api/v1/category-icons/get/all", 1007L);
        createApiPermissionIfNotExists("GET", "/api/v1/category-icons/id", 1007L);
        createApiPermissionIfNotExists("POST", "/api/v1/category-icons/upload", 1001L);
        createApiPermissionIfNotExists("DELETE", "/api/v1/category-icons/delete", 1001L);

        // ── USER MANAGEMENT (admin only) ─────────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/user/create", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/id", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/id/image", 601L);
        createApiPermissionIfNotExists("GET", "/api/v1/user/id/user", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/All", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/id/update", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/id/delete", 601L);
        createApiPermissionIfNotExists("POST", "/api/v1/user/count", 601L);

        // ── USER PERMISSIONS (admin only) ────────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/user-permissions/get/all", 204L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-permissions/get/id/", 204L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-permissions/create/", 201L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-permissions/update/", 202L);
        createApiPermissionIfNotExists("POST", "/api/v1/user-permissions/delete/", 203L);

        // ── GROUP PERMISSIONS (admin only) ───────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/group-permissions/get/all", 304L);
        createApiPermissionIfNotExists("POST", "/api/v1/group-permissions/get/id/", 304L);
        createApiPermissionIfNotExists("POST", "/api/v1/group-permissions/create/", 301L);
        createApiPermissionIfNotExists("POST", "/api/v1/group-permissions/update/", 302L);
        createApiPermissionIfNotExists("POST", "/api/v1/group-permissions/delete/", 303L);

        // ── FUNCTION PERMISSIONS (admin only) ────────────────────────
        createApiPermissionIfNotExists("POST", "/api/v1/functions/get/all", 404L);
        createApiPermissionIfNotExists("POST", "/api/v1/functions/get/id/", 404L);
        createApiPermissionIfNotExists("POST", "/api/v1/functions/create/", 401L);
        createApiPermissionIfNotExists("POST", "/api/v1/functions/update/", 402L);
        createApiPermissionIfNotExists("POST", "/api/v1/functions/delete/", 403L);
    }

    private void createApiPermissionIfNotExists(String method, String api, Long funcId) {
        if (!apiPermissionRepository.existsByMethodAndApi(method, api)) {
            ApiPermission p = ApiPermission.builder()
                    .method(method)
                    .api(api)
                    .funcId(funcId)
                    .isActive(true)
                    .build();
            apiPermissionRepository.save(p);
            log.info("Seeded API Permission: {} {} -> funcId={}", method, api, funcId);
        }
    }
    private void seedCategoryIcons() {

        String baseUrl = "https://cdn.jsdelivr.net/npm/lucide-static@1.39.0/icons/";

        List<CategoryIcon> icons = List.of(

                CategoryIcon.builder()
                        .name("Electronics")
                        .url(baseUrl + "smartphone.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Mobile Phones")
                        .url(baseUrl + "smartphone.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Computers")
                        .url(baseUrl + "laptop.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Laptops")
                        .url(baseUrl + "laptop.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Desktop Computers")
                        .url(baseUrl + "monitor.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Tablets")
                        .url(baseUrl + "tablet.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Smart Watches")
                        .url(baseUrl + "watch.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Televisions")
                        .url(baseUrl + "tv.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Cameras")
                        .url(baseUrl + "camera.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Headphones")
                        .url(baseUrl + "headphones.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Speakers")
                        .url(baseUrl + "speaker.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Microphones")
                        .url(baseUrl + "mic.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Gaming")
                        .url(baseUrl + "gamepad-2.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Controllers")
                        .url(baseUrl + "gamepad.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Computer Mouse")
                        .url(baseUrl + "mouse.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Keyboards")
                        .url(baseUrl + "keyboard.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Printers")
                        .url(baseUrl + "printer.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Storage")
                        .url(baseUrl + "hard-drive.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("USB Devices")
                        .url(baseUrl + "usb.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Networking")
                        .url(baseUrl + "router.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Fashion")
                        .url(baseUrl + "shirt.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Men Fashion")
                        .url(baseUrl + "user.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Women Fashion")
                        .url(baseUrl + "user-round.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Shoes")
                        .url(baseUrl + "footprints.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Sport Shoes")
                        .url(baseUrl + "footprints.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Bags")
                        .url(baseUrl + "shopping-bag.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Backpacks")
                        .url(baseUrl + "backpack.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Watches")
                        .url(baseUrl + "watch.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Jewelry")
                        .url(baseUrl + "gem.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Glasses")
                        .url(baseUrl + "glasses.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Beauty")
                        .url(baseUrl + "sparkles.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Cosmetics")
                        .url(baseUrl + "brush.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Perfume")
                        .url(baseUrl + "spray-can.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Skincare")
                        .url(baseUrl + "droplets.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Health")
                        .url(baseUrl + "heart-pulse.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Medicine")
                        .url(baseUrl + "pill.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Pharmacy")
                        .url(baseUrl + "pill.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Food")
                        .url(baseUrl + "utensils.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Groceries")
                        .url(baseUrl + "shopping-basket.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Fruits")
                        .url(baseUrl + "apple.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Vegetables")
                        .url(baseUrl + "carrot.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Meat")
                        .url(baseUrl + "beef.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Seafood")
                        .url(baseUrl + "fish.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Bakery")
                        .url(baseUrl + "croissant.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Coffee")
                        .url(baseUrl + "coffee.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Drinks")
                        .url(baseUrl + "cup-soda.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Restaurant")
                        .url(baseUrl + "utensils.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Home")
                        .url(baseUrl + "house.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Furniture")
                        .url(baseUrl + "sofa.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Kitchen")
                        .url(baseUrl + "cooking-pot.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Bedroom")
                        .url(baseUrl + "bed.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Bathroom")
                        .url(baseUrl + "bath.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Lighting")
                        .url(baseUrl + "lamp.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Garden")
                        .url(baseUrl + "flower-2.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Tools")
                        .url(baseUrl + "wrench.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Hardware")
                        .url(baseUrl + "hammer.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Automotive")
                        .url(baseUrl + "car.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Cars")
                        .url(baseUrl + "car-front.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Motorcycles")
                        .url(baseUrl + "bike.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Bicycles")
                        .url(baseUrl + "bike.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Car Parts")
                        .url(baseUrl + "settings-2.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Travel")
                        .url(baseUrl + "plane.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Hotels")
                        .url(baseUrl + "hotel.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Luggage")
                        .url(baseUrl + "luggage.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Tickets")
                        .url(baseUrl + "ticket.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Books")
                        .url(baseUrl + "book-open.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Education")
                        .url(baseUrl + "graduation-cap.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Office Supplies")
                        .url(baseUrl + "briefcase.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Stationery")
                        .url(baseUrl + "pencil.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Music")
                        .url(baseUrl + "music.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Movies")
                        .url(baseUrl + "film.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Photography")
                        .url(baseUrl + "image.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Sports")
                        .url(baseUrl + "trophy.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Fitness")
                        .url(baseUrl + "dumbbell.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Outdoor")
                        .url(baseUrl + "mountain.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Toys")
                        .url(baseUrl + "toy-brick.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Baby Products")
                        .url(baseUrl + "baby.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Pets")
                        .url(baseUrl + "paw-print.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Pet Food")
                        .url(baseUrl + "bone.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Stationery Supplies")
                        .url(baseUrl + "notebook.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Office")
                        .url(baseUrl + "building-2.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Business")
                        .url(baseUrl + "briefcase-business.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Finance")
                        .url(baseUrl + "wallet.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Banking")
                        .url(baseUrl + "landmark.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Credit Cards")
                        .url(baseUrl + "credit-card.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Money")
                        .url(baseUrl + "banknote.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Cryptocurrency")
                        .url(baseUrl + "bitcoin.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Shopping")
                        .url(baseUrl + "shopping-cart.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Discount")
                        .url(baseUrl + "badge-percent.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Coupons")
                        .url(baseUrl + "ticket-percent.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Gift")
                        .url(baseUrl + "gift.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Delivery")
                        .url(baseUrl + "truck.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Shipping")
                        .url(baseUrl + "package.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Warehouse")
                        .url(baseUrl + "warehouse.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Security")
                        .url(baseUrl + "shield-check.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Mobile Accessories")
                        .url(baseUrl + "cable.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Batteries")
                        .url(baseUrl + "battery.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Smart Home")
                        .url(baseUrl + "home.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Software")
                        .url(baseUrl + "code.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Cloud Services")
                        .url(baseUrl + "cloud.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Internet")
                        .url(baseUrl + "globe.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Accessories")
                        .url(baseUrl + "watch.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("New Arrivals")
                        .url(baseUrl + "sparkles.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Popular")
                        .url(baseUrl + "flame.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Best Sellers")
                        .url(baseUrl + "trending-up.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Offers")
                        .url(baseUrl + "badge-percent.svg")
                        .build(),

                CategoryIcon.builder()
                        .name("Other")
                        .url(baseUrl + "shapes.svg")
                        .build()
        );

        icons.forEach(icon -> {
            if (!categoryIconRepository.existsByName(icon.getName())) {
                categoryIconRepository.save(icon);
            }
        });
    }
}
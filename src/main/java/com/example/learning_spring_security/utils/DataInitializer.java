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

    private final GroupPermissionRepository groupPermissionRepository;
    private final UserGroupRepository userGroupRepository;
    @Override
    public void run(String... args) {

        List<String> roles = List.of("ADMIN", "MANAGER", "STAFF", "SALES");
        roles.forEach(this::createRoleIfNotFound);
        seedGroups();
        createAdminIfNotFound();
        seedFunctionPermissions();

        seedAdminGroupPermissions();

        createAdminIfNotFound();
    }
    private void createRoleIfNotFound(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = Role.builder()
                    .name(roleName)
                    .build();
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
                buildFunction(403L, "FUNC_DELETE",       "Delete Function", "Remove a function record",   "FUNCTION")
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

                buildGroup("ADM", "Administrator",
                        "System administrator group"),

                buildGroup("MNG", "Manager",
                        "Manager group"),

                buildGroup("STF", "Staff",
                        "Staff group"),

                buildGroup("SAL", "Sales",
                        "Sales group"),
                buildGroup("USR",
                        "Default User Group", "Default group for regular users")
        );

        groups.forEach(group -> {
            if (!groupRepository.existsByGroupCode(group.getGroupCode())) {
                groupRepository.save(group);
                System.out.println("Group seeded: " + group.getGroupCode());
            }
        });
    }
    private Group buildGroup(
            String groupCode,
            String name,
            String description) {

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

        Group adminGroup = groupRepository
                .findByGroupCode("ADM")
                .orElseThrow(() ->
                        new RuntimeException("Administrator group not found"));

        List<FunctionPermission> functions =
                functionPermissionRepository.findAll();

        for (FunctionPermission function : functions) {

            boolean exists =
                    groupPermissionRepository.existsByGroupIdAndFuncId(
                            adminGroup.getId(),
                            function.getFuncId());

            if (!exists) {

                GroupPermission permission = GroupPermission.builder()
                        .groupId(adminGroup.getId())
                        .funcId(function.getFuncId())
                        .isActive(true)
                        .isDelete(false)
                        .build();

                permission.setCreatedAt(LocalDateTime.now());

                groupPermissionRepository.save(permission);

                System.out.println(
                        "Assigned "
                                + function.getFuncCode()
                                + " -> ADM");
            }
        }
    }
    private void assignAdminToGroup(User admin) {

        Group adminGroup = groupRepository
                .findByGroupCode("ADM")
                .orElseThrow(() ->
                        new RuntimeException("Administrator group not found"));

        boolean exists =
                userGroupRepository.existsByUserIdAndGroupId(
                        admin.getId(),
                        adminGroup.getId());

        if (!exists) {

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
}
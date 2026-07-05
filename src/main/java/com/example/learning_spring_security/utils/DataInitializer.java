package com.example.learning_spring_security.utils;

import com.example.learning_spring_security.Constant.Constant;
import com.example.learning_spring_security.Model.FunctionPermission;
import com.example.learning_spring_security.Model.Role;
import com.example.learning_spring_security.Model.User;
import com.example.learning_spring_security.Repository.FunctionPermissionRepository;
import com.example.learning_spring_security.Repository.RoleRepository;
import com.example.learning_spring_security.Repository.UserRepository;
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
    private final FunctionPermissionRepository functionPermissionRepository;

    @Override
    public void run(String... args) {

        List<String> roles = List.of("ADMIN", "MANAGER", "STAFF", "SALES");

        roles.forEach(this::createRoleIfNotFound);

        createAdminIfNotFound();
        seedFunctionPermissions();
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

    private FunctionPermission buildFunction(Long funcId, String funcCode, String funcName,
                                             String description, String module) {
        FunctionPermission fn = FunctionPermission.builder()
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
}
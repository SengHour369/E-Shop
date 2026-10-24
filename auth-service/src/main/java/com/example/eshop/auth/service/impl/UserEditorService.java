package com.example.eshop.auth.service.impl;

import com.example.eshop.auth.controller.UserEditorController.Update;
import com.example.eshop.auth.model.*;
import com.example.eshop.auth.repository.*;
import com.example.eshop.auth.exception.BadRequestException;
import com.example.eshop.auth.exception.ResourceNotFoundException;
import com.example.eshop.common.audit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.time.LocalDate;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
public class UserEditorService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final GroupRepository groups;
    private final UserGroupRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final UserPermissionRepository userPermissions;
    private final FunctionPermissionRepository functions;
    private final AuditLogService audit;
    private final com.example.eshop.auth.service.UserApprovalRouting approvals;

    private User user(Long id) {
        return users.findUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        User u = user(id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", u.getId());
        out.put("fullName", text(u.getFullName()));
        out.put("username", u.getUsername());
        out.put("email", u.getEmail());
        out.put("phone", text(u.getPhone()));
        out.put("birthdate", text(u.getBirthdate()));
        out.put("status", u.getStatus());
        out.put("jobTitle", text(u.getJobTitle()));
        out.put("department", text(u.getDepartment()));
        out.put("approvalReviewerId", u.getApprovalReviewerId());
        out.put("managerId", u.getManagerId());
        out.put("gender", text(u.getGender()));
        out.put("address", text(u.getProfileAddress()));
        out.put("language", u.getLanguage() == null ? "en-US" : u.getLanguage());
        out.put("twoFactorRequired", u.isTwoFactorRequired());
        out.put("image", text(u.getProfileImage()));
        out.put(
                "roleIds",
                u.getRoles() == null
                        ? List.of()
                        : u.getRoles().stream().map(Role::getId).toList()
        );
        out.put(
                "groupIds",
                memberships.findByUserIdAndIsDeleteFalse(id).stream()
                        .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                        .map(UserGroup::getGroupId)
                        .toList()
        );
        var activeFunctions = functions.findAll().stream()
                .filter(f ->
                        Boolean.TRUE.equals(f.getIsActive())
                                && !Boolean.TRUE.equals(f.getIsDelete())
                )
                .map(FunctionPermission::getFuncId)
                .collect(java.util.stream.Collectors.toSet());
        out.put(
                "permissionIds",
                userPermissions.findAllByUserId(id).stream()
                        .filter(p ->
                                Boolean.TRUE.equals(p.getIsActive())
                                        && !Boolean.TRUE.equals(p.getIsDelete())
                        )
                        .map(UserPermission::getFuncId)
                        .filter(activeFunctions::contains)
                        .distinct()
                        .toList()
        );
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> options() {
        var departments = new TreeSet<String>(List.of(
                "Sales",
                "Product Management",
                "Customer Support",
                "Finance",
                "Marketing",
                "Operations",
                "Engineering"
        ));
        var people = users.findAllUser();
        people.stream()
                .map(User::getDepartment)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .forEach(departments::add);
        return Map.of(
                "roles",
                roles.findAll().stream()
                        .map(r -> Map.of(
                                "id", r.getId(),
                                "name", r.getName()
                        ))
                        .toList(),
                "groups",
                groups.findAll().stream()
                        .filter(g ->
                                !Boolean.TRUE.equals(g.getIsDelete())
                                        && Boolean.TRUE.equals(g.getIsActive())
                        )
                        .map(g -> Map.of(
                                "id", g.getId(),
                                "name", text(g.getName()),
                                "description", text(g.getDescription())
                        ))
                        .toList(),
                "managers",
                people.stream()
                        .filter(u -> "ACT".equals(u.getStatus()))
                        .map(u -> Map.of(
                                "id", u.getId(),
                                "name", text(u.getFullName()).isBlank()
                                        ? u.getUsername()
                                        : u.getFullName(),
                                "username", u.getUsername()
                        ))
                        .toList(),
                "permissions",
                functions.findAll().stream()
                        .filter(f ->
                                Boolean.TRUE.equals(f.getIsActive())
                                        && !Boolean.TRUE.equals(f.getIsDelete())
                        )
                        .map(f -> Map.of(
                                "id", f.getFuncId(),
                                "name", text(f.getFuncName()),
                                "code", f.getFuncCode(),
                                "description", text(f.getDescription()),
                                "module", text(f.getModule())
                        ))
                        .toList(),
                "reviewers", approvals.candidates(),
                "departments", departments
        );
    }

    @Transactional
    public Map<String, Object> update(Long id, Update b) {
        User u = user(id);

        if (!Set.of("en-US", "km-KH").contains(b.language())) {
            throw new BadRequestException("Choose a supported language");
        }

        if (b.gender() != null
                && !Set.of("", "MALE", "FEMALE", "OTHER").contains(b.gender())) {
            throw new BadRequestException("Choose a valid gender");
        }

        if (b.birthdate() != null && !b.birthdate().isBlank()) {
            try {
                if (LocalDate.parse(b.birthdate()).isAfter(LocalDate.now())) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                throw new BadRequestException("Choose a valid date of birth");
            }
        }

        if (b.managerId() != null) {
            if (b.managerId().equals(id)) {
                throw new BadRequestException("A user cannot manage themselves");
            }

            user(b.managerId());
        }

        if (b.approvalReviewerId() != null
                && (b.approvalReviewerId().equals(id)
                || !approvals.eligible(
                user(b.approvalReviewerId())
        ))) {
            throw new BadRequestException(
                    "Choose an independent active reviewer with review, "
                            + "approval and execution permissions"
            );
        }

        if (users.findAllUser().stream().anyMatch(other ->
                !other.getId().equals(id)
                        && (other.getUsername().equalsIgnoreCase(
                        b.username().trim()
                )
                        || other.getEmail().equalsIgnoreCase(
                        b.email().trim()
                )
                        || Objects.equals(other.getPhone(), b.phone().trim())))) {
            throw new BadRequestException(
                    "Username, email or phone already belongs to another user"
            );
        }

        var roleIds = new HashSet<>(b.roleIds());
        var selectedRoles = roles.findAllById(roleIds);

        if (selectedRoles.size() != roleIds.size()) {
            throw new BadRequestException("Choose valid roles");
        }

        var groupIds = new HashSet<>(b.groupIds());
        var selectedGroups = groups.findAllById(groupIds);

        if (selectedGroups.size() != groupIds.size()
                || selectedGroups.stream().anyMatch(g ->
                Boolean.TRUE.equals(g.getIsDelete())
                        || !Boolean.TRUE.equals(g.getIsActive()))) {
            throw new BadRequestException("Choose active user groups");
        }

        Set<Long> permissionIds = b.permissionIds() == null
                ? null
                : new HashSet<>(b.permissionIds());

        if (permissionIds != null) {
            var chosen = functions.findAllById(permissionIds);

            if (chosen.size() != permissionIds.size()
                    || chosen.stream().anyMatch(f ->
                    !Boolean.TRUE.equals(f.getIsActive())
                            || Boolean.TRUE.equals(f.getIsDelete()))) {
                throw new BadRequestException(
                        "Choose valid active user permissions"
                );
            }
        }

        String actor = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        if (actor.equals(u.getUsername())
                && (!b.active()
                || selectedRoles.stream().noneMatch(r ->
                "ADMIN".equals(r.getName())))) {
            throw new BadRequestException(
                    "You cannot remove your own administrator access"
            );
        }

        boolean accessChanged =
                !new HashSet<>(
                        u.getRoles().stream().map(Role::getId).toList()
                ).equals(roleIds)
                        || u.isTwoFactorRequired() != b.twoFactorRequired()
                        || !Objects.equals(
                        u.getUsername(),
                        b.username().trim()
                )
                        || (!b.active() && "ACT".equals(u.getStatus()));
        u.setFullName(b.firstName().trim() + " " + b.lastName().trim());
        u.setUsername(b.username().trim());
        u.setEmail(b.email().trim());
        u.setPhone(b.phone().trim());
        u.setBirthdate(b.birthdate());
        u.setJobTitle(b.jobTitle());
        u.setDepartment(b.department());
        u.setManagerId(b.managerId());
        u.setApprovalReviewerId(b.approvalReviewerId());
        u.setGender(b.gender());
        u.setProfileAddress(b.address());
        u.setLanguage(b.language());
        u.setTwoFactorRequired(b.twoFactorRequired());
        u.setRoles(selectedRoles);

        if (b.active() != "ACT".equals(u.getStatus())) {
            u.setStatus(b.active() ? "ACT" : "BLK");
        }

        users.save(u);
        var existing = memberships.findByUserId(id);

        for (var m : existing) {
            boolean on = groupIds.remove(m.getGroupId());

            if (!Objects.equals(m.getIsActive(), on)) {
                accessChanged = true;
            }

            m.setIsActive(on);
            m.setIsDelete(!on);
        }

        memberships.saveAll(existing);

        for (Long groupId : groupIds) {
            memberships.save(
                    UserGroup.builder()
                            .userId(id)
                            .groupId(groupId)
                            .isActive(true)
                            .isDelete(false)
                            .build()
            );
            accessChanged = true;
        }

        if (permissionIds != null) {
            var existingPermissions = userPermissions.findAllByUserId(id);
            var oldIds = existingPermissions.stream()
                    .filter(p ->
                            Boolean.TRUE.equals(p.getIsActive())
                                    && !Boolean.TRUE.equals(p.getIsDelete())
                    )
                    .map(UserPermission::getFuncId)
                    .collect(java.util.stream.Collectors.toSet());

            if (!oldIds.equals(permissionIds)) {
                accessChanged = true;
            }

            for (var p : existingPermissions) {
                boolean on = permissionIds.remove(p.getFuncId());
                p.setIsActive(on);
                p.setIsDelete(!on);
            }

            userPermissions.saveAll(existingPermissions);

            for (Long function : permissionIds) {
                userPermissions.save(
                        UserPermission.builder()
                                .userId(id)
                                .funcId(function)
                                .isActive(true)
                                .isDelete(false)
                                .build()
                );
            }
        }

        if (accessChanged) {
            refreshTokens.deleteByUser(u);
        }

        audit.record(
                AuditEvent.success(
                        AuditAction.UPDATE,
                        "USER",
                        id,
                        null,
                        Map.of(
                                "profileUpdated", true,
                                "accessChanged", accessChanged
                        )
                )
        );
        return get(id);
    }

    @Transactional
    public Map<String, Object> photo(Long id, MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) {
            throw new BadRequestException(
                    "Choose a profile photo up to 2 MB"
            );
        }

        try {
            byte[] bytes = file.getBytes();

            try (var input = ImageIO.createImageInputStream(
                    new ByteArrayInputStream(bytes)
            )) {
                var readers = ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new BadRequestException(
                            "Choose a valid JPEG or PNG image"
                    );
                }

                var reader = readers.next();

                try {
                    reader.setInput(input);
                    String format = reader.getFormatName()
                            .toLowerCase(java.util.Locale.ROOT);

                    if (!Set.of("jpeg", "png").contains(format)
                            || reader.getWidth(0) > 4096
                            || reader.getHeight(0) > 4096) {
                        throw new BadRequestException(
                                "Choose a JPEG or PNG image up to 4096 pixels"
                        );
                    }

                    var source = reader.read(0);
                    double scale = Math.min(
                            1,
                            256.0 / Math.max(
                                    source.getWidth(),
                                    source.getHeight()
                            )
                    );
                    var thumbnail = new java.awt.image.BufferedImage(
                            Math.max(
                                    1,
                                    (int) (source.getWidth() * scale)
                            ),
                            Math.max(
                                    1,
                                    (int) (source.getHeight() * scale)
                            ),
                            java.awt.image.BufferedImage.TYPE_INT_ARGB
                    );
                    var graphics = thumbnail.createGraphics();

                    try {
                        graphics.setRenderingHint(
                                java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints
                                        .VALUE_INTERPOLATION_BILINEAR
                        );
                        graphics.drawImage(
                                source,
                                0,
                                0,
                                thumbnail.getWidth(),
                                thumbnail.getHeight(),
                                null
                        );
                    } finally {
                        graphics.dispose();
                    }

                    var output = new java.io.ByteArrayOutputStream();
                    ImageIO.write(thumbnail, "png", output);
                    User u = user(id);
                    u.setProfileImage(
                            "data:image/png;base64,"
                                    + Base64.getEncoder()
                                    .encodeToString(
                                            output.toByteArray()
                                    )
                    );
                    users.save(u);
                    audit.record(
                            AuditEvent.success(
                                    AuditAction.UPDATE,
                                    "USER",
                                    id,
                                    null,
                                    Map.of("profilePhotoUpdated", true)
                            )
                    );
                } finally {
                    reader.dispose();
                }
            }

            return get(id);
        } catch (java.io.IOException e) {
            throw new BadRequestException("Could not read profile photo");
        }
    }
}
package com.lms.backend.config;

import com.lms.backend.entity.AccountStatus;
import com.lms.backend.entity.Role;
import com.lms.backend.entity.User;
import com.lms.backend.entity.Permission;
import com.lms.backend.entity.Material;
import com.lms.backend.entity.TestDefinition;
import com.lms.backend.repository.RoleRepository;
import com.lms.backend.repository.UserRepository;
import com.lms.backend.repository.PermissionRepository;
import com.lms.backend.repository.MaterialRepository;
import com.lms.backend.repository.TestDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final MaterialRepository materialRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing LIMS permissions, roles and default admin user...");

        // Drop the old roles check constraint if it exists (remnant from when RoleName was an Enum)
        try {
            jdbcTemplate.execute("ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check");
        } catch (Exception e) {
            log.warn("Failed to drop roles_name_check constraint, it might not exist", e);
        }

        // Define permissions
        String[][] permissionData = {
            {"CUSTOMER_VIEW", "View customer details"},
            {"CUSTOMER_CREATE", "Create new customers"},
            {"CUSTOMER_EDIT", "Edit existing customers"},
            {"CUSTOMER_DELETE", "Delete customers"},

            {"PROJECT_VIEW", "View projects"},
            {"PROJECT_CREATE", "Create new projects"},
            {"PROJECT_EDIT", "Edit existing projects"},
            {"PROJECT_DELETE", "Delete projects"},

            {"WORKORDER_VIEW", "View work orders"},
            {"WORKORDER_CREATE", "Create new work orders"},
            {"WORKORDER_EDIT", "Edit existing work orders"},
            {"WORKORDER_DELETE", "Delete work orders"},

            {"SAMPLE_VIEW", "View samples"},
            {"SAMPLE_CREATE", "Register new samples"},
            {"SAMPLE_RECEIVE", "Mark samples as physically received"},
            {"SAMPLE_ASSIGN", "Assign tests to samples"},
            {"SAMPLE_EDIT", "Modify sample details"},
            {"SAMPLE_DELETE", "Delete samples"},

            {"TEST_ASSIGN", "Assign/manage test configurations"},
            {"TEST_START", "Start laboratory testing process"},
            {"RESULT_DRAFT", "Enter and save draft test results"},
            {"RESULT_SUBMIT", "Submit test results for review"},

            {"RESULT_REVIEW", "Review submitted test results"},
            {"RESULT_APPROVE", "Approve reviewed test results"},
            {"RESULT_REJECT", "Reject/send back test results"},

            {"REPORT_VIEW", "View laboratory reports"},
            {"REPORT_GENERATE", "Generate final test reports"},
            {"REPORT_RELEASE", "Release generated reports"},
            {"REPORT_DOWNLOAD", "Download test reports"},

            {"USER_VIEW", "View system users"},
            {"USER_CREATE", "Create new users"},
            {"USER_EDIT", "Edit existing users"},
            {"USER_DELETE", "Delete users"},

            {"INVOICE_VIEW", "View invoices"},
            {"INVOICE_CREATE", "Create invoices"},
            {"INVOICE_EDIT", "Edit invoices"},
            {"INVOICE_DELETE", "Delete invoices"},

            {"MASTER_VIEW", "View material and test definition master data"},
            {"MASTER_CREATE", "Create master data entries"},
            {"MASTER_EDIT", "Edit master data entries"},
            {"MASTER_DELETE", "Delete master data entries"},

            {"ROLE_MANAGE", "Manage roles and permission mappings"},
            {"SETTINGS_MANAGE", "Modify system configurations and settings"},
            {"AUDIT_VIEW", "View system audit logs"},
            {"BACKUP_MANAGE", "Perform database backups and restore"}
        };

        // Create/retrieve permissions
        Map<String, Permission> permMap = new HashMap<>();
        for (String[] data : permissionData) {
            String code = data[0];
            String desc = data[1];
            Permission perm = permissionRepository.findByCode(code)
                    .orElseGet(() -> {
                        Permission newPerm = Permission.builder()
                                .code(code)
                                .description(desc)
                                .build();
                        return permissionRepository.save(newPerm);
                    });
            permMap.put(code, perm);
        }

        // Define Role permissions
        Map<String, Set<String>> rolePerms = new HashMap<>();
        
        // Super Admin gets all
        rolePerms.put("ROLE_SUPER_ADMIN", permMap.keySet());

        // Admin gets all except role/settings/backup/audit and testing/review/approval write actions
        Set<String> adminPerms = new java.util.HashSet<>(permMap.keySet());
        adminPerms.remove("ROLE_MANAGE");
        adminPerms.remove("SETTINGS_MANAGE");
        adminPerms.remove("BACKUP_MANAGE");
        adminPerms.remove("AUDIT_VIEW");
        adminPerms.remove("TEST_START");
        adminPerms.remove("RESULT_DRAFT");
        adminPerms.remove("RESULT_SUBMIT");
        rolePerms.put("ROLE_ADMIN", adminPerms);

        // Lab Manager
        rolePerms.put("ROLE_LAB_MANAGER", Set.of(
            "CUSTOMER_VIEW", "PROJECT_VIEW", "WORKORDER_VIEW", "MASTER_VIEW",
            "SAMPLE_VIEW", "SAMPLE_CREATE", "SAMPLE_RECEIVE", "SAMPLE_ASSIGN", "SAMPLE_EDIT",
            "TEST_ASSIGN", "REPORT_VIEW", "INVOICE_VIEW", "USER_VIEW",
            "REPORT_GENERATE", "REPORT_RELEASE", "REPORT_DOWNLOAD"
        ));

        // Quality Engineer
        rolePerms.put("ROLE_QUALITY_ENGINEER", Set.of(
            "SAMPLE_VIEW", "MASTER_VIEW", "RESULT_REVIEW", "RESULT_APPROVE", "RESULT_REJECT",
            "REPORT_VIEW", "REPORT_DOWNLOAD"
        ));

        // Technician
        rolePerms.put("ROLE_TECHNICIAN", Set.of(
            "SAMPLE_VIEW", "TEST_START", "RESULT_DRAFT", "RESULT_SUBMIT", "REPORT_DOWNLOAD", "TEST_ASSIGN", "MASTER_VIEW"
        ));

        // Client Viewer
        rolePerms.put("ROLE_CLIENT_VIEWER", Set.of(
            "CUSTOMER_VIEW", "PROJECT_VIEW", "WORKORDER_VIEW", "SAMPLE_VIEW",
            "REPORT_DOWNLOAD", "INVOICE_VIEW"
        ));

        // Reception / Data Entry
        rolePerms.put("ROLE_RECEPTION", Set.of(
            "CUSTOMER_VIEW", "CUSTOMER_CREATE", "CUSTOMER_EDIT",
            "PROJECT_VIEW", "PROJECT_CREATE", "PROJECT_EDIT",
            "WORKORDER_VIEW", "WORKORDER_CREATE", "WORKORDER_EDIT",
            "SAMPLE_VIEW", "SAMPLE_CREATE", "SAMPLE_RECEIVE", "SAMPLE_EDIT",
            "REPORT_VIEW", "REPORT_DOWNLOAD",
            "INVOICE_VIEW"
        ));

        // Seed Roles and associate permissions
        Map<String, Role> seededRoles = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : rolePerms.entrySet()) {
            String roleName = entry.getKey();
            Set<Permission> permissions = entry.getValue().stream()
                    .map(permMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Optional<Role> existingRole = roleRepository.findByName(roleName);
            Role role;
            if (existingRole.isEmpty()) {
                role = Role.builder()
                        .name(roleName)
                        .description("System role for " + roleName.substring(5).replace("_", " "))
                        .permissions(permissions)
                        .build();
                role = roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            } else {
                role = existingRole.get();
                role.setPermissions(permissions);
                role = roleRepository.save(role);
            }
            seededRoles.put(roleName, role);
        }

        // Seed default SUPER_ADMIN user
        String adminEmail = "admin@wemurz.com";
        Optional<User> existingAdmin = userRepository.findByEmail(adminEmail);
        if (existingAdmin.isEmpty()) {
            Role adminRole = seededRoles.get("ROLE_SUPER_ADMIN");

            User admin = User.builder()
                    .name("LIMS Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@WeMurz25"))
                    .status(AccountStatus.ACTIVE)
                    .deleted(false)
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Seeded default super admin user with email: {}", adminEmail);
        }

        // Seed default Materials and Test Definitions
        seedMaterialsAndTests();
    }

    private void seedMaterialsAndTests() {
        log.info("Seeding default Material Master and Test Definitions...");

        // Concrete
        Material concrete = getOrCreateMaterial("CON", "Concrete", "CON", "Concrete construction samples");
        getOrCreateTest(concrete, "Slump Test", "mm", "IS:1199", "Slump Cone Method");
        getOrCreateTest(concrete, "Compression 7 Days", "N/mm²", "IS:516", "UTM Compression");
        getOrCreateTest(concrete, "Compression 28 Days", "N/mm²", "IS:516", "UTM Compression");

        // Cement
        Material cement = getOrCreateMaterial("CEM", "Cement", "CEM", "Cement raw or process samples");
        getOrCreateTest(cement, "Fineness", "m²/kg", "IS:4031 Part 2", "Blaine Air Permeability");
        getOrCreateTest(cement, "Setting Time", "min", "IS:4031 Part 5", "Vicat Needle Method");
        getOrCreateTest(cement, "Soundness", "mm", "IS:4031 Part 3", "Le-Chatelier Method");

        // Steel
        Material steel = getOrCreateMaterial("STL", "Steel", "STL", "Steel reinforcement bars");
        getOrCreateTest(steel, "Tensile Strength", "N/mm²", "IS:1608", "UTM Tensile Test");
        getOrCreateTest(steel, "Bend Test", "degrees", "IS:1599", "Mandrel Bend Method");

        // Soil
        Material soil = getOrCreateMaterial("SOL", "Soil", "SOL", "Soil and subgrade samples");
        getOrCreateTest(soil, "Liquid Limit", "%", "IS:2720 Part 5", "Casagrande Cup");
        getOrCreateTest(soil, "Plastic Limit", "%", "IS:2720 Part 5", "Thread Rolling Method");
        getOrCreateTest(soil, "CBR (California Bearing Ratio)", "%", "IS:2720 Part 16", "Penetration Piston");
    }

    private Material getOrCreateMaterial(String code, String name, String prefix, String description) {
        Optional<Material> existing = materialRepository.findByMaterialCode(code);
        if (existing.isPresent()) {
            return existing.get();
        }
        Material m = Material.builder()
                .materialCode(code)
                .materialName(name)
                .samplePrefix(prefix)
                .description(description)
                .active(true)
                .build();
        Material saved = materialRepository.save(m);
        log.info("Seeded Material: {}", code);
        return saved;
    }

    private void getOrCreateTest(Material material, String testName, String unit, String spec, String method) {
        // Query to check if testName already exists for the material
        boolean exists = testDefinitionRepository.findByMaterialId(material.getId()).stream()
                .anyMatch(t -> t.getTestName().equalsIgnoreCase(testName));
        if (!exists) {
            TestDefinition t = TestDefinition.builder()
                    .material(material)
                    .testName(testName)
                    .unit(unit)
                    .specification(spec)
                    .method(method)
                    .active(true)
                    .build();
            testDefinitionRepository.save(t);
            log.info("Seeded Test Definition: {} for {}", testName, material.getMaterialCode());
        }
    }
}

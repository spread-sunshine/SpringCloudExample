package com.template.microservice.config;

import com.template.microservice.model.entity.Role;
import com.template.microservice.model.entity.User;
import com.template.microservice.repository.RoleRepository;
import com.template.microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * 数据初始化器，用于在开发环境自动创建默认测试账号。
 * 仅在 dev 和 test profile 下生效。
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Profile({ "dev", "test" })
    public CommandLineRunner initTestData() {
        return args -> {
            log.info("开始初始化测试数据...");

            // 创建角色
            Role userRole = createRoleIfNotExists("ROLE_USER", "Default user role");
            Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "Administrator role");

            // 创建管理员账号
            createAdminUserIfNotExists(adminRole, userRole);

            // 创建普通用户账号
            createUserIfNotExists(userRole);

            log.info("测试数据初始化完成");
        };
    }

    /**
     * 创建角色（如果不存在）
     */
    private Role createRoleIfNotExists(String roleName, String description) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    log.info("创建角色: {}", roleName);
                    Role role = Role.builder()
                            .name(roleName)
                            .description(description)
                            .build();
                    return roleRepository.save(role);
                });
    }

    /**
     * 创建管理员账号（如果不存在）
     */
    private void createAdminUserIfNotExists(Role adminRole, Role userRole) {
        String adminUsername = "admin";
        if (!userRepository.existsByUsername(adminUsername)) {
            log.info("创建管理员账号: {}", adminUsername);
            User admin = User.builder()
                    .username(adminUsername)
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("Admin")
                    .lastName("User")
                    .enabled(true)
                    .locked(false)
                    .roles(Set.of(adminRole, userRole))
                    .build();
            userRepository.save(admin);
            log.info("管理员账号创建成功 - 用户名: {}, 密码: admin123, 角色: ROLE_ADMIN, ROLE_USER",
                    adminUsername);
        }
    }

    /**
     * 创建普通用户账号（如果不存在）
     */
    private void createUserIfNotExists(Role userRole) {
        String userUsername = "user";
        if (!userRepository.existsByUsername(userUsername)) {
            log.info("创建普通用户账号: {}", userUsername);
            User user = User.builder()
                    .username(userUsername)
                    .email("user@example.com")
                    .password(passwordEncoder.encode("user123"))
                    .firstName("Test")
                    .lastName("User")
                    .enabled(true)
                    .locked(false)
                    .roles(Set.of(userRole))
                    .build();
            userRepository.save(user);
            log.info("普通用户账号创建成功 - 用户名: {}, 密码: user123, 角色: ROLE_USER",
                    userUsername);
        }
    }
}

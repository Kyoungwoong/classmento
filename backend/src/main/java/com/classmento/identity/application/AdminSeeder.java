package com.classmento.identity.application;

import com.classmento.identity.domain.AppUser;
import com.classmento.identity.domain.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 앱 시작 시 ADMIN_EMAIL/ADMIN_PASSWORD로 관리자 계정을 한 번 만든다(이미 있으면 건드리지 않음). */
@Component
public class AdminSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AdminProperties properties;

  public AdminSeeder(
      AppUserRepository users, PasswordEncoder passwordEncoder, AdminProperties properties) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (blank(properties.email()) || blank(properties.password())) {
      return;
    }
    String email = properties.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email)) {
      return;
    }
    users.save(
        AppUser.local(
            email, passwordEncoder.encode(properties.password()), "관리자", AppUser.Role.ADMIN));
    log.info("관리자 계정 생성: {}", email);
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  @ConfigurationProperties("classmento.admin")
  public record AdminProperties(String email, String password) {}
}

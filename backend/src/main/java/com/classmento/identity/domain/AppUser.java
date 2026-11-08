package com.classmento.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

  public enum Role {
    USER,
    ADMIN
  }

  public enum Provider {
    LOCAL,
    GOOGLE,
    KAKAO
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  private String passwordHash;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Provider provider;

  private String providerId;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected AppUser() {}

  private AppUser(
      String email,
      String passwordHash,
      String name,
      Role role,
      Provider provider,
      String providerId) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.name = name;
    this.role = role;
    this.provider = provider;
    this.providerId = providerId;
  }

  /** 이메일·비밀번호 가입. 이메일은 {@link Email}로 검증·정규화된 값만 받는다. */
  public static AppUser local(Email email, String passwordHash, String name, Role role) {
    return new AppUser(email.value(), passwordHash, name, role, Provider.LOCAL, null);
  }

  /** 소셜 가입. 비밀번호가 없으므로 이메일 로그인은 할 수 없다. */
  public static AppUser social(Email email, String name, Provider provider, String providerId) {
    return new AppUser(email.value(), null, name, Role.USER, provider, providerId);
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getName() {
    return name;
  }

  public Role getRole() {
    return role;
  }

  public Provider getProvider() {
    return provider;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

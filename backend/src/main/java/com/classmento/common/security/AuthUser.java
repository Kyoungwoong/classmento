package com.classmento.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 로그인한 사용자(세션에 저장되는 principal). 공용 커널 타입이라 모든 컨텍스트가 {@code @AuthenticationPrincipal AuthUser user}로
 * 받는다. 다른 컨텍스트는 {@link #id()}(userId)만 쓰고, 회원 상세가 필요하면 identity 컨텍스트에 묻는다.
 *
 * <p>이메일 로그인(UserDetails)과 소셜 로그인(OAuth2User)을 하나로 통일한다. 세션 DB에 직렬화되므로 단순 값만 둔다. identity 도메인에 의존하지
 * 않도록 역할은 문자열("USER"/"ADMIN")로 담는다. 생성은 identity 컨텍스트가 한다.
 *
 * @param role "USER" 또는 "ADMIN"
 * @param passwordHash 비밀번호 로그인 검증용(소셜 계정은 null)
 */
public record AuthUser(Long id, String email, String name, String role, String passwordHash)
    implements UserDetails, OAuth2User {

  public static final String ADMIN = "ADMIN";

  public boolean isAdmin() {
    return ADMIN.equals(role);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return Map.of();
  }

  @Override
  public String getName() {
    return String.valueOf(id);
  }

  @Override
  public String toString() {
    return "AuthUser[id=" + id + ", role=" + role + "]"; // 비밀번호 해시가 로그에 찍히지 않게
  }
}

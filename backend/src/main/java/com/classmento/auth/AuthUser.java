package com.classmento.auth;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 로그인한 사용자(세션에 저장되는 principal). 이메일 로그인(UserDetails)과 소셜 로그인(OAuth2User)을 같은 타입으로 통일해 컨트롤러에서는
 * {@code @AuthenticationPrincipal AuthUser user} 하나로 받는다. 세션 DB에 직렬화되므로 필드는 단순 값만 둔다.
 */
public record AuthUser(Long id, String email, String name, AppUser.Role role, String passwordHash)
    implements UserDetails, OAuth2User {

  public static AuthUser from(AppUser user) {
    return new AuthUser(
        user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getPasswordHash());
  }

  public boolean isAdmin() {
    return role == AppUser.Role.ADMIN;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
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

package com.classmento.identity.infrastructure.security;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;

/** 회원 엔티티 → 세션 principal(공용 커널 {@link AuthUser}) 변환. */
public final class AuthUsers {

  private AuthUsers() {}

  public static AuthUser from(AppUser user) {
    return new AuthUser(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getRole().name(),
        user.getPasswordHash());
  }
}

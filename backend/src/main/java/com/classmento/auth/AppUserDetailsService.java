package com.classmento.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

  private final AppUserRepository users;

  public AppUserDetailsService(AppUserRepository users) {
    this.users = users;
  }

  @Override
  public UserDetails loadUserByUsername(String email) {
    AppUser user =
        users
            .findByEmailIgnoreCase(email)
            .filter(u -> u.getPasswordHash() != null) // 소셜 전용 계정은 비밀번호 로그인 불가
            .orElseThrow(() -> new UsernameNotFoundException("not found"));
    return AuthUser.from(user);
  }
}

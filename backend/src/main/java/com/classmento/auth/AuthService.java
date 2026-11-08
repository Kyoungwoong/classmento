package com.classmento.auth;

import com.classmento.auth.AuthDtos.SignupRequest;
import com.classmento.common.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;

  public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AppUser signup(SignupRequest request) {
    String email = request.email().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email)) {
      throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "이미 가입된 이메일이에요.");
    }
    return users.save(
        AppUser.local(
            email,
            passwordEncoder.encode(request.password()),
            request.name().trim(),
            AppUser.Role.USER));
  }
}

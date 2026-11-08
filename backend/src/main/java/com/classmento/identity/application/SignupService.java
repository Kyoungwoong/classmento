package com.classmento.identity.application;

import com.classmento.common.domain.DomainException;
import com.classmento.identity.domain.AppUser;
import com.classmento.identity.domain.AppUserRepository;
import com.classmento.identity.domain.Email;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이메일 회원가입 유스케이스. */
@Service
public class SignupService {

  private final AppUserRepository users;
  private final PasswordEncoder passwordEncoder;

  public SignupService(AppUserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AppUser signup(SignupCommand command) {
    Email email = Email.of(command.email());
    if (users.existsByEmailIgnoreCase(email.value())) {
      throw new DomainException("EMAIL_TAKEN", "이미 가입된 이메일이에요.");
    }
    return users.save(
        AppUser.local(
            email,
            passwordEncoder.encode(command.password()),
            command.name().trim(),
            AppUser.Role.USER));
  }

  /** 입력 형식 검증은 presentation에서 끝난 값. */
  public record SignupCommand(String email, String password, String name) {}
}

package com.classmento.identity.domain;

import java.util.List;
import java.util.Optional;

/** 회원 저장소 (도메인 포트). 구현: infrastructure.persistence.AppUserJpaRepository (Spring Data). */
public interface AppUserRepository {

  /** Spring Data의 save와 같은 시그니처(구현체가 그대로 만족하도록). */
  <S extends AppUser> S save(S user);

  Optional<AppUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  Optional<AppUser> findByProviderAndProviderId(AppUser.Provider provider, String providerId);

  List<AppUser> findTop200ByOrderByCreatedAtDesc();
}

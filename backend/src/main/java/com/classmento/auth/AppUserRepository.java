package com.classmento.auth;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

  Optional<AppUser> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  Optional<AppUser> findByProviderAndProviderId(AppUser.Provider provider, String providerId);

  List<AppUser> findTop200ByOrderByCreatedAtDesc();
}

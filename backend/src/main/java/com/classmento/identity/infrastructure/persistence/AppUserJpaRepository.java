package com.classmento.identity.infrastructure.persistence;

import com.classmento.identity.domain.AppUser;
import com.classmento.identity.domain.AppUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 도메인 포트 {@link AppUserRepository}의 Spring Data JPA 구현. 메서드 이름 규칙으로 쿼리가 만들어지므로 도메인 인터페이스의 메서드 이름은
 * Spring Data 규칙을 따른다.
 */
public interface AppUserJpaRepository extends JpaRepository<AppUser, Long>, AppUserRepository {}

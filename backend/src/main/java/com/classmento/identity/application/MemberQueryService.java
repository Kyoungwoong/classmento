package com.classmento.identity.application;

import com.classmento.identity.domain.AppUser;
import com.classmento.identity.domain.AppUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 조회 유스케이스 (관리자 화면 등). */
@Service
public class MemberQueryService {

  private final AppUserRepository users;

  public MemberQueryService(AppUserRepository users) {
    this.users = users;
  }

  /** 최근 가입 순 200명. */
  @Transactional(readOnly = true)
  public List<AppUser> recentMembers() {
    return users.findTop200ByOrderByCreatedAtDesc();
  }
}

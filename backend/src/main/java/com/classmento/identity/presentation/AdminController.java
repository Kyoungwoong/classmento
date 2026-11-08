package com.classmento.identity.presentation;

import com.classmento.identity.application.MemberQueryService;
import com.classmento.identity.domain.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 전용 API. /api/admin/** 는 SecurityConfig에서 ROLE_ADMIN만 허용한다. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final MemberQueryService members;

  public AdminController(MemberQueryService members) {
    this.members = members;
  }

  @GetMapping("/users")
  public List<AdminUserView> users() {
    return members.recentMembers().stream().map(AdminUserView::of).toList();
  }

  @Schema(requiredProperties = {"id", "email", "name", "role", "provider", "createdAt"})
  public record AdminUserView(
      Long id,
      String email,
      String name,
      AppUser.Role role,
      AppUser.Provider provider,
      Instant createdAt) {

    static AdminUserView of(AppUser u) {
      return new AdminUserView(
          u.getId(), u.getEmail(), u.getName(), u.getRole(), u.getProvider(), u.getCreatedAt());
    }
  }
}

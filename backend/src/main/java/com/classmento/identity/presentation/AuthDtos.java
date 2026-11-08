package com.classmento.identity.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 인증 API 요청/응답. */
public final class AuthDtos {

  private AuthDtos() {}

  public record SignupRequest(
      @NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 맞지 않아요.") String email,
      @NotBlank(message = "비밀번호를 입력해 주세요.") @Size(min = 8, max = 64, message = "비밀번호는 8자 이상이에요.")
          String password,
      @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 30, message = "이름은 30자까지예요.") String name) {}

  public record LoginRequest(
      @NotBlank(message = "이메일을 입력해 주세요.") String email,
      @NotBlank(message = "비밀번호를 입력해 주세요.") String password) {}

  @Schema(requiredProperties = {"id", "email", "name", "role"})
  public record UserView(Long id, String email, String name, AppUser.Role role) {

    static UserView of(AuthUser user) {
      return new UserView(user.id(), user.email(), user.name(), AppUser.Role.valueOf(user.role()));
    }
  }

  /** 로그인 안 했으면 authenticated=false, user=null (401 대신 200으로 상태를 알려준다). */
  @Schema(requiredProperties = {"authenticated"})
  public record MeResponse(boolean authenticated, UserView user) {}

  /** 키가 설정된 소셜 로그인 목록. 프론트는 /oauth2/authorization/{id} 로 이동시킨다. */
  @Schema(requiredProperties = {"providers"})
  public record ProvidersResponse(List<SocialProvider> providers) {}

  @Schema(requiredProperties = {"id", "name"})
  public record SocialProvider(String id, String name) {}

  @Schema(requiredProperties = {"headerName", "token"})
  public record CsrfResponse(String headerName, String token) {}
}

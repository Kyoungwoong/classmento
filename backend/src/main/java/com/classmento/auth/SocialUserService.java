package com.classmento.auth;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소셜 로그인 성공 후 공급자 사용자 정보를 우리 app_user로 연결(없으면 가입)한다. */
@Service
public class SocialUserService extends DefaultOAuth2UserService {

  private final AppUserRepository users;

  public SocialUserService(AppUserRepository users) {
    this.users = users;
  }

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest request) {
    OAuth2User raw = super.loadUser(request);
    SocialProfile profile =
        SocialProfile.of(request.getClientRegistration().getRegistrationId(), raw);

    AppUser user =
        users
            .findByProviderAndProviderId(profile.provider(), profile.providerId())
            .orElseGet(() -> register(profile));
    return AuthUser.from(user);
  }

  private AppUser register(SocialProfile profile) {
    if (users.existsByEmailIgnoreCase(profile.email())) {
      // 같은 이메일의 다른 가입 방식이 있으면 자동 연결하지 않는다(계정 탈취 방지).
      throw new OAuth2AuthenticationException(
          new OAuth2Error("email_taken"), "이미 다른 방법으로 가입된 이메일이에요.");
    }
    return users.save(
        AppUser.social(profile.email(), profile.name(), profile.provider(), profile.providerId()));
  }

  record SocialProfile(AppUser.Provider provider, String providerId, String email, String name) {

    @SuppressWarnings("unchecked")
    static SocialProfile of(String registrationId, OAuth2User raw) {
      Map<String, Object> a = raw.getAttributes();
      return switch (registrationId) {
        case "google" ->
            new SocialProfile(
                AppUser.Provider.GOOGLE,
                String.valueOf(a.get("sub")),
                String.valueOf(a.get("email")),
                String.valueOf(a.getOrDefault("name", "회원")));
        case "kakao" -> {
          String id = String.valueOf(a.get("id"));
          Map<String, Object> account =
              (Map<String, Object>) a.getOrDefault("kakao_account", Map.of());
          Map<String, Object> profile =
              (Map<String, Object>) account.getOrDefault("profile", Map.of());
          Object email = account.get("email");
          yield new SocialProfile(
              AppUser.Provider.KAKAO,
              id,
              email != null ? email.toString() : "kakao_" + id + "@kakao.local",
              String.valueOf(profile.getOrDefault("nickname", "회원")));
        }
        default -> throw new IllegalArgumentException("지원하지 않는 공급자: " + registrationId);
      };
    }
  }
}

package com.classmento.auth;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/** 소셜 로그인 키. 값이 있는 공급자만 등록된다(키가 없으면 소셜 로그인 버튼도 숨겨진다). */
@ConfigurationProperties("classmento.social")
public record SocialProperties(
    String googleClientId,
    String googleClientSecret,
    String kakaoClientId,
    String kakaoClientSecret) {

  private static final String REDIRECT = "{baseUrl}/login/oauth2/code/{registrationId}";

  public List<ClientRegistration> registrations() {
    List<ClientRegistration> list = new ArrayList<>();
    if (present(googleClientId) && present(googleClientSecret)) {
      list.add(
          CommonOAuth2Provider.GOOGLE
              .getBuilder("google")
              .clientId(googleClientId)
              .clientSecret(googleClientSecret)
              .scope("profile", "email") // openid 제외 → OAuth2 userinfo 방식으로 통일
              .build());
    }
    if (present(kakaoClientId)) {
      list.add(
          ClientRegistration.withRegistrationId("kakao")
              .clientId(kakaoClientId)
              .clientSecret(kakaoClientSecret == null ? "" : kakaoClientSecret)
              .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri(REDIRECT)
              .scope("profile_nickname", "account_email")
              .authorizationUri("https://kauth.kakao.com/oauth/authorize")
              .tokenUri("https://kauth.kakao.com/oauth/token")
              .userInfoUri("https://kapi.kakao.com/v2/user/me")
              .userNameAttributeName("id")
              .clientName("카카오")
              .build());
    }
    return list;
  }

  private static boolean present(String s) {
    return s != null && !s.isBlank();
  }
}

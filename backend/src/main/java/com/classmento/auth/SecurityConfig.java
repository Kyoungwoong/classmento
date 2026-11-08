package com.classmento.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * 인증 구조: 세션 쿠키(HttpOnly, spring-session-jdbc로 DB 저장) + 쿠키 기반 CSRF 토큰. 프론트는 Next.js rewrites로 같은
 * 출처에서 /api 를 호출한다. 상세: docs/adr/0002-postgres-pgvector-auth.md
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityContextRepository contextRepository,
      SocialProperties social,
      SocialUserService socialUserService,
      OAuthRedirectProperties redirects)
      throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/health",
                        "/api/auth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health",
                        "/oauth2/**",
                        "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .csrf(csrf -> csrf.spa())
        .securityContext(ctx -> ctx.securityContextRepository(contextRepository))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(
            logout ->
                logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));

    List<ClientRegistration> registrations = social.registrations();
    if (!registrations.isEmpty()) {
      http.oauth2Login(
          oauth ->
              oauth
                  .clientRegistrationRepository(
                      new InMemoryClientRegistrationRepository(registrations))
                  .userInfoEndpoint(u -> u.userService(socialUserService))
                  .defaultSuccessUrl(redirects.successUrl(), true)
                  .failureUrl(redirects.failureUrl()));
    }
    return http.build();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(
      AppUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  @ConfigurationProperties("classmento.oauth")
  public record OAuthRedirectProperties(String successUrl, String failureUrl) {}
}

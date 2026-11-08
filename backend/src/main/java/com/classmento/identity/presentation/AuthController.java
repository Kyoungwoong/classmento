package com.classmento.identity.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.application.SignupService;
import com.classmento.identity.application.SignupService.SignupCommand;
import com.classmento.identity.infrastructure.security.SocialProperties;
import com.classmento.identity.presentation.AuthDtos.CsrfResponse;
import com.classmento.identity.presentation.AuthDtos.LoginRequest;
import com.classmento.identity.presentation.AuthDtos.MeResponse;
import com.classmento.identity.presentation.AuthDtos.ProvidersResponse;
import com.classmento.identity.presentation.AuthDtos.SignupRequest;
import com.classmento.identity.presentation.AuthDtos.SocialProvider;
import com.classmento.identity.presentation.AuthDtos.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final SignupService signupService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository contextRepository;
  private final SocialProperties social;

  public AuthController(
      SignupService signupService,
      AuthenticationManager authenticationManager,
      SecurityContextRepository contextRepository,
      SocialProperties social) {
    this.signupService = signupService;
    this.authenticationManager = authenticationManager;
    this.contextRepository = contextRepository;
    this.social = social;
  }

  /** 프론트가 첫 POST 전에 호출해 XSRF-TOKEN 쿠키를 받는다. */
  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getHeaderName(), token.getToken());
  }

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public UserView signup(
      @Valid @RequestBody SignupRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    signupService.signup(new SignupCommand(body.email(), body.password(), body.name()));
    return login(new LoginRequest(body.email(), body.password()), request, response);
  }

  @PostMapping("/login")
  public UserView login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    Authentication auth =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                body.email().trim(), body.password()));
    // 세션 고정 공격 방지: 로그인 시 세션 ID 교체
    if (request.getSession(false) != null) {
      request.changeSessionId();
    }
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(auth);
    SecurityContextHolder.setContext(context);
    contextRepository.saveContext(context, request, response);
    return UserView.of((AuthUser) auth.getPrincipal());
  }

  @GetMapping("/me")
  public MeResponse me(@AuthenticationPrincipal AuthUser user) {
    return user == null ? new MeResponse(false, null) : new MeResponse(true, UserView.of(user));
  }

  @GetMapping("/providers")
  public ProvidersResponse providers() {
    return new ProvidersResponse(
        social.registrations().stream()
            .map(r -> new SocialProvider(r.getRegistrationId(), r.getClientName()))
            .toList());
  }
}

package com.classmento.identity.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.core.serializer.support.SerializingConverter;

/**
 * 세션 DB(spring-session-jdbc) 속성 직렬화. 기본 동작은 역직렬화에 실패하면 예외 → 그 쿠키를 가진 사용자의 모든 요청이 500이 된다. 클래스
 * 이름·구조가 바뀌는 배포(예: principal 패키지 이동) 뒤에도 사용자가 다시 로그인할 수 있도록, 읽을 수 없는 속성은 "없음"(null)으로 취급한다. 결과적으로 그
 * 사용자는 로그아웃 상태가 된다.
 *
 * <p>Spring Session은 이름이 {@code springSessionConversionService}인 빈을 찾아 사용한다.
 */
@Configuration
public class SessionSerializationConfig {

  private static final Logger log = LoggerFactory.getLogger(SessionSerializationConfig.class);

  @Bean("springSessionConversionService")
  ConversionService springSessionConversionService() {
    GenericConversionService conversion = new GenericConversionService();
    conversion.addConverter(Object.class, byte[].class, new SerializingConverter());
    conversion.addConverter(
        byte[].class,
        Object.class,
        new LenientDeserializer(new DeserializingConverter(getClass().getClassLoader())));
    return conversion;
  }

  /** 역직렬화 실패 시 null(속성 없음)을 돌려준다. */
  static final class LenientDeserializer implements Converter<byte[], Object> {

    private final DeserializingConverter delegate;

    LenientDeserializer(DeserializingConverter delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object convert(byte[] source) {
      try {
        return delegate.convert(source);
      } catch (SerializationFailedException e) {
        log.warn("읽을 수 없는 세션 속성을 무시합니다(로그아웃 상태로 처리): {}", rootMessage(e));
        return null;
      }
    }

    private static String rootMessage(Throwable e) {
      Throwable t = e;
      while (t.getCause() != null) {
        t = t.getCause();
      }
      return t.toString();
    }
  }
}

package com.classmento.common.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("classmento.cors")
public record CorsProperties(List<String> allowedOrigins) {}

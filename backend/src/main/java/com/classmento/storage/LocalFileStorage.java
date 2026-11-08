package com.classmento.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** {@code classmento.storage.root} 아래에 파일을 저장한다. 원래 파일명은 DB에만 두고 디스크에는 UUID 이름으로 저장(경로 조작 방지). */
@Component
public class LocalFileStorage implements FileStorage {

  private final Path root;

  public LocalFileStorage(StorageProperties properties) {
    this.root = Path.of(properties.root()).toAbsolutePath().normalize();
  }

  @Override
  public String store(String directory, String filename, byte[] content) {
    String ext = extension(filename);
    String key = directory + "/" + UUID.randomUUID() + ext;
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      Files.write(target, content);
      return key;
    } catch (IOException e) {
      throw new UncheckedIOException("파일 저장 실패: " + key, e);
    }
  }

  @Override
  public byte[] load(String key) {
    try {
      return Files.readAllBytes(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("파일 읽기 실패: " + key, e);
    }
  }

  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("잘못된 저장 키: " + key);
    }
    return path;
  }

  private static String extension(String filename) {
    int dot = filename == null ? -1 : filename.lastIndexOf('.');
    if (dot < 0) {
      return "";
    }
    String ext = filename.substring(dot).toLowerCase();
    return ext.matches("\\.[a-z0-9]{1,8}") ? ext : "";
  }

  @ConfigurationProperties("classmento.storage")
  public record StorageProperties(String root) {}
}

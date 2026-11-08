package com.classmento.resume.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.ParsedResume;
import com.classmento.resume.domain.ParsedResume.Project;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 같은 이력서에 결과 수정이 동시에 오면, 뒤에 온 요청은 이력서 행 잠금에서 먼저 기다려야 한다. 경험을 지우고 다시 넣기(DELETE/INSERT)를 시작한 뒤에 기다리면
 * unique 충돌로 500이 나거나 parsed_json과 경험이 서로 다른 요청 내용이 된다.
 *
 * <p>결정적으로 확인하는 법: 테스트가 이력서 행을 FOR UPDATE로 잡아 둔 동안 다른 스레드가 updateParsed를 호출 → 그 스레드가 대기 상태가 될 때까지
 * 기다린 뒤 resume_experience 테이블에 쓰기 잠금(RowExclusiveLock)을 이미 잡고 있는지 pg_locks로 본다.
 */
class ResumeUpdateConcurrencyTest extends IntegrationTest {

  @Autowired ResumeService service;
  @Autowired ResumeRepository resumes;
  @Autowired TransactionTemplate tx;

  private Long parsedResume(Long userId) {
    Resume r = new Resume(userId, "cv.pdf", "application/pdf", 10, "k");
    r.markExtracted("본문");
    r.markParsed("{}", "m", "v");
    return resumes.save(r).getId();
  }

  private static ParsedResume edited() {
    return new ParsedResume(
        null,
        List.of(),
        List.of(),
        List.of(new Project("새 프로젝트", null, "요약", "설명", List.of())),
        null);
  }

  @Test
  void 행_잠금을_기다리는_동안에는_경험_테이블을_건드리지_않는다() throws Exception {
    Long userId = createUser("me@test.com", AppUser.Role.USER).id();
    Long resumeId = parsedResume(userId);

    CountDownLatch locked = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    CompletableFuture<Void> holder =
        CompletableFuture.runAsync(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      jdbc.queryForObject(
                          "SELECT id FROM resume WHERE id = ? FOR UPDATE", Long.class, resumeId);
                      locked.countDown();
                      await(release);
                    }));
    assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();

    CompletableFuture<?> writer =
        CompletableFuture.runAsync(() -> service.updateParsed(userId, resumeId, edited()));
    try {
      waitUntilSomeoneIsBlocked();
      Integer experienceWriteLocks =
          jdbc.queryForObject(
              """
              SELECT count(*) FROM pg_locks l JOIN pg_class c ON l.relation = c.oid
              WHERE c.relname = 'resume_experience' AND l.mode = 'RowExclusiveLock'
                AND l.pid <> pg_backend_pid()
              """,
              Integer.class);
      assertThat(experienceWriteLocks).as("잠금 대기 중 경험 테이블 쓰기 잠금").isZero();
    } finally {
      release.countDown();
      holder.get(10, TimeUnit.SECONDS);
      writer.get(10, TimeUnit.SECONDS);
    }
  }

  private void waitUntilSomeoneIsBlocked() throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      Integer waiting =
          jdbc.queryForObject("SELECT count(*) FROM pg_locks WHERE NOT granted", Integer.class);
      if (waiting != null && waiting > 0) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("updateParsed가 잠금 대기에 들어가지 않았다");
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(15, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

package com.classmento.questionbank.application;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository.Missing;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 기동 시 임베딩이 비어 있는 질문(시드, 키 없이 저장한 질문)을 채운다. 실패해도 기동은 막지 않는다. */
@Component
public class QuestionEmbeddingBackfill implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(QuestionEmbeddingBackfill.class);
  private static final int BATCH = 50;

  private final QuestionEmbeddingRepository embeddings;
  private final EmbeddingClient embeddingClient;

  public QuestionEmbeddingBackfill(
      QuestionEmbeddingRepository embeddings, EmbeddingClient embeddingClient) {
    this.embeddings = embeddings;
    this.embeddingClient = embeddingClient;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      int filled = fillMissing();
      if (filled > 0) {
        log.info("질문 임베딩 {}개를 채웠어요.", filled);
      }
    } catch (RuntimeException e) {
      log.warn("질문 임베딩 채우기 실패(다음 기동 때 다시 시도): {}", e.getMessage());
    }
  }

  /** 채운 개수를 돌려준다. 임베딩을 쓸 수 없으면 0. */
  public int fillMissing() {
    if (!embeddingClient.isAvailable()) {
      return 0;
    }
    int total = 0;
    while (true) {
      List<Missing> batch = embeddings.findMissing(BATCH);
      if (batch.isEmpty()) {
        return total;
      }
      List<float[]> vectors =
          embeddingClient.embedAll(batch.stream().map(Missing::embeddingText).toList());
      for (int i = 0; i < batch.size(); i++) {
        embeddings.update(batch.get(i).questionId(), vectors.get(i));
      }
      total += batch.size();
      if (batch.size() < BATCH) {
        return total;
      }
    }
  }
}

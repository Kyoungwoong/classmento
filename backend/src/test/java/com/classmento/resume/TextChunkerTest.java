package com.classmento.resume;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

  @Test
  void keepsShortTextInOneChunk() {
    assertThat(new TextChunker(800, 100).chunk("첫 문단\n\n둘째 문단")).containsExactly("첫 문단\n\n둘째 문단");
  }

  @Test
  void splitsLongTextWithinLimitAndOverlaps() {
    String para = "가".repeat(300);
    String text = String.join("\n\n", para, para, para, para);

    List<String> chunks = new TextChunker(800, 100).chunk(text);

    assertThat(chunks).hasSizeGreaterThan(1);
    assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(800 + 100));
    // 다음 청크는 앞 청크의 끝 100자로 시작
    String prevTail = chunks.get(0).substring(chunks.get(0).length() - 100);
    assertThat(chunks.get(1)).startsWith(prevTail);
  }

  @Test
  void splitsSingleHugeParagraph() {
    List<String> chunks = new TextChunker(800, 100).chunk("나".repeat(2000));
    assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
  }
}

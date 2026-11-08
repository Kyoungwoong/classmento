package com.classmento.resume.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 긴 텍스트를 검색 단위(청크)로 자른다. 문단(빈 줄) 경계를 우선 지키며 maxChars를 넘지 않게 묶고, 문맥이 끊기지 않도록 앞 청크의 끝 overlap 글자를 다음
 * 청크 앞에 붙인다.
 */
public class TextChunker {

  private final int maxChars;
  private final int overlap;

  public TextChunker(int maxChars, int overlap) {
    if (overlap >= maxChars) {
      throw new IllegalArgumentException("overlap must be < maxChars");
    }
    this.maxChars = maxChars;
    this.overlap = overlap;
  }

  public List<String> chunk(String text) {
    List<String> pieces = new ArrayList<>();
    for (String para : text.split("\\n\\s*\\n")) {
      String p = para.strip();
      if (p.isEmpty()) {
        continue;
      }
      // 문단 자체가 너무 길면 강제로 자른다
      for (int i = 0; i < p.length(); i += maxChars - overlap) {
        pieces.add(p.substring(i, Math.min(p.length(), i + maxChars)));
        if (i + maxChars >= p.length()) {
          break;
        }
      }
    }

    List<String> chunks = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    for (String piece : pieces) {
      if (current.length() > 0 && current.length() + 2 + piece.length() > maxChars) {
        String done = current.toString();
        chunks.add(done);
        current = new StringBuilder(tail(done));
      }
      if (current.length() > 0) {
        current.append("\n\n");
      }
      current.append(piece);
    }
    if (current.length() > 0) {
      chunks.add(current.toString());
    }
    return chunks;
  }

  private String tail(String s) {
    return s.length() <= overlap ? s : s.substring(s.length() - overlap);
  }
}

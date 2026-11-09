package com.classmento.embedding;

/** float[] ↔ pgvector 텍스트 표현("[0.1,0.2,...]"). SQL에서는 {@code ?::vector}로 캐스팅해 쓴다. */
public final class PgVectors {

  private PgVectors() {}

  /** "[0.1,0.2,...]" → float[] (pgvector의 텍스트 표현, SQL에서 {@code embedding::text}로 읽는다). */
  public static float[] parse(String literal) {
    String body = literal.strip();
    body = body.substring(1, body.length() - 1);
    if (body.isBlank()) {
      return new float[0];
    }
    String[] parts = body.split(",");
    float[] out = new float[parts.length];
    for (int i = 0; i < parts.length; i++) {
      out[i] = Float.parseFloat(parts[i].strip());
    }
    return out;
  }

  public static String toLiteral(float[] vector) {
    StringBuilder sb = new StringBuilder(vector.length * 10).append('[');
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append(vector[i]);
    }
    return sb.append(']').toString();
  }
}

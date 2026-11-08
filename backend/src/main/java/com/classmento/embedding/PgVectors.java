package com.classmento.embedding;

/** float[] ↔ pgvector 텍스트 표현("[0.1,0.2,...]"). SQL에서는 {@code ?::vector}로 캐스팅해 쓴다. */
public final class PgVectors {

  private PgVectors() {}

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

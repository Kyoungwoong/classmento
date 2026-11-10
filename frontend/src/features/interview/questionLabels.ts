/** 문항 번호 표시: 본 질문은 1, 2, 3…(사용자가 고른 순서), 꼬리는 "질문 N의 꼬리 d". seq는 고정 식별자라 화면 번호와 다르다(ADR 0008). */
export interface ChainItem {
  seq: number;
  parentSeq?: number | null;
  depth: number;
}

/** seq → 그 사슬의 본 질문 번호(1부터). items는 화면 순서(본 질문 → 그 꼬리들). */
export function rootNumbers(items: readonly ChainItem[]): Map<number, number> {
  const out = new Map<number, number>();
  let n = 0;
  for (const it of items) {
    if (it.depth === 0) n += 1;
    out.set(it.seq, Math.max(n, 1));
  }
  return out;
}

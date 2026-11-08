export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes}B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)}KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}

export const MAX_RESUME_BYTES = 20 * 1024 * 1024;
export const ACCEPTED_RESUME = ".pdf,.docx";

/** 서버와 같은 기준(확장자)으로 먼저 걸러 사용자에게 바로 알려준다. 최종 검사는 서버가 한다. */
export function validateResumeFile(file: File): string | null {
  const name = file.name.toLowerCase();
  if (!name.endsWith(".pdf") && !name.endsWith(".docx")) {
    return "PDF나 DOCX 파일만 올릴 수 있어요.";
  }
  if (file.size > MAX_RESUME_BYTES) {
    return "파일이 너무 커요. 20MB 이하로 올려주세요.";
  }
  return null;
}

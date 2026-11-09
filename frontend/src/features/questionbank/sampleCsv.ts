/** 예시 CSV. 형식 규칙은 docs/api/README.md "질문 CSV 형식"과 같다(규칙을 바꾸면 이 예시와 문서를 함께). */
export const SAMPLE_CSV = [
  "대분류,소분류,질문,난이도,직무,태그,의도",
  "CS,네트워크,TCP와 UDP의 차이를 설명하고 각각 어디에 쓰이는지 말해 주세요.,신입,백엔드|공통,TCP|UDP,기본기 확인",
  'Framework,Spring,"JPA에서 N+1 문제가 생기는 이유와, 해결 방법을 설명해 주세요.",주니어,백엔드,JPA,',
  "인성,협업,다른 직군과 협업하면서 가장 기억에 남는 경험을 말해 주세요.,신입,공통,,",
].join("\n");

/** 엑셀이 한글을 깨뜨리지 않도록 BOM을 붙여 내려받게 한다. */
const BOM = String.fromCharCode(0xfeff);

export function downloadSampleCsv() {
  const blob = new Blob([BOM + SAMPLE_CSV + "\n"], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "질문은행-예시.csv";
  a.click();
  // 다운로드가 시작되기 전에 URL이 사라지지 않게 한 틱 늦춘다(일부 브라우저)
  setTimeout(() => URL.revokeObjectURL(url), 0);
}

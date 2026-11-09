"use client";

import { useParams } from "next/navigation";
import { QuestionFormScreen } from "./QuestionFormScreen";

/** /admin/questions/[id] — URL의 id로 수정 화면을 연다 */
export function QuestionEditRoute() {
  const { id } = useParams<{ id: string }>();
  return <QuestionFormScreen id={Number(id)} />;
}

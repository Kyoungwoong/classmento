"use client";

import { useParams } from "next/navigation";
import { InterviewScreen } from "./InterviewScreen";

/** /interview/[id] — URL의 id로 진행 화면을 연다 */
export function InterviewRoute() {
  const { id } = useParams<{ id: string }>();
  return <InterviewScreen id={Number(id)} />;
}

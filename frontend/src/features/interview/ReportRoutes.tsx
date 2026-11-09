"use client";

import { useParams } from "next/navigation";
import { ReportItemScreen } from "./ReportItemScreen";
import { ReportScreen } from "./ReportScreen";

/** /interview/[id]/report — URL의 id로 리포트를 연다 */
export function ReportRoute() {
  const { id } = useParams<{ id: string }>();
  return <ReportScreen id={Number(id)} />;
}

/** /interview/[id]/report/[seq] — 문항 하나 */
export function ReportItemRoute() {
  const { id, seq } = useParams<{ id: string; seq: string }>();
  return <ReportItemScreen id={Number(id)} seq={Number(seq)} />;
}

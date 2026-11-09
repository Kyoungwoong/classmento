"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  BottomCTA,
  ButtonLink,
  ListRow,
  Screen,
  ScoreBar,
  ScreenTitle,
  TopBar,
} from "@/components/ui";
import { api, ApiError, type ReportItemView, type ReportView } from "@/lib/api/client";
import { criterionLabel } from "./labels";

const POLL_MS = 3000;

function itemDescription(it: ReportItemView): string {
  switch (it.answerStatus) {
    case "EVALUATED":
      return `${it.overall?.toFixed(1)}점`;
    case "FAILED":
      return it.failureReason ?? "평가하지 못했어요.";
    case undefined:
      return "답하지 않았어요";
    default:
      return "평가하고 있어요";
  }
}

/**
 * /interview/[id]/report — 종합 점수(큰 숫자)·강점·보완점·기준별 점수·문항별 점수.
 * 평가 중이면 3초마다 다시 불러온다(종합이 빠졌을 때 서버가 다시 만드는 계기이기도 하다).
 */
export function ReportScreen({ id }: { id: number }) {
  const router = useRouter();
  const [report, setReport] = useState<ReportView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [retrying, setRetrying] = useState<number | null>(null);
  const [tick, setTick] = useState(0);

  const load = useCallback(
    () =>
      api
        .interviewReport(id)
        .then((r) => {
          setReport(r);
          setError(null);
        })
        .catch((err) => {
          if (err instanceof ApiError && err.status === 401) {
            router.replace(`/login?next=/interview/${id}/report`);
            return;
          }
          setError(
            err instanceof ApiError && err.status === 404
              ? "리포트를 찾을 수 없어요."
              : "리포트를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
          );
        }),
    [id, router],
  );

  useEffect(() => {
    load();
  }, [load, tick]);

  // 평가 중이면 다시 불러오기
  useEffect(() => {
    if (report?.status !== "EVALUATING") return;
    const t = setTimeout(() => setTick((n) => n + 1), POLL_MS);
    return () => clearTimeout(t);
  }, [report]);

  async function retry(seq: number) {
    setRetrying(seq);
    try {
      await api.retryAnswer(id, seq);
    } finally {
      setRetrying(null);
      setTick((n) => n + 1);
    }
  }

  const cta = (
    <BottomCTA>
      <ButtonLink href="/interview/new">다시 연습하기</ButtonLink>
    </BottomCTA>
  );

  if (!report) {
    return (
      <Screen header={<TopBar onBack={() => router.push("/")} />} cta={cta}>
        <ScreenTitle title={error ?? "리포트를 불러오고 있어요"} />
      </Screen>
    );
  }

  const ready = report.status === "READY";

  return (
    <Screen header={<TopBar onBack={() => router.push("/")} />} cta={cta}>
      <ScreenTitle
        title={ready ? "면접이 끝났어요" : "답변을 평가하고 있어요"}
        description={ready ? undefined : "끝나면 이 화면에 바로 보여 드려요."}
      />

      {ready && report.overall !== undefined && (
        <section aria-label="종합 점수">
          <p className="flex items-baseline gap-1">
            <span className="text-title1 text-text-strong tabular-nums">
              {report.overall.toFixed(1)}
            </span>
            <span className="text-body2 text-text-sub">/ 5</span>
          </p>
          <dl className="mt-6 flex flex-col gap-4">
            <div>
              <dt className="text-caption text-text-sub">잘한 점</dt>
              <dd className="mt-1 text-body1 text-text">{report.strength}</dd>
            </div>
            <div>
              <dt className="text-caption text-text-sub">보완할 점</dt>
              <dd className="mt-1 text-body1 text-text">{report.improvement}</dd>
            </div>
          </dl>
        </section>
      )}

      {report.criteria.length > 0 && (
        <section className="mt-10">
          <h2 className="text-body1 font-semibold text-text-strong">기준별 점수</h2>
          <div className="mt-4 flex flex-col gap-4">
            {report.criteria.map((c) => (
              <ScoreBar key={c.criterion} label={criterionLabel(c.criterion)} score={c.average} />
            ))}
          </div>
        </section>
      )}

      <section className="mt-10">
        <h2 className="text-body1 font-semibold text-text-strong">문항별</h2>
        <ul className="mt-2">
          {report.items.map((it) => (
            <li key={it.seq}>
              <ListRow
                title={`${it.seq}. ${it.question}`}
                titleLines={2}
                description={itemDescription(it)}
                onClick={
                  it.answerStatus
                    ? () => router.push(`/interview/${id}/report/${it.seq}`)
                    : undefined
                }
              />
              {it.answerStatus === "FAILED" && (
                <button
                  type="button"
                  aria-label={`${it.seq}번 다시 평가하기`}
                  disabled={retrying === it.seq}
                  onClick={() => retry(it.seq)}
                  className="-mt-2 inline-flex h-11 items-center text-caption text-primary disabled:text-text-hint"
                >
                  {retrying === it.seq ? "다시 평가하고 있어요" : "다시 평가하기"}
                </button>
              )}
            </li>
          ))}
        </ul>
      </section>
    </Screen>
  );
}

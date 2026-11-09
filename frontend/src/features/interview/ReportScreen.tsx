"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
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

/** 서버는 빈 값을 null로 보낸다(TS 타입은 optional) — null·undefined를 함께 본다 */
function itemStatus(it: ReportItemView): string | undefined {
  if (it.answerStatus == null) return "답하지 않았어요";
  switch (it.answerStatus) {
    case "EVALUATED":
      return undefined; // 점수는 오른쪽 숫자로
    case "FAILED":
      return it.failureReason ?? "평가하지 못했어요.";
    default:
      return "평가하고 있어요";
  }
}

/**
 * 다시 불러올 가치가 있을 때만 폴링한다: 평가 중인 답변이 있거나, 모두 평가됐는데 종합만 아직일 때.
 * (답하지 않은 문항·다시 평가할 문항만 남았으면 기다려도 바뀌지 않는다)
 */
function shouldPoll(r: ReportView): boolean {
  if (r.status !== "EVALUATING") return false;
  const inFlight = r.items.some(
    (it) => it.answerStatus === "UPLOADED" || it.answerStatus === "TRANSCRIBED",
  );
  const allEvaluated = r.items.every((it) => it.answerStatus === "EVALUATED");
  return inFlight || allEvaluated;
}

/**
 * /interview/[id]/report — 종합 점수(큰 숫자)·강점·보완점·기준별 점수·문항별 점수.
 * 평가 중이면 3초마다 다시 불러온다(종합이 빠졌을 때 서버가 다시 만드는 계기이기도 하다).
 */
export function ReportScreen({ id }: { id: number }) {
  const router = useRouter();
  const [report, setReport] = useState<ReportView | null>(null);
  const [error, setError] = useState<{ message: string; notFound: boolean } | null>(null);
  const [retrying, setRetrying] = useState<number | null>(null);
  const [retryFailed, setRetryFailed] = useState<number | null>(null);
  const [tick, setTick] = useState(0);
  const invalidId = !Number.isInteger(id) || id <= 0;

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
          const notFound = err instanceof ApiError && err.status === 404;
          setError({
            notFound,
            message: notFound
              ? "리포트를 찾을 수 없어요."
              : "리포트를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
          });
        }),
    [id, router],
  );

  useEffect(() => {
    if (invalidId) return;
    load();
  }, [load, tick, invalidId]);

  // 평가 중이면 다시 불러오기(불러오기가 실패하면 멈추고 안내한다)
  useEffect(() => {
    if (!report || !shouldPoll(report) || error) return;
    const t = setTimeout(() => setTick((n) => n + 1), POLL_MS);
    return () => clearTimeout(t);
  }, [report, error]);

  const reload = () => {
    setError(null);
    setTick((n) => n + 1);
  };

  async function retry(seq: number) {
    setRetrying(seq);
    setRetryFailed(null);
    try {
      await api.retryAnswer(id, seq);
      setTick((n) => n + 1);
    } catch {
      setRetryFailed(seq);
    } finally {
      setRetrying(null);
    }
  }

  const header = <TopBar onBack={() => router.push("/")} />;
  const practice = (
    <BottomCTA>
      <ButtonLink href="/interview/new">다시 연습하기</ButtonLink>
    </BottomCTA>
  );

  if (invalidId || !report) {
    const shown = invalidId ? { message: "리포트를 찾을 수 없어요.", notFound: true } : error;
    const failed = shown && !shown.notFound;
    return (
      <Screen
        header={header}
        cta={
          failed ? (
            <BottomCTA>
              <Button onClick={reload}>다시 불러오기</Button>
            </BottomCTA>
          ) : (
            practice
          )
        }
      >
        <div role={shown ? "alert" : undefined}>
          <ScreenTitle title={shown?.message ?? "리포트를 불러오고 있어요"} />
        </div>
      </Screen>
    );
  }

  const ready = report.status === "READY";

  return (
    <Screen header={header} cta={practice}>
      <div aria-live="polite">
        <ScreenTitle
          title={ready ? "면접이 끝났어요" : "답변을 평가하고 있어요"}
          description={ready ? undefined : "끝나면 이 화면에 바로 보여 드려요."}
        />
      </div>

      {error && (
        <div role="alert" className="-mt-4 mb-6">
          <p className="text-body2 text-danger">
            평가 결과를 불러오지 못했어요. 연결을 확인하고 다시 불러와 주세요.
          </p>
          <button
            type="button"
            onClick={reload}
            className="inline-flex h-11 items-center text-body2 text-accent"
          >
            다시 불러오기
          </button>
        </div>
      )}

      {ready && report.overall != null && (
        <section aria-label="종합 점수">
          <p className="flex items-baseline gap-1">
            <span className="text-display text-text-strong tabular-nums">
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
              <ScoreBar
                key={c.criterion}
                label={criterionLabel(c.criterion)}
                score={c.average}
                decimals={1}
              />
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
                description={itemStatus(it)}
                trailing={
                  it.answerStatus === "EVALUATED" && it.overall != null ? (
                    <span className="text-body1 font-semibold text-text-strong tabular-nums">
                      {`${it.overall.toFixed(1)}점`}
                    </span>
                  ) : undefined
                }
                onClick={
                  it.answerStatus
                    ? () => router.push(`/interview/${id}/report/${it.seq}`)
                    : undefined
                }
              />
              {it.answerStatus === "FAILED" && (
                <>
                  <button
                    type="button"
                    aria-label={`${it.seq}번 다시 평가하기`}
                    disabled={retrying === it.seq}
                    onClick={() => retry(it.seq)}
                    className="-mt-2 inline-flex h-11 items-center text-caption text-accent disabled:text-text-hint"
                  >
                    {retrying === it.seq ? "다시 평가하고 있어요" : "다시 평가하기"}
                  </button>
                  {retryFailed === it.seq && (
                    <p role="alert" className="text-caption text-danger">
                      다시 평가를 시작하지 못했어요. 잠시 후 다시 눌러 주세요.
                    </p>
                  )}
                </>
              )}
            </li>
          ))}
        </ul>
      </section>
    </Screen>
  );
}

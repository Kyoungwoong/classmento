"use client";

import { CornerLeftUp } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  Panel,
  Screen,
  ScoreBar,
  ScreenTitle,
  TopBar,
} from "@/components/ui";
import { NoteSheet, StickyNote } from "@/components/note";
import { Mascot } from "@/components/site/Mascot";
import { api, ApiError, type ReportItemView } from "@/lib/api/client";
import { criterionLabel } from "./labels";
import { rootNumbers } from "./questionLabels";
import { markEvidence } from "./markEvidence";

/**
 * /interview/[id]/report/[seq] — 문항 하나: 녹음 다시 듣기, 내 답변(STT 글자), 기준별 점수와 근거, 잘한 점·고칠 점·이렇게 답해 보세요.
 */
export function ReportItemScreen({ id, seq }: { id: number; seq: number }) {
  const router = useRouter();
  const [item, setItem] = useState<ReportItemView | null | undefined>(undefined);
  const [rootNumber, setRootNumber] = useState(seq);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const invalid = !Number.isInteger(id) || id <= 0 || !Number.isInteger(seq) || seq <= 0;

  useEffect(() => {
    if (invalid) return;
    api
      .interviewReport(id)
      .then((r) => {
        setRootNumber(rootNumbers(r.items).get(seq) ?? seq);
        setItem(r.items.find((it) => it.seq === seq) ?? null);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace(`/login?next=/interview/${id}/report/${seq}`);
          return;
        }
        setError("답변을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.");
      });
  }, [id, seq, router, attempt, invalid]);

  const back = () => router.push(`/interview/${id}/report`);
  const cta = (
    <BottomCTA>
      <ButtonLink href={`/interview/${id}/report`}>목록으로 가기</ButtonLink>
    </BottomCTA>
  );

  if (invalid || item === undefined || item === null) {
    const title = error ?? (invalid || item === null ? "문항을 찾을 수 없어요" : null);
    return (
      <Screen
        header={<TopBar onBack={back} />}
        cta={
          error ? (
            <BottomCTA>
              <Button
                onClick={() => {
                  setError(null);
                  setAttempt((n) => n + 1);
                }}
              >
                다시 불러오기
              </Button>
            </BottomCTA>
          ) : (
            cta
          )
        }
      >
        <Mascot mood={title ? "oops" : "thinking"} size={96} className="mb-6" />
        <div role={title ? "alert" : undefined}>
          <ScreenTitle title={title ?? "답변을 불러오고 있어요"} />
        </div>
      </Screen>
    );
  }

  // 왼쪽: 질문 → 내 답변 노트(줄 친 종이, 채점 근거는 형광펜)
  const aside = (
    <div>
      <ScreenTitle
        eyebrow={
          item.depth > 0 ? `질문 ${rootNumber}의 꼬리 ${item.depth}` : `${rootNumber}번 질문`
        }
        title={item.question}
        description={item.intent ? `이 질문으로 보려는 것: ${item.intent}` : undefined}
      />
      {item.parentSeq != null && (
        <Link
          href={`/interview/${id}/report/${item.parentSeq}`}
          className="-mt-4 mb-6 inline-flex h-11 items-center gap-1 text-body2 font-semibold text-accent"
        >
          <CornerLeftUp size={20} aria-hidden />앞 질문 보기
        </Link>
      )}
      {item.answerStatus && (
        <NoteSheet role="region" aria-labelledby="answer-heading">
          <h2 id="answer-heading" className="text-body1 font-semibold text-text-strong">
            내 답변
          </h2>
          {/* 녹음은 본인만 받을 수 있다(세션 쿠키) */}
          <audio
            controls
            preload="none"
            src={api.answerAudioUrl(id, seq)}
            className="mt-3 w-full"
          />
          {item.transcript && (
            <p className="mt-5 text-body1 leading-loose whitespace-pre-line text-text underline decoration-rule decoration-1 underline-offset-8">
              {markEvidence(
                item.transcript,
                item.scores.map((s) => s.evidence),
              )}
            </p>
          )}
        </NoteSheet>
      )}
    </div>
  );

  const evaluated = item.answerStatus === "EVALUATED";

  return (
    <Screen header={<TopBar onBack={back} />} aside={aside} cta={cta}>
      {/* 오른쪽: 채점 — 점수(큰 숫자) · 근거 · 피드백 · 포스트잇 예시 */}
      <div className="flex flex-col gap-6">
        {item.scores.length > 0 && (
          <Panel>
            <div className="flex items-baseline justify-between gap-4">
              <h2 className="text-body1 font-semibold text-text-strong">기준별 점수</h2>
              {evaluated && item.overall != null && (
                <p className="flex items-baseline gap-1">
                  <span aria-hidden="true" className="text-display text-text-strong tabular-nums">
                    {item.overall.toFixed(1)}
                  </span>
                  <span aria-hidden="true" className="text-body2 text-text-sub">
                    / 5
                  </span>
                  <span className="sr-only">{`이 문항 ${item.overall.toFixed(1)}점, 5점 만점`}</span>
                </p>
              )}
            </div>
            <ul className="mt-5 flex flex-col gap-5">
              {item.scores.map((s) => (
                <li key={s.criterion}>
                  <ScoreBar label={criterionLabel(s.criterion)} score={s.score} />
                  {s.evidence && (
                    <p className="mt-2 text-caption text-text-sub">{`“${s.evidence}”`}</p>
                  )}
                </li>
              ))}
            </ul>
          </Panel>
        )}

        {evaluated ? (
          <>
            <Panel>
              <dl className="flex flex-col gap-5">
                <div>
                  <dt className="text-caption font-semibold text-text-strong">잘한 점</dt>
                  <dd className="mt-1 text-body1 text-text">{item.strength}</dd>
                </div>
                <div>
                  <dt className="text-caption font-semibold text-text-strong">고칠 점</dt>
                  <dd className="mt-1 text-body1 text-text">{item.improvement}</dd>
                </div>
              </dl>
            </Panel>
            {/* 다음에 바로 써먹을 예시 하나만 포스트잇으로 */}
            <StickyNote title="이렇게 답해 보세요">{item.suggestion}</StickyNote>
          </>
        ) : (
          <Panel>
            <p className="text-body2 text-text-sub">
              {item.answerStatus == null
                ? "답하지 않았어요."
                : item.answerStatus === "FAILED"
                  ? (item.failureReason ?? "평가하지 못했어요.")
                  : "평가하고 있어요. 끝나면 점수와 피드백이 보여요."}
            </p>
          </Panel>
        )}
      </div>
    </Screen>
  );
}

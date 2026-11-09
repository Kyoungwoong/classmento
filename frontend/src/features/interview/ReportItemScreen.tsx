"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  Screen,
  ScoreBar,
  ScreenTitle,
  TopBar,
} from "@/components/ui";
import { api, ApiError, type ReportItemView } from "@/lib/api/client";
import { criterionLabel } from "./labels";

/**
 * /interview/[id]/report/[seq] — 문항 하나: 녹음 다시 듣기, 내 답변(STT 글자), 기준별 점수와 근거, 잘한 점·고칠 점·이렇게 답해 보세요.
 */
export function ReportItemScreen({ id, seq }: { id: number; seq: number }) {
  const router = useRouter();
  const [item, setItem] = useState<ReportItemView | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const invalid = !Number.isInteger(id) || id <= 0 || !Number.isInteger(seq) || seq <= 0;

  useEffect(() => {
    if (invalid) return;
    api
      .interviewReport(id)
      .then((r) => setItem(r.items.find((it) => it.seq === seq) ?? null))
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
        <div role={title ? "alert" : undefined}>
          <ScreenTitle title={title ?? "답변을 불러오고 있어요"} />
        </div>
      </Screen>
    );
  }

  return (
    <Screen header={<TopBar onBack={back} />} cta={cta}>
      <p className="mb-2 text-caption text-text-sub">{`${seq}번 질문`}</p>
      <ScreenTitle
        title={item.question}
        description={item.intent ? `이 질문으로 보려는 것: ${item.intent}` : undefined}
      />

      {item.answerStatus && (
        <section>
          <h2 className="text-body1 font-semibold text-text-strong">내 답변</h2>
          {/* 녹음은 본인만 받을 수 있다(세션 쿠키) */}
          <audio
            controls
            preload="none"
            src={api.answerAudioUrl(id, seq)}
            className="mt-3 w-full"
          />
          {item.transcript && (
            <p className="mt-4 rounded-md bg-bg-muted px-4 py-3 text-body2 whitespace-pre-line text-text">
              {item.transcript}
            </p>
          )}
        </section>
      )}

      {item.scores.length > 0 && (
        <section className="mt-10">
          <h2 className="text-body1 font-semibold text-text-strong">기준별 점수</h2>
          <ul className="mt-4 flex flex-col gap-5">
            {item.scores.map((s) => (
              <li key={s.criterion}>
                <ScoreBar label={criterionLabel(s.criterion)} score={s.score} />
                {s.evidence && (
                  <p className="mt-2 text-caption text-text-sub">{`“${s.evidence}”`}</p>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      {item.answerStatus === "EVALUATED" ? (
        <dl className="mt-10 flex flex-col gap-5">
          <div>
            <dt className="text-caption text-text-sub">잘한 점</dt>
            <dd className="mt-1 text-body1 text-text">{item.strength}</dd>
          </div>
          <div>
            <dt className="text-caption text-text-sub">고칠 점</dt>
            <dd className="mt-1 text-body1 text-text">{item.improvement}</dd>
          </div>
          <div>
            <dt className="text-caption text-text-sub">이렇게 답해 보세요</dt>
            <dd className="mt-1 text-body1 text-text">{item.suggestion}</dd>
          </div>
        </dl>
      ) : (
        <p className="mt-10 text-body2 text-text-sub">
          {item.answerStatus == null
            ? "답하지 않았어요."
            : item.answerStatus === "FAILED"
              ? (item.failureReason ?? "평가하지 못했어요.")
              : "평가하고 있어요. 끝나면 점수와 피드백이 보여요."}
        </p>
      )}
    </Screen>
  );
}

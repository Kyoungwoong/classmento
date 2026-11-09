"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { BottomCTA, Button, ProgressBar, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api, ApiError, type InterviewView } from "@/lib/api/client";
import { useRecorder } from "./useRecorder";

const PROCESSING_RETRY_MS = 2000;
const PROCESSING_RETRIES = 15;

const KIND_LABEL = { TECH: "기술 질문", BEHAVIORAL: "인성 질문" } as const;

function mmss(seconds: number) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/** 답이 없거나 실패한 첫 질문 번호(모두 답했으면 마지막) */
function firstOpen(view: InterviewView, done: Set<number>): number {
  const open = view.questions.find(
    (q) => (!q.answerStatus && !done.has(q.seq)) || q.answerStatus === "FAILED",
  );
  return open?.seq ?? view.questions[view.questions.length - 1].seq;
}

/**
 * /interview/[id] — 한 질문씩 말로 답한다(숨고식 단계 폼). 녹음 → 다시 듣기 → 올리고 다음 질문.
 * 마지막 질문을 올리면 면접을 끝내고 리포트로 간다. STT·평가는 서버가 백그라운드에서 한다.
 */
export function InterviewScreen({ id }: { id: number }) {
  const router = useRouter();
  const recorder = useRecorder();
  const [view, setView] = useState<InterviewView | null>(null);
  const [seq, setSeq] = useState<number | null>(null);
  const [done, setDone] = useState<Set<number>>(new Set());
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [uploadFailed, setUploadFailed] = useState(false);

  const load = useCallback(
    () =>
      api.interview(id).then((v) => {
        if (v.status !== "IN_PROGRESS") {
          router.replace(`/interview/${id}/report`);
          return null;
        }
        setView(v);
        return v;
      }),
    [id, router],
  );

  useEffect(() => {
    load()
      .then((v) => v && setSeq(firstOpen(v, new Set())))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace(`/login?next=/interview/${id}`);
          return;
        }
        setError(
          err instanceof ApiError && err.status === 404
            ? "면접을 찾을 수 없어요."
            : "면접을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
        );
      });
  }, [id, load, router]);

  if (!view || seq === null) {
    return (
      <Screen header={<TopBar onBack={() => router.push("/")} />}>
        <ScreenTitle title={error ?? "면접을 불러오고 있어요"} />
      </Screen>
    );
  }

  const question = view.questions.find((q) => q.seq === seq)!;
  const index = view.questions.findIndex((q) => q.seq === seq);
  const total = view.questions.length;
  const remaining = view.questions.filter(
    (q) => q.seq !== seq && ((!q.answerStatus && !done.has(q.seq)) || q.answerStatus === "FAILED"),
  );
  const isLast = remaining.length === 0;

  async function finish(retries = PROCESSING_RETRIES): Promise<void> {
    try {
      await api.finishInterview(id);
      router.push(`/interview/${id}/report`);
    } catch (err) {
      if (err instanceof ApiError && err.code === "ANSWERS_PROCESSING" && retries > 0) {
        setBusy("답변을 정리하고 있어요");
        await new Promise((r) => setTimeout(r, PROCESSING_RETRY_MS));
        return finish(retries - 1);
      }
      if (err instanceof ApiError && err.code === "ANSWERS_FAILED") {
        // 다시 녹음해야 하는 질문으로 돌아간다
        const fresh = await load();
        if (fresh) {
          const failed = fresh.questions.find((q) => q.answerStatus === "FAILED");
          if (failed) {
            setSeq(failed.seq);
            recorder.reset();
            setError(failed.failureReason ?? err.message);
          }
        }
        setBusy(null);
        return;
      }
      setError(
        err instanceof ApiError ? err.message : "면접을 끝내지 못했어요. 다시 시도해 주세요.",
      );
      setBusy(null);
    }
  }

  async function submit() {
    if (!recorder.blob || seq === null) return;
    setError(null);
    setUploadFailed(false);
    setBusy("답변을 올리고 있어요");
    try {
      await api.uploadAnswer(id, seq, recorder.blob);
    } catch {
      setUploadFailed(true);
      setError("답변을 올리지 못했어요. 연결을 확인하고 다시 올려 주세요.");
      setBusy(null);
      return;
    }
    const nextDone = new Set(done).add(seq);
    setDone(nextDone);
    if (isLast) {
      setBusy("면접을 마무리하고 있어요");
      await finish();
      return;
    }
    // 실패로 돌아왔던 질문을 고쳤으면 그 상태를 지운다
    setView({
      ...view!,
      questions: view!.questions.map((q) =>
        q.seq === seq ? { ...q, answerStatus: "UPLOADED", failureReason: undefined } : q,
      ),
    });
    setSeq(remaining[0].seq);
    recorder.reset();
    setBusy(null);
  }

  const micError =
    recorder.state === "denied"
      ? "마이크를 쓸 수 없어요. 주소창의 마이크 권한을 허용한 뒤 다시 시도해 주세요."
      : recorder.state === "unsupported"
        ? "이 브라우저에서는 녹음할 수 없어요. 최신 크롬이나 사파리에서 열어 주세요."
        : null;

  const cta =
    recorder.state === "recording" ? (
      <Button onClick={recorder.stop}>답변 끝내기</Button>
    ) : recorder.state === "recorded" ? (
      <>
        <Button variant="secondary" disabled={busy !== null} onClick={recorder.reset}>
          다시 녹음
        </Button>
        <Button loading={busy !== null} loadingText={busy ?? undefined} onClick={submit}>
          {uploadFailed ? "다시 올리기" : isLast ? "면접 끝내기" : "다음 질문"}
        </Button>
      </>
    ) : (
      <Button onClick={recorder.start}>{micError ? "다시 시도하기" : "답변 시작하기"}</Button>
    );

  return (
    <Screen header={<TopBar onBack={() => router.push("/")} />} cta={<BottomCTA>{cta}</BottomCTA>}>
      <div className="mb-6 flex items-center gap-3">
        <div className="flex-1">
          <ProgressBar value={(index + 1) / total} label="면접 진행도" />
        </div>
        <span className="text-caption text-text-sub">{`${index + 1} / ${total}`}</span>
      </div>
      <ScreenTitle title={question.text} description={KIND_LABEL[question.kind]} />

      {recorder.state === "recording" && (
        <div className="mt-4">
          <p className="text-title1 text-text-strong tabular-nums" aria-live="off">
            {mmss(recorder.elapsed)}
          </p>
          <p className="mt-1 text-body2 text-text-sub">
            녹음하고 있어요. 3분이 되면 저절로 멈춰요.
          </p>
        </div>
      )}
      {recorder.state === "recorded" && recorder.url && (
        <div className="mt-4">
          <p className="mb-3 text-body2 text-text-sub">
            다시 들어 보고 괜찮으면 다음으로 넘어가요.
          </p>
          <audio controls src={recorder.url} className="w-full" />
        </div>
      )}
      {recorder.state === "idle" && !error && (
        <p className="mt-4 text-body2 text-text-sub">
          준비되면 답변을 시작해요. 결론부터 말해 보세요.
        </p>
      )}

      {(error ?? micError) && (
        <p role="alert" className="mt-6 text-body2 text-danger">
          {error ?? micError}
        </p>
      )}
    </Screen>
  );
}

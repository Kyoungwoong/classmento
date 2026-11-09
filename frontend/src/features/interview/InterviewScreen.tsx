"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  Panel,
  ProgressBar,
  Screen,
  ScreenTitle,
  TopBar,
} from "@/components/ui";
import { Mascot, type MascotMood } from "@/components/site/Mascot";
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
  // 마지막 답변은 올렸는데 끝내기만 실패했을 때: 다시 올리지 않고 끝내기만 다시 한다
  const [finishFailed, setFinishFailed] = useState(false);
  const [loadFailed, setLoadFailed] = useState<"notFound" | "error" | null>(null);
  const [attempt, setAttempt] = useState(0);
  // 화면을 떠나면 끝내기 재시도를 멈춘다(나중에 리포트로 끌려가지 않게)
  const alive = useRef(true);
  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);
  const invalidId = !Number.isInteger(id) || id <= 0;

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
    if (invalidId) return;
    load()
      .then((v) => v && setSeq(firstOpen(v, new Set())))
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace(`/login?next=/interview/${id}`);
          return;
        }
        const notFound = err instanceof ApiError && err.status === 404;
        setLoadFailed(notFound ? "notFound" : "error");
        setError(
          notFound
            ? "면접을 찾을 수 없어요."
            : "면접을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.",
        );
      });
  }, [id, invalidId, load, router, attempt]);

  if (invalidId || !view || seq === null) {
    const failure = invalidId ? "notFound" : loadFailed;
    const title = invalidId ? "면접을 찾을 수 없어요." : error;
    return (
      <Screen
        header={<TopBar onBack={() => router.push("/")} />}
        cta={
          failure === "notFound" ? (
            <BottomCTA>
              <ButtonLink href="/interview/new">새 면접 만들기</ButtonLink>
            </BottomCTA>
          ) : failure === "error" ? (
            <BottomCTA>
              <Button
                onClick={() => {
                  setLoadFailed(null);
                  setError(null);
                  setAttempt((n) => n + 1);
                }}
              >
                다시 불러오기
              </Button>
            </BottomCTA>
          ) : undefined
        }
      >
        <Mascot mood={title ? "oops" : "thinking"} size={96} className="mb-6" />
        <div role={title ? "alert" : undefined}>
          <ScreenTitle title={title ?? "면접을 불러오고 있어요"} />
        </div>
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
  // 모든 질문을 올렸으면(새로고침·끝내기 실패 뒤) 다시 녹음하지 않고 끝내기만 한다
  const allAnswered = view.questions.every((q) =>
    q.answerStatus === "FAILED" ? false : Boolean(q.answerStatus) || done.has(q.seq),
  );
  const finishOnly = allAnswered || finishFailed;

  async function finish(retries = PROCESSING_RETRIES): Promise<void> {
    setFinishFailed(false);
    try {
      await api.finishInterview(id);
      if (alive.current) router.push(`/interview/${id}/report`);
    } catch (err) {
      if (!alive.current) return;
      if (err instanceof ApiError && err.code === "ANSWERS_PROCESSING" && retries > 0) {
        setBusy("정리하고 있어요");
        await new Promise((r) => setTimeout(r, PROCESSING_RETRY_MS));
        if (!alive.current) return;
        return finish(retries - 1);
      }
      if (err instanceof ApiError && err.code === "ANSWERS_FAILED") {
        // 다시 녹음해야 하는 질문으로 돌아간다
        const fresh = await load().catch(() => undefined); // null이면 이미 리포트로 보냈다
        const failed = fresh?.questions.find((q) => q.answerStatus === "FAILED");
        if (failed) {
          setSeq(failed.seq);
          recorder.reset();
          setError(`${failed.failureReason ?? err.message} 다시 녹음해 주세요.`);
        } else if (fresh !== null) {
          setError("면접을 끝내지 못했어요. 다시 시도해 주세요.");
          setFinishFailed(true);
        }
        setBusy(null);
        return;
      }
      setError(
        err instanceof ApiError ? err.message : "면접을 끝내지 못했어요. 다시 시도해 주세요.",
      );
      setFinishFailed(true);
      setBusy(null);
    }
  }

  async function submit() {
    if (finishOnly) {
      setError(null);
      setBusy("마무리하고 있어요");
      await finish();
      return;
    }
    if (!recorder.blob || seq === null) return;
    setError(null);
    setUploadFailed(false);
    setBusy("올리고 있어요");
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
    // 실패로 돌아왔던 질문을 고쳤으면 그 상태를 지운다
    setView({
      ...view!,
      questions: view!.questions.map((q) =>
        q.seq === seq ? { ...q, answerStatus: "UPLOADED", failureReason: undefined } : q,
      ),
    });
    if (isLast) {
      setBusy("마무리하고 있어요");
      await finish();
      return;
    }
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

  function rerecord() {
    setUploadFailed(false);
    setError(null);
    recorder.reset();
  }

  const cta =
    finishOnly && recorder.state !== "recording" ? (
      <Button loading={busy !== null} loadingText={busy ?? undefined} onClick={submit}>
        {finishFailed ? "다시 끝내기" : "면접 끝내기"}
      </Button>
    ) : recorder.state === "recording" ? (
      <Button onClick={recorder.stop}>답변 끝내기</Button>
    ) : recorder.state === "recorded" && busy !== null ? (
      <Button loading loadingText={busy}>
        {isLast ? "면접 끝내기" : "다음 질문"}
      </Button>
    ) : recorder.state === "recorded" ? (
      <>
        <Button variant="secondary" onClick={rerecord}>
          다시 녹음
        </Button>
        <Button onClick={submit}>
          {uploadFailed ? "다시 올리기" : isLast ? "면접 끝내기" : "다음 질문"}
        </Button>
      </>
    ) : (
      <Button
        loading={recorder.state === "requesting"}
        loadingText="마이크를 켜고 있어요"
        onClick={recorder.start}
      >
        {micError ? "다시 시도하기" : "답변 시작하기"}
      </Button>
    );

  const shownError = error ?? micError;
  // 마스코트 표정이 지금 상태를 말한다: 녹음 중 듣기 · 올리는 중 생각 · 문제 · 그 밖엔 대기
  const mood: MascotMood =
    recorder.state === "recording"
      ? "listening"
      : busy !== null
        ? "thinking"
        : shownError
          ? "oops"
          : "idle";

  // 왼쪽: 진행도 + 질문(크게)
  const aside = (
    <div>
      <div className="flex items-center gap-3">
        <div className="flex-1">
          <ProgressBar value={(index + 1) / total} label="면접 진행도" />
        </div>
        <span className="text-caption text-text-sub">{`${index + 1} / ${total}`}</span>
      </div>
      <p className="mt-8 inline-flex rounded-full bg-highlight-weak px-3 py-1 text-caption font-semibold text-ink">
        {KIND_LABEL[question.kind]}
      </p>
      <h1 className="mt-4 text-title1 whitespace-pre-line text-text-strong md:text-title1-lg">
        {question.text}
      </h1>
    </div>
  );

  return (
    <Screen
      header={<TopBar onBack={() => router.push("/")} />}
      aside={aside}
      stickyAside
      cta={<BottomCTA>{cta}</BottomCTA>}
    >
      {/* 오른쪽: 녹음 패널 */}
      <Panel className="flex flex-col items-center text-center">
        <Mascot mood={mood} size={112} />
        {recorder.state === "recording" && (
          <div className="mt-4">
            <p className="text-display text-text-strong tabular-nums" aria-live="off">
              {mmss(recorder.elapsed)}
            </p>
            <p role="status" className="mt-1 text-body2 text-text-sub">
              녹음하고 있어요. 3분이 되면 저절로 멈춰요.
            </p>
          </div>
        )}
        {finishOnly && !error && (
          <p className="mt-4 text-body2 text-text-sub">
            모든 질문에 답했어요. 면접을 끝내면 리포트를 볼 수 있어요.
          </p>
        )}
        {!finishOnly && recorder.state === "recorded" && recorder.url && (
          <div className="mt-4 w-full">
            {!error && (
              <p className="mb-3 text-body2 text-text-sub">
                {isLast
                  ? "다시 들어 보고 괜찮으면 면접을 끝내요."
                  : "다시 들어 보고 괜찮으면 다음으로 넘어가요."}
              </p>
            )}
            <audio controls src={recorder.url} className="w-full" />
          </div>
        )}
        {!finishOnly && recorder.state === "idle" && !error && (
          <p className="mt-4 text-body2 text-text-sub">
            준비되면 답변을 시작해요. 결론부터 말해 보세요.
          </p>
        )}
        {shownError && (
          <p role="alert" className="mt-4 text-body2 text-danger">
            {shownError}
          </p>
        )}
      </Panel>
    </Screen>
  );
}

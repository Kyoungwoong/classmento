"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
  ChipGroup,
  ChoiceRow,
  ProgressBar,
  Screen,
  ScreenTitle,
  SegmentedControl,
  SelectChip,
  Toggle,
  TopBar,
} from "@/components/ui";
import { HandNote, NoteSheet } from "@/components/note";
import { ExperienceBanner } from "@/features/resume/ExperienceBanner";
import { JOB_ROLES, LEVELS } from "@/features/questionbank/labels";
import { StepProgress, type ProgressStep } from "./StepProgress";
import {
  api,
  ApiError,
  type CreateInterviewRequest,
  type ExperienceView,
  type ResumeSummary,
} from "@/lib/api/client";

type Type = CreateInterviewRequest["type"];
type Level = NonNullable<CreateInterviewRequest["level"]>;
type JobRole = NonNullable<CreateInterviewRequest["jobRole"]>;
type Step = "type" | "conditions" | "count";

const TYPES: readonly { value: Type; title: string; description: string }[] = [
  {
    value: "EXPERIENCE",
    title: "경험 기반 면접",
    description: "내 경험 하나를 골라 깊게 물어봐요",
  },
  { value: "TECH", title: "기술 면접", description: "직무와 연차에 맞춰 기술·CS를 물어봐요" },
  { value: "BEHAVIORAL", title: "인성 면접", description: "자기소개부터 협업 경험까지 물어봐요" },
  { value: "MIXED", title: "기술 + 인성", description: "실제 면접처럼 번갈아 물어봐요" },
];
/** 질문 하나에 붙일 꼬리질문 최대 수(ADR 0008). 인성 면접에는 없다 */
const FOLLOW_UPS = [
  { value: "0", label: "없음" },
  { value: "1", label: "한 번" },
  { value: "2", label: "두 번" },
] as const;
const COUNTS = [
  { value: "3", label: "3개" },
  { value: "5", label: "5개" },
  { value: "7", label: "7개" },
] as const;
const JOBS = JOB_ROLES.filter((j) => j.value !== "COMMON");

/** /interview/new — 숨고식으로 한 화면에 한 질문: 탭 → 조건 → 질문 수 (ADR 0005). */
export function NewInterviewScreen() {
  const router = useRouter();
  const [step, setStep] = useState<Step>("type");
  const [type, setType] = useState<Type | null>(null);
  const [resumes, setResumes] = useState<ResumeSummary[] | null>(null);
  const [resumesError, setResumesError] = useState(false);
  const [resumesTry, setResumesTry] = useState(0);
  const [experiencesError, setExperiencesError] = useState(false);
  const [experiencesTry, setExperiencesTry] = useState(0);
  const [resumeId, setResumeId] = useState<number | null>(null);
  const [experiences, setExperiences] = useState<ExperienceView[] | null>(null);
  const [experienceId, setExperienceId] = useState<number | null>(null);
  const [jobRole, setJobRole] = useState<JobRole | null>(null);
  const [level, setLevel] = useState<Level | null>(null);
  const [narrow, setNarrow] = useState(false);
  const [count, setCount] = useState<"3" | "5" | "7">("3");
  const [followUps, setFollowUps] = useState<"0" | "1" | "2">("1");
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const parsed = (resumes ?? []).filter((r) => r.status === "PARSED");

  useEffect(() => {
    api
      .resumes()
      .then((list) => {
        setResumesError(false);
        setResumes(list);
        const first = list.find((r) => r.status === "PARSED");
        if (first) setResumeId(first.id);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login?next=/interview/new");
          return;
        }
        setResumesError(true);
      });
  }, [router, resumesTry]);

  // 경험 기반: 고른 이력서의 경험을 불러온다
  useEffect(() => {
    if (type !== "EXPERIENCE" || resumeId === null) return;
    let alive = true;
    api
      .resumeExperiences(resumeId)
      .then((list) => {
        if (!alive) return;
        setExperiencesError(false);
        setExperiences(list);
      })
      .catch(() => alive && setExperiencesError(true));
    return () => {
      alive = false;
    };
  }, [type, resumeId, experiencesTry]);

  function goNext() {
    setError(null);
    if (step === "type") {
      if (!type) return setError("면접 종류를 골라 주세요.");
      setStep(type === "BEHAVIORAL" ? "count" : "conditions");
    } else if (step === "conditions") {
      if (type === "EXPERIENCE" && experienceId === null) {
        return setError("경험을 하나 골라 주세요.");
      }
      if ((type === "TECH" || type === "MIXED") && (!jobRole || !level)) {
        return setError("직무와 연차를 골라 주세요.");
      }
      setStep("count");
    }
  }

  function goBack() {
    setError(null);
    if (step === "count") setStep(type === "BEHAVIORAL" ? "type" : "conditions");
    else if (step === "conditions") setStep("type");
    else router.back();
  }

  async function create() {
    if (!type) return;
    const n = Number(count);
    const base: CreateInterviewRequest =
      type === "EXPERIENCE"
        ? {
            type,
            count: n,
            resumeId: resumeId ?? undefined,
            experienceId: experienceId ?? undefined,
          }
        : type === "TECH"
          ? {
              type,
              count: n,
              level: level ?? undefined,
              jobRole: jobRole ?? undefined,
              narrowByResume: narrow,
              ...(narrow && resumeId !== null ? { resumeId } : {}),
            }
          : type === "MIXED"
            ? { type, count: n, level: level ?? undefined, jobRole: jobRole ?? undefined }
            : { type, count: n };
    // 인성 면접은 꼬리질문이 없다(서버도 0만 받는다)
    const body: CreateInterviewRequest = {
      ...base,
      followUps: type === "BEHAVIORAL" ? 0 : Number(followUps),
    };
    setCreating(true);
    setError(null);
    try {
      const interview = await api.createInterview(body);
      router.push(`/interview/${interview.id}`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/login?next=/interview/new");
        return;
      }
      setError(
        err instanceof ApiError
          ? err.message
          : "면접을 만들지 못했어요. 잠시 후 다시 시도해 주세요.",
      );
      setCreating(false);
    }
  }

  // 인성은 조건 단계가 없다
  const steps: Step[] = type === "BEHAVIORAL" ? ["type", "count"] : ["type", "conditions", "count"];
  const progress = (steps.indexOf(step) + 1) / steps.length;
  const chosen = TYPES.find((t) => t.value === type);
  const stepInfo: Record<Step, ProgressStep> = {
    type: { key: "type", label: "면접 종류", hint: chosen?.title ?? "네 가지 중에서 골라요" },
    conditions: {
      key: "conditions",
      label: "조건",
      hint: type === "EXPERIENCE" ? "경험 하나 고르기" : "직무와 연차",
    },
    count: {
      key: "count",
      label: "질문 수",
      hint: type === "BEHAVIORAL" ? "3·5·7개 중에서" : "질문 수 · 꼬리질문",
    },
  };

  // 모바일: 위 진행 막대 / 데스크톱: 왼쪽 단계 목록
  const aside = (
    <>
      <div className="md:hidden">
        <ProgressBar value={progress} label="면접 만들기 진행도" />
      </div>
      <div className="hidden md:block">
        {/* 노트 첫 장에 적는 면접 계획: 지금 단계가 연한 연두 행 */}
        <NoteSheet>
          <div className="flex items-baseline justify-between gap-3">
            <p className="text-caption font-semibold text-text-sub">면접 계획</p>
            <HandNote className="-rotate-2">금방 끝나요</HandNote>
          </div>
          <div className="mt-3 -ml-4">
            <StepProgress
              label="면접 만들기 단계"
              steps={steps.map((k) => stepInfo[k])}
              current={steps.indexOf(step)}
            />
          </div>
        </NoteSheet>
      </div>
    </>
  );

  const cta =
    step === "count" ? (
      <Button
        loading={creating}
        loadingText={
          type === "EXPERIENCE" ? "경험을 읽고 질문을 만들고 있어요" : "질문 은행에서 고르고 있어요"
        }
        onClick={create}
      >
        질문 만들기
      </Button>
    ) : (
      <Button disabled={step === "type" && !type} onClick={goNext}>
        다음
      </Button>
    );

  return (
    <Screen
      header={<TopBar onBack={goBack} />}
      aside={aside}
      stickyAside
      cta={<BottomCTA>{cta}</BottomCTA>}
    >
      {step === "type" && (
        <>
          <ScreenTitle title="어떤 면접을 볼까요?" description="질문은 모두 말로 답해요." />
          <div role="radiogroup" aria-label="면접 종류" className="-mx-4 flex flex-col gap-2">
            {TYPES.map((t) => {
              const isExperience = t.value === "EXPERIENCE";
              const loading = isExperience && resumes === null && !resumesError;
              const failed = isExperience && resumesError;
              const needsResume = isExperience && resumes !== null && parsed.length === 0;
              // 올렸지만 아직 정리 중이면 다시 올리라고 하지 않는다
              const preparing = needsResume && resumes.length > 0;
              const description = loading
                ? "이력서를 불러오고 있어요"
                : failed
                  ? "이력서를 불러오지 못했어요"
                  : preparing
                    ? "이력서를 정리하고 있어요. 끝나면 고를 수 있어요"
                    : needsResume
                      ? "이력서가 필요해요"
                      : t.description;
              return (
                <div key={t.value}>
                  <ChoiceRow
                    title={t.title}
                    description={description}
                    selected={type === t.value}
                    disabled={loading || failed || needsResume}
                    onSelect={() => setType(t.value)}
                  />
                  {failed && (
                    <button
                      type="button"
                      onClick={() => {
                        setResumesError(false);
                        setResumesTry((n) => n + 1);
                      }}
                      className="-mt-2 inline-flex h-11 items-center px-4 text-caption text-accent"
                    >
                      다시 불러오기
                    </button>
                  )}
                  {needsResume && !preparing && (
                    <Link
                      href="/resume/upload"
                      className="-mt-2 inline-flex h-11 items-center px-4 text-caption text-accent"
                    >
                      이력서 올리러 가기
                    </Link>
                  )}
                </div>
              );
            })}
          </div>
        </>
      )}

      {step === "conditions" && type === "EXPERIENCE" && (
        <>
          <ScreenTitle
            title="어떤 경험으로 면접을 볼까요?"
            description="고른 경험과 관련된 기술·CS 질문을 만들어요."
          />
          {parsed.length > 1 && (
            <div className="mb-6">
              <ChipGroup label="이력서">
                {parsed.map((r) => (
                  <SelectChip
                    key={r.id}
                    label={r.filename}
                    selected={r.id === resumeId}
                    onClick={() => {
                      setResumeId(r.id);
                      setExperiencesError(false);
                      setExperiences(null);
                      setExperienceId(null);
                    }}
                  />
                ))}
              </ChipGroup>
            </div>
          )}
          {experiencesError ? (
            <div role="alert">
              <p className="text-body2 text-text-sub">경험을 불러오지 못했어요.</p>
              <button
                type="button"
                onClick={() => {
                  setExperiencesError(false);
                  setExperiences(null);
                  setExperiencesTry((n) => n + 1);
                }}
                className="-ml-2 inline-flex h-11 items-center px-2 text-caption text-accent"
              >
                다시 불러오기
              </button>
            </div>
          ) : (
            experiences === null && (
              <p className="text-body2 text-text-hint">경험을 불러오고 있어요</p>
            )
          )}
          {experiences?.length === 0 && (
            <div>
              <p className="text-body2 text-text-sub">
                경험을 찾지 못했어요. 이력서 화면에서 경험을 나눈 뒤 다시 와 주세요.
              </p>
              <Link
                href={`/resume/${resumeId}`}
                className="-ml-2 inline-flex h-11 items-center px-2 text-caption text-accent"
              >
                이력서 화면으로 가기
              </Link>
            </div>
          )}
          {experiences && experiences.length > 0 && (
            <div role="radiogroup" aria-label="경험" className="flex flex-col gap-3">
              {experiences.map((e) => (
                // button 안에는 제목·목록 같은 블록 요소를 둘 수 없어 div에 radio 역할과 키보드를 준다
                <div
                  key={e.id}
                  role="radio"
                  tabIndex={0}
                  aria-checked={e.id === experienceId}
                  aria-label={`${e.title}. ${e.summary}`}
                  onClick={() => setExperienceId(e.id)}
                  onKeyDown={(ev) => {
                    if (ev.key === "Enter" || ev.key === " ") {
                      ev.preventDefault();
                      setExperienceId(e.id);
                    }
                  }}
                  className="cursor-pointer rounded-lg text-left outline-none focus-visible:ring-2 focus-visible:ring-accent"
                >
                  <ExperienceBanner experience={e} selectable selected={e.id === experienceId} />
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {step === "conditions" && (type === "TECH" || type === "MIXED") && (
        <>
          <ScreenTitle
            title="어떤 직무로 볼까요?"
            description="직무와 연차에 맞는 질문을 골라요."
          />
          <div className="flex flex-col gap-8">
            <ChipGroup label="직무">
              {JOBS.map((j) => (
                <SelectChip
                  key={j.value}
                  label={j.label}
                  selected={jobRole === j.value}
                  onClick={() => setJobRole(j.value)}
                />
              ))}
            </ChipGroup>
            <SegmentedControl label="연차" options={LEVELS} value={level} onChange={setLevel} />
            {type === "TECH" && parsed.length > 0 && (
              <Toggle
                label={`${parsed.find((r) => r.id === resumeId)?.filename ?? "내 이력서"} 기술로 좁히기`}
                checked={narrow}
                onChange={setNarrow}
              />
            )}
          </div>
        </>
      )}

      {step === "count" && (
        <>
          <ScreenTitle
            title="질문은 몇 개로 할까요?"
            description="질문 하나에 2~3분 정도 걸려요."
          />
          <SegmentedControl label="질문 수" options={COUNTS} value={count} onChange={setCount} />
          {type !== "BEHAVIORAL" && (
            <div className="mt-8">
              <SegmentedControl
                label="꼬리질문"
                options={FOLLOW_UPS}
                value={followUps}
                onChange={setFollowUps}
              />
              <p className="mt-2 text-caption text-text-sub">
                답을 듣고 그 답에서 더 파고들어요. 기술 질문에만 붙어요.
              </p>
            </div>
          )}
        </>
      )}

      {error && (
        <p role="alert" className="mt-6 text-body2 text-danger">
          {error}
        </p>
      )}
    </Screen>
  );
}

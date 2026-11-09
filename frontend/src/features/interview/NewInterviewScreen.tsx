"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  ChipGroup,
  ChoiceRow,
  Screen,
  ScreenTitle,
  SegmentedControl,
  SelectChip,
  Toggle,
  TopBar,
} from "@/components/ui";
import { ExperienceBanner } from "@/features/resume/ExperienceBanner";
import { JOB_ROLES, LEVELS } from "@/features/questionbank/labels";
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
  const [resumeId, setResumeId] = useState<number | null>(null);
  const [experiences, setExperiences] = useState<ExperienceView[] | null>(null);
  const [experienceId, setExperienceId] = useState<number | null>(null);
  const [jobRole, setJobRole] = useState<JobRole | null>(null);
  const [level, setLevel] = useState<Level | null>(null);
  const [narrow, setNarrow] = useState(false);
  const [count, setCount] = useState<"3" | "5" | "7">("3");
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);

  const parsed = (resumes ?? []).filter((r) => r.status === "PARSED");

  useEffect(() => {
    api
      .resumes()
      .then((list) => {
        setResumes(list);
        const first = list.find((r) => r.status === "PARSED");
        if (first) setResumeId(first.id);
      })
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login?next=/interview/new");
          return;
        }
        setResumes([]);
      });
  }, [router]);

  // 경험 기반: 고른 이력서의 경험을 불러온다
  useEffect(() => {
    if (type !== "EXPERIENCE" || resumeId === null) return;
    let alive = true;
    api
      .resumeExperiences(resumeId)
      .then((list) => alive && setExperiences(list))
      .catch(() => alive && setExperiences([]));
    return () => {
      alive = false;
    };
  }, [type, resumeId]);

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
    const body: CreateInterviewRequest =
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
      setError(err instanceof ApiError ? err.message : "잠시 후 다시 시도해 주세요.");
      setCreating(false);
    }
  }

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
      <Button onClick={goNext}>다음</Button>
    );

  return (
    <Screen header={<TopBar onBack={goBack} />} cta={<BottomCTA>{cta}</BottomCTA>}>
      {step === "type" && (
        <>
          <ScreenTitle title="어떤 면접을 볼까요?" description="질문은 모두 말로 답해요." />
          <div role="radiogroup" aria-label="면접 종류" className="flex flex-col gap-2">
            {TYPES.map((t) => {
              const needsResume = t.value === "EXPERIENCE" && parsed.length === 0;
              return (
                <ChoiceRow
                  key={t.value}
                  title={t.title}
                  description={needsResume ? "이력서가 필요해요" : t.description}
                  selected={type === t.value}
                  disabled={needsResume}
                  onSelect={() => setType(t.value)}
                />
              );
            })}
          </div>
          {resumes !== null && parsed.length === 0 && (
            <ButtonLink href="/resume/upload" variant="ghost" size="md" className="mt-4 -ml-4">
              이력서 올리러 가기
            </ButtonLink>
          )}
        </>
      )}

      {step === "conditions" && type === "EXPERIENCE" && (
        <>
          <ScreenTitle
            title={"어떤 경험으로\n면접을 볼까요?"}
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
                      setExperiences(null);
                      setExperienceId(null);
                    }}
                  />
                ))}
              </ChipGroup>
            </div>
          )}
          {experiences === null && (
            <p className="text-body2 text-text-hint">경험을 불러오고 있어요</p>
          )}
          {experiences?.length === 0 && (
            <p className="text-body2 text-text-sub">
              이 이력서에서 경험을 찾지 못했어요. 이력서 화면에서 경험으로 나눠 주세요.
            </p>
          )}
          {experiences && experiences.length > 0 && (
            <div role="radiogroup" aria-label="경험" className="flex flex-col gap-3">
              {experiences.map((e) => (
                <button
                  key={e.id}
                  type="button"
                  role="radio"
                  aria-checked={e.id === experienceId}
                  aria-label={`${e.title}. ${e.summary}`}
                  onClick={() => setExperienceId(e.id)}
                  className="rounded-lg text-left"
                >
                  <ExperienceBanner experience={e} selected={e.id === experienceId} />
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {step === "conditions" && (type === "TECH" || type === "MIXED") && (
        <>
          <ScreenTitle
            title={"어떤 직무로\n볼까요?"}
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
              <Toggle label="내 이력서 기술로 좁히기" checked={narrow} onChange={setNarrow} />
            )}
          </div>
        </>
      )}

      {step === "count" && (
        <>
          <ScreenTitle
            title={"질문은\n몇 개로 할까요?"}
            description="질문 하나에 2~3분 정도 걸려요."
          />
          <SegmentedControl label="질문 수" options={COUNTS} value={count} onChange={setCount} />
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

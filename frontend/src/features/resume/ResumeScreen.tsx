"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState, type ReactNode } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  Chip,
  ListRow,
  ProgressBar,
  Screen,
  ScreenTitle,
  TextField,
  Toast,
  TopBar,
} from "@/components/ui";
import {
  api,
  ApiError,
  type ExperienceView,
  type ParsedResume,
  type ResumeDetail,
} from "@/lib/api/client";
import { ExperienceBanner } from "./ExperienceBanner";
import { useResume } from "./useResume";

export function ResumeScreen() {
  const id = Number(useParams<{ id: string }>().id);
  const router = useRouter();
  const { resume, setResume, error } = useResume(id);

  // 세션이 만료됐으면 로그인으로 (렌더 중이 아니라 effect에서 이동)
  useEffect(() => {
    if (error?.status === 401) router.replace(`/login?next=/resume/${id}`);
  }, [error, id, router]);

  if (error) {
    const notFound = error.status === 404;
    return (
      <Screen
        header={<TopBar />}
        cta={
          <BottomCTA>
            {notFound ? (
              <ButtonLink href="/resume/upload">이력서 올리기</ButtonLink>
            ) : (
              <Button onClick={() => window.location.reload()}>다시 시도하기</Button>
            )}
          </BottomCTA>
        }
      >
        <ScreenTitle
          title={notFound ? "이력서를 찾을 수 없어요" : "이력서를 불러오지 못했어요"}
          description={notFound ? "지워졌거나 다른 계정의 이력서예요." : error.message}
        />
      </Screen>
    );
  }
  // 처음 불러오는 중: 처리 중 화면과 구분 (이미 끝난 이력서를 "읽고 있어요"로 보이지 않게)
  if (!resume) {
    return (
      <Screen header={<TopBar />}>
        <p className="text-body2 text-text-hint">이력서를 불러오고 있어요</p>
      </Screen>
    );
  }
  if (resume.status === "UPLOADED" || resume.status === "EXTRACTED") {
    return <Processing status={resume.status} />;
  }
  if (resume.status === "FAILED") {
    return (
      <Screen
        header={<TopBar />}
        cta={
          <BottomCTA>
            <ButtonLink href="/resume/upload">다시 올리기</ButtonLink>
          </BottomCTA>
        }
      >
        <ScreenTitle
          title="이력서를 읽지 못했어요"
          description={resume.failureReason ?? "다른 파일로 다시 올려주세요."}
        />
      </Screen>
    );
  }
  return <ParsedView resume={resume} onSaved={setResume} />;
}

function Processing({ status }: { status?: string }) {
  const steps = [
    { key: "UPLOADED", label: "글자를 읽고 있어요" },
    { key: "EXTRACTED", label: "경력과 기술을 정리하고 있어요" },
  ];
  const current = status === "EXTRACTED" ? 1 : 0;
  return (
    <Screen header={<TopBar back={false} />}>
      <ScreenTitle title="이력서를 읽고 있어요" description="보통 30초 안에 끝나요." />
      <ProgressBar value={status === "EXTRACTED" ? 0.7 : 0.3} label="이력서 처리 진행률" />
      <ol className="mt-6" aria-live="polite">
        {steps.map((s, i) => (
          <li
            key={s.key}
            className={
              i <= current ? "py-2 text-body1 text-text-strong" : "py-2 text-body1 text-text-hint"
            }
          >
            {s.label}
          </li>
        ))}
      </ol>
    </Screen>
  );
}

function ParsedView({
  resume,
  onSaved,
}: {
  resume: ResumeDetail;
  onSaved: (r: ResumeDetail) => void;
}) {
  const [draft, setDraft] = useState<ParsedResume>(resume.parsed!);
  const [newSkill, setNewSkill] = useState("");
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const dirty = JSON.stringify(draft) !== JSON.stringify(resume.parsed);

  function addSkill() {
    const skill = newSkill.trim();
    if (skill && !draft.skills.includes(skill)) {
      setDraft({ ...draft, skills: [...draft.skills, skill] });
    }
    setNewSkill("");
  }

  async function save() {
    setSaving(true);
    try {
      const saved = await api.updateParsed(resume.id, draft);
      onSaved(saved);
      setToast("저장했어요");
    } catch (err) {
      setToast(
        err instanceof ApiError ? err.message : "저장하지 못했어요. 잠시 후 다시 저장해 주세요.",
      );
    } finally {
      setSaving(false);
      setTimeout(() => setToast(null), 2000);
    }
  }

  const { profile, educations } = draft;

  return (
    <Screen
      header={<TopBar />}
      cta={
        <BottomCTA caption={dirty ? undefined : "면접 질문 만들기는 곧 열려요"}>
          {dirty ? (
            <Button onClick={save} loading={saving} loadingText="저장하고 있어요">
              고친 내용 저장하기
            </Button>
          ) : (
            <Button disabled>면접 준비하기</Button>
          )}
        </BottomCTA>
      }
    >
      <ScreenTitle
        title={"이렇게 정리했어요\n빠진 기술이 있으면 더해 주세요"}
        description={resume.filename}
      />

      <Section title="기본 정보">
        <ListRow title={profile.name ?? "이름 없음"} description={profileLine(profile)} />
      </Section>

      <Section title="기술" count={draft.skills.length}>
        <div className="flex flex-wrap gap-2 pt-2">
          {draft.skills.map((s) => (
            <Chip
              key={s}
              label={s}
              onRemove={() => setDraft({ ...draft, skills: draft.skills.filter((x) => x !== s) })}
            />
          ))}
        </div>
        <form
          className="mt-4 flex items-end gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            addSkill();
          }}
        >
          <TextField
            label="빠진 기술 추가"
            placeholder="예: Docker"
            value={newSkill}
            onChange={(e) => setNewSkill(e.target.value)}
            className="flex-1"
          />
          <Button
            type="submit"
            variant="secondary"
            size="md"
            className="h-14"
            disabled={!newSkill.trim()}
          >
            추가하기
          </Button>
        </form>
      </Section>

      <ExperienceSection
        resumeId={resume.id}
        version={resume.parsed}
        onResync={() => api.updateParsed(resume.id, resume.parsed!).then(onSaved)}
      />

      <Section title="학력" count={educations.length}>
        {educations.map((e, i) => (
          <ListRow
            key={i}
            title={e.school ?? ""}
            description={[e.major, e.period].filter(Boolean).join(" · ")}
          />
        ))}
      </Section>

      <Toast open={!!toast} message={toast ?? ""} />
    </Screen>
  );
}

function profileLine(p: ParsedResume["profile"]) {
  const years = p.yearsOfExperience != null ? `경력 ${p.yearsOfExperience}년` : null;
  return [p.headline, years].filter(Boolean).join(" · ") || undefined;
}

function Section({
  title,
  count,
  description,
  showEmpty = true,
  children,
}: {
  title: string;
  count?: number;
  description?: string;
  /** false면 count가 0이어도 "찾지 못했어요" 대신 children을 그린다(섹션이 빈 상태를 직접 그릴 때) */
  showEmpty?: boolean;
  children: ReactNode;
}) {
  return (
    <section className="mt-10 first-of-type:mt-0">
      <h2 className="text-title2 text-text-strong">
        {title}
        {count !== undefined && count > 0 && <span className="ml-1.5 text-text-hint">{count}</span>}
        {count === 0 && showEmpty && <span className="ml-1.5 text-text-hint">0</span>}
      </h2>
      {description && <p className="mt-1 text-body2 text-text-sub">{description}</p>}
      {count === 0 && showEmpty ? (
        <p className="py-4 text-body2 text-text-hint">찾지 못했어요</p>
      ) : (
        <div className="mt-3">{children}</div>
      )}
    </section>
  );
}

/**
 * 경험(경력·프로젝트 하나씩) 배너 목록. 경험 기반 면접에서 고르는 단위(ADR 0005).
 * 이전 버전으로 정리된 이력서는 경험이 비어 있을 수 있어, 다시 나누는 행동을 준다.
 */
function ExperienceSection({
  resumeId,
  version,
  onResync,
}: {
  resumeId: number;
  version: unknown;
  onResync: () => Promise<unknown>;
}) {
  const [items, setItems] = useState<ExperienceView[] | null>(null);
  const [error, setError] = useState<"load" | "resync" | null>(null);
  const [resyncing, setResyncing] = useState(false);
  const [resynced, setResynced] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    api
      .resumeExperiences(resumeId)
      .then((list) => {
        if (cancelled) return;
        setItems(list);
        setError(null);
      })
      .catch(() => !cancelled && setError("load"));
    return () => {
      cancelled = true;
    };
  }, [resumeId, version, reloadKey]);

  async function resync() {
    setResyncing(true);
    setError(null);
    try {
      await onResync();
      setResynced(true);
    } catch {
      setError("resync");
    } finally {
      setResyncing(false);
    }
  }

  const retry = error === "resync" ? resync : () => setReloadKey((k) => k + 1);

  return (
    <Section
      title="경험"
      count={items?.length}
      description="곧 경험 하나를 골라 그 경험으로 면접을 볼 수 있어요."
      showEmpty={false}
    >
      {error ? (
        <div role="alert">
          <p className="text-body2 text-danger">
            {error === "load"
              ? "경험을 불러오지 못했어요."
              : "경험으로 나누지 못했어요. 다시 시도해 주세요."}
          </p>
          <Button variant="secondary" size="md" className="mt-3" onClick={retry}>
            다시 시도하기
          </Button>
        </div>
      ) : items === null ? (
        <p className="text-body2 text-text-hint">경험을 불러오고 있어요</p>
      ) : items.length === 0 ? (
        <div className="rounded-lg bg-bg-muted px-5 py-4">
          <p className="text-body2 text-text">
            {resynced
              ? "이력서에서 경력·프로젝트를 찾지 못했어요."
              : "이 이력서는 경험별로 아직 정리하지 않았어요."}
          </p>
          {!resynced && (
            <Button
              variant="secondary"
              size="md"
              className="mt-3"
              onClick={resync}
              loading={resyncing}
              loadingText="나누고 있어요"
            >
              경험으로 나누기
            </Button>
          )}
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {items.map((e) => (
            <li key={e.id}>
              <ExperienceBanner experience={e} />
            </li>
          ))}
        </ul>
      )}
    </Section>
  );
}

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
import { api, ApiError, type ParsedResume, type ResumeDetail } from "@/lib/api/client";
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

  const { profile, experiences, projects, educations } = draft;

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

      <Section title="경력" count={experiences.length}>
        {experiences.map((e, i) => (
          <ListRow
            key={i}
            title={[e.company, e.role].filter(Boolean).join(" · ")}
            description={[e.period, ...e.highlights].filter(Boolean).join("\n")}
          />
        ))}
      </Section>

      <Section title="프로젝트" count={projects.length}>
        {projects.map((p, i) => (
          <ListRow
            key={i}
            title={p.name ?? "이름 없는 프로젝트"}
            description={[p.period, p.description, p.techStack.join(", ")]
              .filter(Boolean)
              .join("\n")}
          />
        ))}
      </Section>

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
  children,
}: {
  title: string;
  count?: number;
  children: ReactNode;
}) {
  return (
    <section className="mt-10 first-of-type:mt-0">
      <h2 className="text-title2 text-text-strong">
        {title}
        {count !== undefined && <span className="ml-1.5 text-text-hint">{count}</span>}
      </h2>
      {count === 0 ? (
        <p className="py-4 text-body2 text-text-hint">찾지 못했어요</p>
      ) : (
        <div className="mt-2">{children}</div>
      )}
    </section>
  );
}

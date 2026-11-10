"use client";

import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
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
import { GridPaper, NoteSheet } from "@/components/note";
import { Mascot } from "@/components/site/Mascot";
import { api, ApiError, type ParsedResume, type ResumeDetail } from "@/lib/api/client";
import { ExperienceSection } from "./ExperienceSection";
import { Section } from "./ResumeSection";
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
        <Mascot mood="oops" size={96} className="mb-6" />
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
      <Screen width="narrow" header={<TopBar />}>
        <Mascot mood="thinking" size={96} className="mb-6" />
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
        <Mascot mood="oops" size={96} className="mb-6" />
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
    <Screen width="narrow" header={<TopBar back={false} />}>
      <Mascot mood="thinking" size={112} className="mb-6" />
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

  const cta = (
    <BottomCTA>
      {dirty ? (
        <Button onClick={save} loading={saving} loadingText="저장하고 있어요">
          고친 내용 저장하기
        </Button>
      ) : (
        <ButtonLink href="/interview/new">면접 준비하기</ButtonLink>
      )}
    </BottomCTA>
  );

  // 위: 모눈 위 "정리 노트"(프로필 + 기술). 아래: 경험(넓게) · 학력(좁게)
  return (
    <main>
      <section aria-labelledby="resume-heading" className="relative isolate bg-paper">
        <GridPaper />
        <div className="mx-auto w-full max-w-site px-5 pb-12 md:px-8 md:pb-16">
          <div className="flex h-14 items-center">
            <TopBar />
          </div>
          <div className="pt-4 md:pt-6">
            <p className="text-caption font-semibold text-text-sub">{resume.filename}</p>
            <h1 id="resume-heading" className="mt-2 text-title1 text-text-strong md:text-title1-lg">
              이렇게 정리했어요
              <br />
              빠진 기술이 있으면 더해 주세요
            </h1>
          </div>
          <NoteSheet className="mt-8">
            <div className="grid gap-8 lg:grid-cols-12 lg:gap-10">
              <div className="lg:col-span-4">
                <h2 className="text-caption font-semibold text-text-sub">기본 정보</h2>
                <p className="mt-2 text-title2 text-text-strong">{profile.name ?? "이름 없음"}</p>
                {profileLine(profile) && (
                  <p className="mt-1 text-body2 text-text">{profileLine(profile)}</p>
                )}
              </div>
              <div className="lg:col-span-8">
                <h2 className="text-caption font-semibold text-text-sub">
                  기술 <span className="text-text-hint">{draft.skills.length}</span>
                </h2>
                {draft.skills.length === 0 ? (
                  <p className="py-2 text-body2 text-text-hint">찾지 못했어요</p>
                ) : (
                  <div className="mt-3 flex flex-wrap gap-2">
                    {draft.skills.map((s) => (
                      <Chip
                        key={s}
                        label={s}
                        onRemove={() =>
                          setDraft({ ...draft, skills: draft.skills.filter((x) => x !== s) })
                        }
                      />
                    ))}
                  </div>
                )}
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
              </div>
            </div>
          </NoteSheet>
        </div>
      </section>

      <div className="mx-auto grid w-full max-w-site gap-12 px-5 pt-12 pb-32 md:px-8 md:pt-16 md:pb-20 lg:grid-cols-12 lg:gap-10">
        <div className="lg:col-span-8">
          <ExperienceSection
            resumeId={resume.id}
            version={resume.parsed}
            onResync={() => api.updateParsed(resume.id, resume.parsed!).then(onSaved)}
          />
        </div>
        <div className="lg:col-span-4">
          <Section title="학력" count={educations.length}>
            {educations.map((e, i) => (
              <ListRow
                key={i}
                title={e.school ?? ""}
                description={[e.major, e.period].filter(Boolean).join(" · ")}
              />
            ))}
          </Section>
        </div>
        <div className="lg:col-span-12">{cta}</div>
      </div>

      <Toast open={!!toast} message={toast ?? ""} />
    </main>
  );
}

function profileLine(p: ParsedResume["profile"]) {
  const years = p.yearsOfExperience != null ? `경력 ${p.yearsOfExperience}년` : null;
  return [p.headline, years].filter(Boolean).join(" · ") || undefined;
}

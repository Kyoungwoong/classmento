"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  Screen,
  ScreenTitle,
  SegmentedControl,
  SelectChip,
  TagInput,
  TextArea,
  TextField,
  TopBar,
  type TagItem,
} from "@/components/ui";
import { actionError, adminError } from "@/features/admin/adminError";
import { api, type CategoryTreeNode, type QuestionView } from "@/lib/api/client";
import { JOB_ROLES, LEVELS, type JobRole, type Level } from "./labels";

const TEXT_MIN = 5;
const TEXT_MAX = 300;
const INTENT_MAX = 200;

type Errors = Partial<Record<"category" | "text" | "level" | "jobRoles", string>>;

const suggestTags = (q: string) => api.suggestTags(q);
const createTag = (name: string) => api.createTag(name).then((r) => r.tag);

/** /admin/questions/new, /admin/questions/[id] — 질문 넣기·고치기 */
export function QuestionFormScreen({ id }: { id?: number }) {
  const router = useRouter();
  const editing = id !== undefined;
  const [tree, setTree] = useState<CategoryTreeNode[] | null>(null);
  const [original, setOriginal] = useState<QuestionView | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [topId, setTopId] = useState<number | null>(null);
  const [subId, setSubId] = useState<number | null>(null);
  const [text, setText] = useState("");
  const [level, setLevel] = useState<Level | null>(null);
  const [jobRoles, setJobRoles] = useState<JobRole[]>([]);
  const [tags, setTags] = useState<TagItem[]>([]);
  const [intent, setIntent] = useState("");

  const [errors, setErrors] = useState<Errors>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [toggling, setToggling] = useState(false);

  useEffect(() => {
    const here = editing ? `/admin/questions/${id}` : "/admin/questions/new";
    Promise.all([api.adminCategories(), editing ? api.adminQuestion(id) : null])
      .then(([categories, q]) => {
        setTree(categories);
        if (q) {
          setOriginal(q);
          setTopId(q.topCategoryId);
          setSubId(q.categoryId);
          setText(q.text);
          setLevel(q.level);
          setJobRoles(q.jobRoles);
          setTags(q.tags);
          setIntent(q.intent ?? "");
        }
      })
      .catch((err) => {
        const message = adminError(err, router, here);
        if (message) setLoadError(message);
      });
  }, [editing, id, router]);

  // 숨긴 카테고리는 고를 수 없다(지금 질문이 들어 있는 곳만 예외로 보여준다)
  const tops = (tree ?? []).filter((t) => t.active || t.id === original?.topCategoryId);
  const subs =
    tops
      .find((t) => t.id === topId)
      ?.children.filter((c) => c.active || c.id === original?.categoryId) ?? [];

  function chooseTop(next: number) {
    setTopId(next);
    if (!tops.find((t) => t.id === next)?.children.some((c) => c.id === subId)) setSubId(null);
  }

  function toggleJobRole(role: JobRole) {
    setJobRoles((prev) =>
      prev.includes(role)
        ? prev.filter((r) => r !== role)
        : JOB_ROLES.map((j) => j.value).filter((v) => v === role || prev.includes(v)),
    );
  }

  function validate(): Errors {
    const e: Errors = {};
    const len = text.trim().length;
    if (subId === null) e.category = "소분류를 골라 주세요.";
    if (len < TEXT_MIN || len > TEXT_MAX) e.text = "질문은 5~300자로 입력해 주세요.";
    if (level === null) e.level = "난이도를 골라 주세요.";
    if (jobRoles.length === 0) e.jobRoles = "직무를 하나 이상 골라 주세요.";
    return e;
  }

  async function save() {
    const e = validate();
    setErrors(e);
    setFormError(null);
    if (Object.keys(e).length > 0 || subId === null || level === null) return;
    const body = {
      categoryId: subId,
      text: text.trim(),
      level,
      jobRoles,
      tagIds: tags.map((t) => t.id),
      intent: intent.trim() || undefined,
    };
    setSaving(true);
    try {
      await (editing ? api.updateQuestion(id, body) : api.createQuestion(body));
      router.replace("/admin/questions");
    } catch (err) {
      setFormError(actionError(err));
      setSaving(false);
    }
  }

  async function toggleActive() {
    if (!original) return;
    setToggling(true);
    setFormError(null);
    try {
      await api.setQuestionActive(original.id, !original.active);
      router.replace("/admin/questions");
    } catch (err) {
      setFormError(actionError(err));
      setToggling(false);
    }
  }

  if (loadError) {
    return (
      <Screen
        header={<TopBar />}
        cta={
          <BottomCTA>
            <ButtonLink href="/admin/questions">목록으로 가기</ButtonLink>
          </BottomCTA>
        }
      >
        <ScreenTitle title="질문을 불러오지 못했어요" description={loadError} />
      </Screen>
    );
  }

  const ready = tree !== null && (!editing || original !== null);

  return (
    <Screen
      header={<TopBar />}
      cta={
        <BottomCTA>
          {original && (
            <Button
              variant="secondary"
              loading={toggling}
              loadingText="바꾸고 있어요"
              onClick={toggleActive}
            >
              {original.active ? "숨기기" : "다시 보이기"}
            </Button>
          )}
          <Button loading={saving} loadingText="저장하고 있어요" disabled={!ready} onClick={save}>
            저장하기
          </Button>
        </BottomCTA>
      }
    >
      <ScreenTitle
        title={
          !ready ? "질문을 불러오고 있어요" : editing ? "질문을 고쳐요" : "어떤 질문을 넣을까요?"
        }
        description={
          original && !original.active
            ? "숨긴 질문이에요. 면접에 나오지 않아요."
            : "소분류에 넣고 난이도와 직무를 골라 주세요."
        }
      />

      {ready && (
        <div className="flex flex-col gap-8">
          <div>
            <p className="mb-2 text-caption text-text-sub">대분류</p>
            <div className="flex flex-wrap gap-2">
              {tops.map((t) => (
                <SelectChip
                  key={t.id}
                  label={t.name}
                  selected={t.id === topId}
                  onClick={() => chooseTop(t.id)}
                />
              ))}
            </div>
            <p className="mt-6 mb-2 text-caption text-text-sub">소분류</p>
            {topId === null ? (
              <p className="text-body2 text-text-hint">대분류를 먼저 골라 주세요</p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {subs.map((c) => (
                  <SelectChip
                    key={c.id}
                    label={c.name}
                    selected={c.id === subId}
                    onClick={() => setSubId(c.id)}
                  />
                ))}
              </div>
            )}
            {errors.category && <p className="mt-2 text-caption text-danger">{errors.category}</p>}
          </div>

          <TextArea
            label="질문"
            value={text}
            maxLength={TEXT_MAX}
            placeholder="예: TCP와 UDP의 차이를 설명해 주세요."
            error={errors.text}
            onChange={(e) => setText(e.target.value)}
          />

          <div>
            <SegmentedControl label="난이도" options={LEVELS} value={level} onChange={setLevel} />
            {errors.level && <p className="mt-2 text-caption text-danger">{errors.level}</p>}
          </div>

          <div>
            <p className="mb-2 text-caption text-text-sub">직무 (여러 개 고를 수 있어요)</p>
            <div className="flex flex-wrap gap-2">
              {JOB_ROLES.map((j) => (
                <SelectChip
                  key={j.value}
                  label={j.label}
                  selected={jobRoles.includes(j.value)}
                  onClick={() => toggleJobRole(j.value)}
                />
              ))}
            </div>
            {errors.jobRoles && <p className="mt-2 text-caption text-danger">{errors.jobRoles}</p>}
          </div>

          <TagInput
            label="태그 (선택)"
            value={tags}
            onChange={setTags}
            suggest={suggestTags}
            create={createTag}
          />

          <TextField
            label="질문 의도 (선택)"
            value={intent}
            maxLength={INTENT_MAX}
            placeholder="이 질문으로 무엇을 확인하나요?"
            onChange={(e) => setIntent(e.target.value)}
          />

          {formError && (
            <p role="alert" className="text-body2 text-danger">
              {formError}
            </p>
          )}
        </div>
      )}
    </Screen>
  );
}

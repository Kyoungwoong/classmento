"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Button, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { actionError, adminError } from "@/features/admin/adminError";
import { api, type CategoryTreeNode } from "@/lib/api/client";
import { InlineNameForm } from "./InlineNameForm";

const NAME_MAX = 30;
type Editing = { kind: "rename"; id: number } | { kind: "add"; parentId: number | null } | null;

const textButton = "inline-flex h-11 shrink-0 items-center px-2 text-body2";

/** /admin/categories — 대분류별 소분류 목록. 이름 바꾸기·숨기기·추가를 목록 안에서 한다. */
export function CategoriesScreen() {
  const router = useRouter();
  const [tree, setTree] = useState<CategoryTreeNode[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [editing, setEditing] = useState<Editing>(null);
  // 숨기기 실패는 그 행 바로 아래에 보여준다
  const [toggleError, setToggleError] = useState<{ id: number; message: string } | null>(null);

  const load = useCallback(() => {
    // 에러 지우기는 응답 뒤에(effect 안에서 바로 setState 하지 않게)
    return api
      .adminCategories()
      .then((next) => {
        setTree(next);
        setLoadError(null);
      })
      .catch((err) => {
        const message = adminError(err, router, "/admin/categories");
        if (message) setLoadError(message);
      });
  }, [router]);

  useEffect(() => {
    load();
  }, [load]);

  async function done(action: Promise<unknown>) {
    await action;
    setEditing(null);
    await load();
  }

  async function toggle(id: number, active: boolean) {
    setToggleError(null);
    try {
      await done(api.updateCategory(id, { active }));
    } catch (err) {
      setToggleError({ id, message: actionError(err) });
    }
  }

  function renameForm(id: number, name: string) {
    return (
      <InlineNameForm
        label="새 이름"
        initial={name}
        submitLabel="저장"
        maxLength={NAME_MAX}
        onSubmit={(next) => done(api.updateCategory(id, { name: next }))}
        onCancel={() => setEditing(null)}
      />
    );
  }

  const rowError = (id: number) =>
    toggleError?.id === id && (
      <p role="alert" className="-mt-1 pb-2 text-caption text-danger">
        {toggleError.message}
      </p>
    );

  const isRenaming = (id: number) => editing?.kind === "rename" && editing.id === id;
  const isAdding = (parentId: number | null) =>
    editing?.kind === "add" && editing.parentId === parentId;

  return (
    <Screen header={<TopBar />}>
      <ScreenTitle
        title={
          tree
            ? "카테고리를 정리해요"
            : loadError
              ? "카테고리를 불러오지 못했어요"
              : "카테고리를 불러오고 있어요"
        }
        description={
          loadError ? undefined : "질문은 소분류에 넣어요. 소분류 이름을 누르면 바꿀 수 있어요."
        }
      />

      {loadError && (
        <div role="alert">
          <p className="text-body2 text-text-sub">{loadError}</p>
          <Button variant="secondary" size="md" className="mt-4" onClick={load}>
            다시 시도하기
          </Button>
        </div>
      )}
      {tree?.length === 0 && (
        <p className="mb-4 text-body2 text-text-hint">아직 카테고리가 없어요</p>
      )}

      {tree?.map((top, i) => (
        <section key={top.id} className={i === 0 ? undefined : "mt-10"}>
          {isRenaming(top.id) ? (
            renameForm(top.id, top.name)
          ) : (
            <div className="flex items-center gap-2">
              <h2 className="text-title2 text-text-strong">{top.name}</h2>
              {!top.active && <span className="text-body2 text-text-hint">숨김</span>}
              <span className="flex-1" />
              <button
                type="button"
                aria-label={`${top.name} 이름 바꾸기`}
                onClick={() => setEditing({ kind: "rename", id: top.id })}
                className={`${textButton} text-text-sub`}
              >
                이름 바꾸기
              </button>
              <button
                type="button"
                aria-label={`${top.name} ${top.active ? "숨기기" : "다시 보이기"}`}
                onClick={() => toggle(top.id, !top.active)}
                className={`${textButton} -mr-2 text-text-sub`}
              >
                {top.active ? "숨기기" : "다시 보이기"}
              </button>
            </div>
          )}
          {rowError(top.id)}

          <ul className="mt-2">
            {top.children.map((sub) => (
              <li key={sub.id} aria-label={sub.name}>
                {isRenaming(sub.id) ? (
                  renameForm(sub.id, sub.name)
                ) : (
                  <div className="flex items-center gap-2 py-3">
                    <button
                      type="button"
                      aria-label={`${sub.name} 이름 바꾸기`}
                      aria-describedby={`category-${sub.id}-count`}
                      onClick={() => setEditing({ kind: "rename", id: sub.id })}
                      className="-mx-2 min-w-0 flex-1 rounded-sm px-2 py-1 text-left active:bg-bg-subtle"
                    >
                      <span
                        className={`block truncate text-body1 ${sub.active ? "text-text-strong" : "text-text-hint"}`}
                      >
                        {sub.name}
                      </span>
                      <span
                        id={`category-${sub.id}-count`}
                        className="block text-body2 text-text-sub"
                      >
                        {`질문 ${sub.questionCount}개${sub.active ? "" : " · 숨김"}`}
                      </span>
                    </button>
                    <button
                      type="button"
                      aria-label={`${sub.name} ${sub.active ? "숨기기" : "다시 보이기"}`}
                      onClick={() => toggle(sub.id, !sub.active)}
                      className={`${textButton} -mr-2 text-text-sub`}
                    >
                      {sub.active ? "숨기기" : "다시 보이기"}
                    </button>
                  </div>
                )}
                {rowError(sub.id)}
              </li>
            ))}
          </ul>

          {isAdding(top.id) ? (
            <InlineNameForm
              label={`${top.name}의 새 소분류`}
              submitLabel="추가"
              maxLength={NAME_MAX}
              onSubmit={(name) => done(api.createCategory({ parentId: top.id, name }))}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <button
              type="button"
              aria-label={`${top.name}에 소분류 추가`}
              onClick={() => setEditing({ kind: "add", parentId: top.id })}
              className={`${textButton} -ml-2 text-primary`}
            >
              소분류 추가하기
            </button>
          )}
        </section>
      ))}

      {tree && (
        <section className="mt-10">
          {isAdding(null) ? (
            <InlineNameForm
              label="새 대분류"
              submitLabel="추가"
              maxLength={NAME_MAX}
              onSubmit={(name) => done(api.createCategory({ name }))}
              onCancel={() => setEditing(null)}
            />
          ) : (
            <button
              type="button"
              onClick={() => setEditing({ kind: "add", parentId: null })}
              className={`${textButton} -ml-2 text-primary`}
            >
              대분류 추가하기
            </button>
          )}
        </section>
      )}
    </Screen>
  );
}

"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  ListRow,
  Screen,
  ScreenTitle,
  SegmentedControl,
  SelectChip,
  TextField,
  Toggle,
  TopBar,
} from "@/components/ui";
import { adminError } from "@/features/admin/adminError";
import {
  api,
  type CategoryTreeNode,
  type QuestionSearchParams,
  type QuestionView,
} from "@/lib/api/client";
import { LEVELS, levelLabel, type Level } from "./labels";

const PAGE_SIZE = 20;
const SEARCH_DELAY_MS = 300;
const LEVEL_OPTIONS = [{ value: "ALL", label: "전체" }, ...LEVELS] as const;

interface Filter {
  topCategoryId?: number;
  categoryId?: number;
  level?: Level;
  q: string;
  includeHidden: boolean;
}

/** /admin/questions — 질문 은행 목록. 대분류 → 소분류 → 난이도 → 검색어 순서로 좁힌다. */
export function QuestionListScreen() {
  const router = useRouter();
  const [tree, setTree] = useState<CategoryTreeNode[]>([]);
  const [filter, setFilter] = useState<Filter>({ q: "", includeHidden: false });
  const [query, setQuery] = useState("");
  const [items, setItems] = useState<QuestionView[] | null>(null);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);
  const latest = useRef(0);

  useEffect(() => {
    api
      .adminCategories()
      .then(setTree)
      .catch(() => undefined);
  }, []);

  // 검색어는 입력이 멈춘 뒤에 반영
  useEffect(() => {
    const timer = setTimeout(() => setFilter((f) => ({ ...f, q: query.trim() })), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [query]);

  const load = useCallback(
    (nextPage: number) => {
      const req = ++latest.current;
      const params: QuestionSearchParams = {
        topCategoryId: filter.topCategoryId,
        categoryId: filter.categoryId,
        level: filter.level,
        q: filter.q || undefined,
        includeHidden: filter.includeHidden,
        page: nextPage,
        size: PAGE_SIZE,
      };
      return api
        .adminQuestions(params)
        .then((res) => {
          if (req !== latest.current) return;
          setError(null);
          setItems((prev) => (nextPage === 0 || !prev ? res.items : [...prev, ...res.items]));
          setTotal(res.total);
          setPage(nextPage);
        })
        .catch((err) => {
          if (req !== latest.current) return;
          const message = adminError(err, router, "/admin/questions");
          if (message) setError(`질문을 불러오지 못했어요. ${message}`);
        });
    },
    [filter, router],
  );

  useEffect(() => {
    load(0);
  }, [load]);

  async function more() {
    setLoadingMore(true);
    await load(page + 1);
    setLoadingMore(false);
  }

  const top = tree.find((t) => t.id === filter.topCategoryId);

  return (
    <Screen
      header={<TopBar />}
      cta={
        <BottomCTA>
          <ButtonLink href="/admin/questions/new">질문 추가하기</ButtonLink>
        </BottomCTA>
      }
    >
      <ScreenTitle
        title={items ? `질문이 ${total}개 있어요` : "질문을 불러오고 있어요"}
        description="최근에 넣은 순서예요."
      />

      <div className="flex flex-col gap-4">
        <div className="-mx-5 flex gap-2 overflow-x-auto px-5">
          <SelectChip
            label="전체"
            selected={filter.topCategoryId === undefined}
            onClick={() =>
              setFilter((f) => ({ ...f, topCategoryId: undefined, categoryId: undefined }))
            }
          />
          {tree.map((t) => (
            <SelectChip
              key={t.id}
              label={t.name}
              selected={filter.topCategoryId === t.id}
              onClick={() =>
                setFilter((f) => ({ ...f, topCategoryId: t.id, categoryId: undefined }))
              }
            />
          ))}
        </div>
        {top && (
          <div className="-mx-5 flex gap-2 overflow-x-auto px-5">
            <SelectChip
              label={`${top.name} 전체`}
              selected={filter.categoryId === undefined}
              onClick={() => setFilter((f) => ({ ...f, categoryId: undefined }))}
            />
            {top.children.map((c) => (
              <SelectChip
                key={c.id}
                label={c.name}
                selected={filter.categoryId === c.id}
                onClick={() => setFilter((f) => ({ ...f, categoryId: c.id }))}
              />
            ))}
          </div>
        )}
        <SegmentedControl
          label="난이도"
          options={LEVEL_OPTIONS}
          value={filter.level ?? "ALL"}
          onChange={(v) => setFilter((f) => ({ ...f, level: v === "ALL" ? undefined : v }))}
        />
        <TextField
          label="검색"
          type="search"
          placeholder="질문 내용으로 찾기"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <Toggle
          label="숨긴 질문도 보기"
          checked={filter.includeHidden}
          onChange={(v) => setFilter((f) => ({ ...f, includeHidden: v }))}
        />
      </div>

      <div className="mt-6">
        {error && (
          <div role="alert">
            <p className="text-body2 text-text-sub">{error}</p>
            <Button variant="secondary" size="md" className="mt-4" onClick={() => load(0)}>
              다시 시도하기
            </Button>
          </div>
        )}
        {!error && items?.length === 0 && (
          <p className="py-10 text-center text-body2 text-text-hint">조건에 맞는 질문이 없어요</p>
        )}
        {items && items.length > 0 && (
          <ul>
            {items.map((q) => (
              <li key={q.id}>
                <ListRow
                  title={q.text}
                  description={describe(q)}
                  onClick={() => router.push(`/admin/questions/${q.id}`)}
                />
              </li>
            ))}
          </ul>
        )}
        {items && items.length < total && !error && (
          <Button
            variant="ghost"
            size="md"
            fullWidth
            loading={loadingMore}
            loadingText="더 불러오고 있어요"
            onClick={more}
          >
            더 보기
          </Button>
        )}
      </div>
    </Screen>
  );
}

function describe(q: QuestionView) {
  const parts = [q.categoryName, levelLabel(q.level), ...q.tags.map((t) => t.name)];
  if (!q.active) parts.push("숨김");
  return parts.join(" · ");
}

"use client";

import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react";
import { Chip } from "./Chip";

export interface TagItem {
  id: number;
  name: string;
}

interface TagInputProps {
  label: string;
  value: TagItem[];
  onChange: (tags: TagItem[]) => void;
  /** 자동완성 후보 */
  suggest: (query: string) => Promise<TagItem[]>;
  /** 없으면 만들고, 이미 있으면 그 태그를 돌려준다 */
  create: (name: string) => Promise<TagItem>;
  max?: number;
}

const SUGGEST_DELAY_MS = 150;
const sameName = (a: string, b: string) => a.trim().toLowerCase() === b.trim().toLowerCase();

/**
 * Notion식 태그 입력: 입력하면 아래에 후보가 뜨고, 고르거나 Enter·쉼표로 확정한다.
 * 없는 태그는 바로 만든다. 붙은 태그는 칩(지우기 가능)으로 보여준다.
 */
export function TagInput({ label, value, onChange, suggest, create, max = 10 }: TagInputProps) {
  const inputId = useId();
  const listId = useId();
  const [text, setText] = useState("");
  const [options, setOptions] = useState<TagItem[]>([]);
  const [error, setError] = useState<string | null>(null);
  const latest = useRef(0);
  // 연속 확정(쉼표 여러 개) 중에도 최신 목록을 보도록
  const current = useRef(value);
  useEffect(() => {
    current.current = value;
  }, [value]);

  const full = value.length >= max;
  const query = text.trim();

  useEffect(() => {
    if (!query) return;
    const req = ++latest.current;
    const timer = setTimeout(() => {
      suggest(query)
        .then((found) => {
          if (req === latest.current) setOptions(found);
        })
        .catch(() => undefined);
    }, SUGGEST_DELAY_MS);
    return () => clearTimeout(timer);
  }, [query, suggest]);

  function add(tag: TagItem) {
    const now = current.current;
    if (now.some((t) => t.id === tag.id) || now.length >= max) return;
    const next = [...now, tag];
    current.current = next;
    onChange(next);
  }

  async function commit(raw: string) {
    const name = raw.trim();
    if (!name) return;
    if (current.current.some((t) => sameName(t.name, name))) return;
    const known = options.find((o) => sameName(o.name, name));
    if (known) {
      add(known);
      return;
    }
    try {
      add(await create(name));
      setError(null);
    } catch {
      setError("태그를 만들지 못했어요. 다시 시도해 주세요.");
    }
  }

  function changeText(next: string) {
    if (!next.includes(",")) {
      setText(next);
      if (!next.trim()) setOptions([]);
      return;
    }
    const parts = next.split(",");
    const rest = parts.pop() ?? "";
    setText(rest);
    setOptions([]);
    parts.reduce((p, part) => p.then(() => commit(part)), Promise.resolve());
  }

  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      const name = text;
      setText("");
      setOptions([]);
      void commit(name);
    } else if (e.key === "Backspace" && !text && value.length > 0) {
      onChange(value.slice(0, -1));
    } else if (e.key === "Escape") {
      setOptions([]);
    }
  }

  function pick(tag: TagItem) {
    add(tag);
    setText("");
    setOptions([]);
  }

  const visible = query ? options.filter((o) => !value.some((t) => t.id === o.id)) : [];

  return (
    <div>
      <label htmlFor={inputId} className="mb-2 block text-caption text-text-sub">
        {label}
      </label>
      <div className="flex flex-wrap items-center gap-2 rounded-md bg-bg-muted px-3 py-2 focus-within:ring-2 focus-within:ring-primary">
        {value.map((t) => (
          <Chip
            key={t.id}
            label={t.name}
            tone="surface"
            onRemove={() => onChange(value.filter((v) => v.id !== t.id))}
          />
        ))}
        <input
          id={inputId}
          value={text}
          disabled={full}
          onChange={(e) => changeText(e.target.value)}
          onKeyDown={onKeyDown}
          role="combobox"
          aria-expanded={visible.length > 0}
          aria-controls={listId}
          aria-autocomplete="list"
          placeholder={full ? `태그는 ${max}개까지예요` : value.length ? "" : "예: JPA, 트랜잭션"}
          className="h-10 min-w-24 flex-1 bg-transparent text-body1 text-text-strong outline-none placeholder:text-text-hint"
        />
      </div>
      {visible.length > 0 && (
        <div
          id={listId}
          role="listbox"
          aria-label={`${label} 후보`}
          className="mt-1 rounded-md bg-bg-subtle py-1"
        >
          {visible.map((o) => (
            <button
              key={o.id}
              type="button"
              role="option"
              aria-selected={false}
              // 입력칸 blur보다 먼저 고르기
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => pick(o)}
              className="flex h-11 w-full items-center px-4 text-left text-body2 text-text active:bg-bg-muted"
            >
              {o.name}
            </button>
          ))}
        </div>
      )}
      {error ? (
        <p className="mt-2 text-caption text-danger">{error}</p>
      ) : (
        <p className="mt-2 text-caption text-text-hint">Enter나 쉼표를 누르면 새 태그를 만들어요</p>
      )}
    </div>
  );
}

"use client";

import { useState, type FormEvent } from "react";
import { Button, TextField } from "@/components/ui";
import { actionError } from "@/features/admin/adminError";

interface InlineNameFormProps {
  label: string;
  initial?: string;
  submitLabel: string;
  maxLength: number;
  onSubmit: (name: string) => Promise<unknown>;
  onCancel: () => void;
}

/** 목록 안에서 이름을 넣거나 바꾸는 한 줄 폼. 실패하면 칸 아래에 서버 문구를 보여준다. */
export function InlineNameForm({
  label,
  initial = "",
  submitLabel,
  maxLength,
  onSubmit,
  onCancel,
}: InlineNameFormProps) {
  const [name, setName] = useState(initial);
  const [error, setError] = useState<string | undefined>();
  const [saving, setSaving] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) {
      setError("이름을 입력해 주세요.");
      return;
    }
    setSaving(true);
    try {
      await onSubmit(name.trim());
    } catch (err) {
      setError(actionError(err));
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} className="py-3">
      <TextField
        label={label}
        value={name}
        maxLength={maxLength}
        autoFocus
        error={error}
        onChange={(e) => setName(e.target.value)}
      />
      <div className="mt-3 flex justify-end gap-2">
        <Button variant="ghost" size="md" onClick={onCancel}>
          취소
        </Button>
        <Button
          type="submit"
          variant="secondary"
          size="md"
          loading={saving}
          loadingText="저장하고 있어요"
        >
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}

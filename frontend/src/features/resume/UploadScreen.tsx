"use client";

import { useRouter } from "next/navigation";
import { useRef, useState } from "react";
import { BottomCTA, Button, ListRow, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api/client";
import { ACCEPTED_RESUME, formatBytes, validateResumeFile } from "./formatBytes";

export function UploadScreen() {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function pick(selected: File | undefined) {
    if (!selected) return;
    const problem = validateResumeFile(selected);
    setError(problem);
    setFile(problem ? null : selected);
  }

  async function upload() {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const resume = await api.uploadResume(file);
      router.push(`/resume/${resume.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "잠시 후 다시 시도해 주세요.");
      setLoading(false);
    }
  }

  return (
    <Screen
      header={<TopBar />}
      cta={
        <BottomCTA caption="올린 이력서는 면접 질문을 만드는 데만 써요">
          <Button onClick={upload} disabled={!file} loading={loading} loadingText="올리고 있어요">
            올리기
          </Button>
        </BottomCTA>
      }
    >
      <ScreenTitle
        title="이력서를 올려주세요"
        description="경력, 기술, 프로젝트를 자동으로 정리해 드려요."
      />

      <input
        ref={inputRef}
        type="file"
        accept={ACCEPTED_RESUME}
        className="sr-only"
        onChange={(e) => pick(e.target.files?.[0])}
        aria-label="이력서 파일 선택"
      />

      {file ? (
        <ListRow
          leading={<FileBadge name={file.name} />}
          title={file.name}
          description={formatBytes(file.size)}
          trailing={
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              disabled={loading}
              className="-mr-2 h-11 px-2 font-semibold text-primary disabled:text-text-hint"
            >
              바꾸기
            </button>
          }
        />
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="flex w-full flex-col items-center justify-center rounded-lg bg-bg-muted px-5 py-10 active:bg-gray-200"
        >
          <span className="text-body1 font-semibold text-text-strong">파일 고르기</span>
          <span className="mt-1 text-body2 text-text-sub">PDF, DOCX · 최대 20MB</span>
        </button>
      )}

      {error && (
        <p role="alert" className="mt-4 text-body2 text-danger">
          {error}
        </p>
      )}
    </Screen>
  );
}

function FileBadge({ name }: { name: string }) {
  const ext = name.split(".").pop()?.toUpperCase() ?? "";
  return (
    <span
      aria-hidden
      className="flex size-10 items-center justify-center rounded-sm bg-bg-muted text-caption font-bold text-text-sub"
    >
      {ext}
    </span>
  );
}

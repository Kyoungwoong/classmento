"use client";

import clsx from "clsx";
import { FileUp } from "lucide-react";
import { useRouter } from "next/navigation";
import { useRef, useState, type DragEvent } from "react";
import { GridPaper, NoteSheet, StickyNote } from "@/components/note";
import { BottomCTA, Button, ListRow, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api/client";
import { ACCEPTED_RESUME, formatBytes, validateResumeFile } from "./formatBytes";

export function UploadScreen() {
  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [dragging, setDragging] = useState(false);

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

  function onDragOver(e: DragEvent) {
    if (!e.dataTransfer.types.includes("Files")) return;
    // 기본 동작(브라우저가 파일을 열어 화면을 떠나는 것)은 늘 막는다
    e.preventDefault();
    if (loading) {
      e.dataTransfer.dropEffect = "none"; // 올리는 중: 놓기 거부 커서
      return;
    }
    setDragging(true);
  }

  function onDrop(e: DragEvent) {
    e.preventDefault();
    setDragging(false);
    if (loading) return;
    pick(e.dataTransfer.files?.[0]);
  }

  return (
    // 모눈 바탕 위 노트 한 장에 이력서를 끼운다(ADR 0007)
    <div className="relative isolate flex-1 bg-paper">
      <GridPaper />
      <Screen
        width="narrow"
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

        {/* 노트 전체가 놓는 자리: 파일을 고른 뒤에도 끌어다 놓아 바꿀 수 있다 */}
        <NoteSheet
          data-dropzone
          active={dragging}
          onDragOver={onDragOver}
          onDragLeave={() => setDragging(false)}
          onDrop={onDrop}
        >
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
                  className="-mr-2 h-11 px-2 font-semibold text-accent disabled:text-text-hint"
                >
                  바꾸기
                </button>
              }
            />
          ) : (
            <button
              type="button"
              onClick={() => inputRef.current?.click()}
              className={clsx(
                // 노트 줄 위의 가벼운 점선 칸(상자 안 상자처럼 보이지 않게 면 없이)
                "flex w-full flex-col items-center justify-center rounded-md border border-dashed px-5 py-12 transition-colors",
                dragging ? "border-accent" : "border-rule hover:bg-bg-subtle active:bg-bg-muted",
              )}
            >
              <FileUp size={24} aria-hidden className="text-ink" />
              <span className="mt-3 text-body1 font-semibold text-text-strong">파일 고르기</span>
              <span className="mt-1 text-body2 text-text-sub">
                <span className="hidden md:inline">여기로 끌어다 놓아도 돼요 · </span>PDF, DOCX ·
                최대 20MB
              </span>
            </button>
          )}

          {error && (
            <p role="alert" className="mt-4 text-body2 text-danger">
              {error}
            </p>
          )}
        </NoteSheet>

        <StickyNote title="이렇게 정리해요" level="h2" className="mt-10">
          경력·프로젝트를 경험 하나씩 나누고, 기술과 학력을 뽑아요. 틀린 건 다음 화면에서 고칠 수
          있어요.
        </StickyNote>
      </Screen>
    </div>
  );
}

function FileBadge({ name }: { name: string }) {
  const ext = name.split(".").pop()?.toUpperCase() ?? "";
  return (
    <span
      aria-hidden
      className="flex size-10 items-center justify-center rounded-sm bg-bg-muted text-caption font-bold text-text"
    >
      {ext}
    </span>
  );
}

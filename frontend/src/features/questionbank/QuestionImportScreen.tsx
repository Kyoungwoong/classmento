"use client";

import { useRouter } from "next/navigation";
import { useId, useRef, useState } from "react";
import {
  BottomCTA,
  Button,
  ButtonLink,
  ListRow,
  Screen,
  ScreenTitle,
  TopBar,
} from "@/components/ui";
import { actionError } from "@/features/admin/adminError";
import { api, ApiError, type ImportResult } from "@/lib/api/client";
import { downloadSampleCsv } from "./sampleCsv";

const MAX_BYTES = 2 * 1024 * 1024;

/** /admin/questions/import — CSV로 질문 여러 개를 한 번에 넣는다. 오류가 하나라도 있으면 서버는 아무것도 저장하지 않는다. */
export function QuestionImportScreen() {
  const router = useRouter();
  const errorId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  function choose(next: File | undefined) {
    setResult(null);
    setFailure(null);
    if (!next) return;
    if (!next.name.toLowerCase().endsWith(".csv")) {
      setFile(null);
      setFileError("CSV 파일(.csv)을 골라 주세요.");
    } else if (next.size > MAX_BYTES) {
      setFile(null);
      setFileError("CSV 파일은 2MB까지 올릴 수 있어요.");
    } else {
      setFile(next);
      setFileError(null);
    }
  }

  async function upload() {
    if (!file) return;
    setUploading(true);
    setFailure(null);
    try {
      setResult(await api.importQuestions(file));
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        router.replace("/login?next=/admin/questions/import");
        return;
      }
      setFailure(actionError(err));
    } finally {
      setUploading(false);
    }
  }

  /** 같은 파일을 고쳐서 다시 골라도 change가 일어나게 값을 비우고 연다. 고르면 choose()가 입력 화면으로 돌려놓는다. */
  function pickAgain() {
    if (inputRef.current) inputRef.current.value = "";
    inputRef.current?.click();
  }

  if (result && result.errors.length === 0) {
    return (
      <Screen
        header={<TopBar />}
        cta={
          <BottomCTA>
            <ButtonLink href="/admin/questions">목록으로 가기</ButtonLink>
          </BottomCTA>
        }
      >
        <ScreenTitle
          title={`질문 ${result.imported}개를 넣었어요`}
          description="목록에서 바로 확인할 수 있어요."
        />
      </Screen>
    );
  }

  const errors = result?.errors ?? [];

  return (
    <Screen
      header={<TopBar />}
      cta={
        <BottomCTA>
          {errors.length > 0 ? (
            <Button onClick={pickAgain}>다른 파일 고르기</Button>
          ) : (
            <Button
              disabled={!file}
              loading={uploading}
              loadingText="올리고 있어요"
              onClick={upload}
            >
              올리기
            </Button>
          )}
        </BottomCTA>
      }
    >
      {errors.length > 0 ? (
        <ScreenTitle
          title={`${new Set(errors.map((e) => e.line)).size}줄을 고쳐 주세요`}
          description="아무것도 저장하지 않았어요. 고친 뒤 다시 올려 주세요."
        />
      ) : (
        <ScreenTitle
          title={"CSV로 질문을\n한꺼번에 넣어요"}
          description="오류가 한 줄이라도 있으면 아무것도 저장하지 않아요."
        />
      )}

      {/* 오류 화면에서도 [다른 파일 고르기]가 열 수 있게 파일 입력은 늘 그려 둔다 */}
      <input
        ref={inputRef}
        type="file"
        accept=".csv,text/csv"
        aria-label="CSV 파일 고르기"
        // 보이는 [파일 고르기] 버튼이 탭 순서를 맡는다(두 번 멈추지 않게)
        tabIndex={-1}
        onChange={(e) => choose(e.target.files?.[0])}
        className="sr-only"
      />

      {errors.length > 0 ? (
        // 결과가 오면 화면이 바뀐 것을 스크린리더가 바로 읽게
        <ul role="alert">
          {errors.map((e) => (
            <li key={`${e.line}-${e.message}`}>
              <ListRow title={`${e.line}번째 줄`} description={e.message} />
            </li>
          ))}
        </ul>
      ) : (
        <div className="flex flex-col gap-8">
          <section>
            <h2 className="text-body1 font-semibold text-text-strong">이렇게 만들어 주세요</h2>
            <ul className="mt-3 flex list-disc flex-col gap-2 pl-5 text-body2 text-text-sub">
              <li>
                첫 줄에는 아래 이름을 그대로 써요
                <span className="mt-1 block text-caption text-text-hint">
                  대분류, 소분류, 질문, 난이도, 직무, 태그, 의도 (쉼표로 구분)
                </span>
              </li>
              <li>대분류·소분류는 이미 있는 이름을 써요</li>
              <li>직무·태그가 여러 개면 | 로 나눠요. 태그는 없으면 새로 만들어요</li>
              <li>엑셀에서는 &lsquo;CSV UTF-8&rsquo;로 저장해요. 1000줄, 2MB까지</li>
            </ul>
            <button
              type="button"
              onClick={downloadSampleCsv}
              className="mt-2 -ml-2 inline-flex h-11 items-center px-2 text-body2 text-primary"
            >
              예시 파일 받기
            </button>
          </section>

          <section>
            <p className="mb-2 text-caption text-text-sub">CSV 파일</p>
            <div className="flex items-center gap-3 rounded-md bg-bg-muted px-4 py-3">
              <p className="min-w-0 flex-1 truncate text-body1 text-text-strong">
                {file ? file.name : <span className="text-text-hint">파일을 골라 주세요</span>}
              </p>
              <Button
                variant="secondary"
                size="md"
                aria-describedby={fileError ? errorId : undefined}
                onClick={() => inputRef.current?.click()}
              >
                {file ? "바꾸기" : "파일 고르기"}
              </Button>
            </div>
            {fileError && (
              <p id={errorId} role="alert" className="mt-2 text-caption text-danger">
                {fileError}
              </p>
            )}
          </section>

          {failure && (
            <p role="alert" className="text-body2 text-danger">
              {failure}
            </p>
          )}
        </div>
      )}
    </Screen>
  );
}

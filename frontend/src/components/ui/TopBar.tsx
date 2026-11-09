"use client";

import { useRouter } from "next/navigation";
import type { ReactNode } from "react";

interface TopBarProps {
  /** 뒤로 가기 버튼 표시 (기본 true) */
  back?: boolean;
  /** 오른쪽 작은 행동(예: 로그아웃). 텍스트 버튼 하나만. */
  trailing?: ReactNode;
  /** 뒤로 가기 대신 할 일(단계 폼에서 이전 단계로). 없으면 브라우저 뒤로 가기 */
  onBack?: () => void;
}

/** Screen의 header 슬롯에 넣는 상단 바. 제목은 두지 않는다(본문 ScreenTitle이 제목 역할). */
export function TopBar({ back = true, trailing, onBack }: TopBarProps) {
  const router = useRouter();
  return (
    <div className="flex w-full items-center justify-between">
      {back ? (
        <button
          type="button"
          onClick={onBack ?? (() => router.back())}
          aria-label="뒤로 가기"
          className="-ml-2 flex size-11 items-center justify-center rounded-sm text-text active:bg-bg-muted"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden>
            <path
              d="M15 5l-7 7 7 7"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </button>
      ) : (
        <span />
      )}
      {trailing}
    </div>
  );
}

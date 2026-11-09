interface ToastProps {
  message: string;
  open: boolean;
}

/**
 * 짧은 결과 알림. 모바일은 하단 CTA 위, md 이상은 화면 아래쪽에 뜬다. 문장은 "~했어요" 체.
 * live region(role=status)은 늘 두고 내용만 바꾼다 — 새로 붙는 live region은 스크린리더가 놓치기 쉽다.
 */
export function Toast({ message, open }: ToastProps) {
  return (
    <div role="status" className="pointer-events-none fixed inset-x-5 bottom-28 z-50 md:bottom-8">
      {open && (
        <p className="mx-auto max-w-narrow rounded-md bg-ink px-5 py-4 text-body2 text-on-ink shadow-float">
          {message}
        </p>
      )}
    </div>
  );
}

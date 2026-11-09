interface ToastProps {
  message: string;
  open: boolean;
}

/** 짧은 결과 알림. 하단 CTA 위에 뜬다. 문장은 "~했어요" 체. 상태 관리는 호출하는 쪽에서. */
export function Toast({ message, open }: ToastProps) {
  if (!open) return null;
  return (
    <div
      role="status"
      className="fixed inset-x-5 bottom-28 z-50 mx-auto max-w-narrow rounded-md bg-ink px-5 py-4 text-body2 text-on-ink shadow-float"
    >
      {message}
    </div>
  );
}

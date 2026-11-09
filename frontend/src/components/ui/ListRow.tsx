import clsx from "clsx";
import type { ReactNode } from "react";

interface ListRowProps {
  /** 왼쪽 숫자·아이콘 자리(선택) */
  leading?: ReactNode;
  title: ReactNode;
  description?: ReactNode;
  /** 오른쪽 값·상태 */
  trailing?: ReactNode;
  onClick?: () => void;
  /** 제목 줄 수. 긴 문장(질문 등)은 2 */
  titleLines?: 1 | 2;
}

/** 정보 나열의 기본 단위. 카드 대신 이걸 세로로 쌓는다(구분은 여백으로). */
export function ListRow({
  leading,
  title,
  description,
  trailing,
  onClick,
  titleLines = 1,
}: ListRowProps) {
  const Comp = onClick ? "button" : "div";
  return (
    <Comp
      {...(onClick ? { type: "button" as const, onClick } : {})}
      className={clsx(
        "flex w-full items-center gap-4 py-4 text-left",
        onClick && "-mx-2 rounded-sm px-2 active:bg-bg-subtle",
      )}
    >
      {leading && <div className="shrink-0">{leading}</div>}
      <div className="min-w-0 flex-1">
        {/* 긴 파일명·이메일이 좁은 화면에서 넘치지 않게 */}
        <p
          className={clsx(
            "text-body1 font-medium text-text-strong",
            titleLines === 1 ? "truncate" : "line-clamp-2",
          )}
        >
          {title}
        </p>
        {description && (
          <p className="mt-0.5 text-body2 break-words whitespace-pre-line text-text-sub">
            {description}
          </p>
        )}
      </div>
      {trailing && <div className="shrink-0 text-body2 text-text-sub">{trailing}</div>}
    </Comp>
  );
}

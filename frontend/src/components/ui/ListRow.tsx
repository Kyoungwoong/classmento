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
}

/** 정보 나열의 기본 단위. 카드 대신 이걸 세로로 쌓는다(구분은 여백으로). */
export function ListRow({ leading, title, description, trailing, onClick }: ListRowProps) {
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
        <p className="text-body1 font-medium text-text-strong">{title}</p>
        {description && <p className="mt-0.5 text-body2 text-text-sub">{description}</p>}
      </div>
      {trailing && <div className="shrink-0 text-body2 text-text-sub">{trailing}</div>}
    </Comp>
  );
}

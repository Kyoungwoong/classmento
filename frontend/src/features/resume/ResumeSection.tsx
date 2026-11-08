import type { ReactNode } from "react";

/** 이력서 결과 화면의 섹션: 제목(+개수) / 설명 / 내용. 비어 있으면 "찾지 못했어요". */
export function Section({
  title,
  count,
  description,
  showEmpty = true,
  children,
}: {
  title: string;
  count?: number;
  description?: string;
  /** false면 count가 0이어도 "찾지 못했어요" 대신 children을 그린다(섹션이 빈 상태를 직접 그릴 때) */
  showEmpty?: boolean;
  children: ReactNode;
}) {
  return (
    <section className="mt-10 first-of-type:mt-0">
      <h2 className="text-title2 text-text-strong">
        {title}
        {count !== undefined && count > 0 && <span className="ml-1.5 text-text-hint">{count}</span>}
        {count === 0 && showEmpty && <span className="ml-1.5 text-text-hint">0</span>}
      </h2>
      {description && <p className="mt-1 text-body2 text-text-sub">{description}</p>}
      {count === 0 && showEmpty ? (
        <p className="py-4 text-body2 text-text-hint">찾지 못했어요</p>
      ) : (
        <div className="mt-3">{children}</div>
      )}
    </section>
  );
}

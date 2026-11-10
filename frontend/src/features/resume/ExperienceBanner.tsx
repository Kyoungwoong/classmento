import clsx from "clsx";
import { CheckMark, Chip } from "@/components/ui";
import type { ExperienceView } from "@/lib/api/client";

const TYPE_LABEL: Record<ExperienceView["type"], string> = {
  WORK: "경력",
  PROJECT: "프로젝트",
};

const MAX_SKILLS = 4;

interface ExperienceBannerProps {
  experience: ExperienceView;
  /** 고르는 목록에 쓸 때: 체크 자리를 미리 잡아 고를 때 제목 폭이 흔들리지 않게 */
  selectable?: boolean;
  /** 골랐을 때(연한 연두 면 + 진한 연두 테두리 + 체크) */
  selected?: boolean;
}

/**
 * 경험 하나(경력·프로젝트). 경험 기반 면접에서 고르는 단위(ADR 0005).
 * 노트에 끼운 색인 카드(ADR 0007): 종이 면 + 얇은 선 + 왼쪽 색 띠(경력 연두 · 프로젝트 남색 연한 면). 그림자는 쓰지 않는다.
 */
export function ExperienceBanner({
  experience,
  selectable = false,
  selected = false,
}: ExperienceBannerProps) {
  const { type, period, title, summary, skills } = experience;
  const shown = skills.slice(0, MAX_SKILLS);
  const rest = skills.length - shown.length;
  const sub = selected ? "text-text" : "text-text-sub";

  return (
    <article
      className={clsx(
        "relative overflow-hidden rounded-lg border py-4 pr-5 pl-7 transition-colors duration-100",
        selected ? "border-accent bg-bg-tint" : "border-rule bg-paper",
      )}
    >
      <span
        aria-hidden="true"
        className={clsx(
          "absolute inset-y-0 left-0 w-2",
          type === "WORK" ? "bg-primary" : "bg-navy-200",
        )}
      />
      {/* 연한 연두 면 위 text-sub는 대비 부족(4.4) → 고르면 text-text */}
      <p className={clsx("text-caption", sub)}>
        {[TYPE_LABEL[type], period].filter(Boolean).join(" · ")}
      </p>
      <div className="mt-1 flex items-start gap-3">
        <h3 className="min-w-0 flex-1 text-body1 font-semibold text-text-strong">{title}</h3>
        {selectable && <CheckMark shown={selected} />}
      </div>
      {summary && <p className={clsx("mt-1 line-clamp-2 text-body2", sub)}>{summary}</p>}
      {shown.length > 0 && (
        <ul className="mt-3 flex flex-wrap gap-2" aria-label="사용한 기술">
          {shown.map((s, i) => (
            <li key={`${i}-${s}`}>
              <Chip label={s} size="sm" tone="muted" />
            </li>
          ))}
          {rest > 0 && (
            <li
              className="flex h-7 items-center px-1 text-caption text-text-hint"
              aria-label={`외 ${rest}개`}
            >
              +{rest}
            </li>
          )}
        </ul>
      )}
    </article>
  );
}

import clsx from "clsx";
import { Chip } from "@/components/ui";
import type { ExperienceView } from "@/lib/api/client";

const TYPE_LABEL: Record<ExperienceView["type"], string> = {
  WORK: "경력",
  PROJECT: "프로젝트",
};

const MAX_SKILLS = 4;

interface ExperienceBannerProps {
  experience: ExperienceView;
  /** 경험 기반 면접에서 골랐을 때(연한 블루 면) */
  selected?: boolean;
}

/**
 * 경험 하나(경력·프로젝트). 경험 기반 면접에서 고르는 단위(ADR 0005).
 * 회색 면(bg-bg-muted) 한 장으로 묶고, 테두리·그림자는 쓰지 않는다.
 */
export function ExperienceBanner({ experience, selected = false }: ExperienceBannerProps) {
  const { type, period, title, summary, skills } = experience;
  const shown = skills.slice(0, MAX_SKILLS);
  const rest = skills.length - shown.length;

  return (
    <article
      className={clsx(
        "rounded-lg px-5 py-4 transition-colors duration-100",
        selected ? "bg-primary-weak" : "bg-bg-muted",
      )}
    >
      <p className="text-caption text-text-sub">
        {[TYPE_LABEL[type], period].filter(Boolean).join(" · ")}
      </p>
      <h3 className="mt-1 text-body1 font-semibold text-text-strong">{title}</h3>
      {summary && <p className="mt-1 line-clamp-2 text-body2 text-text-sub">{summary}</p>}
      {shown.length > 0 && (
        <ul className="mt-3 flex flex-wrap gap-2" aria-label="사용한 기술">
          {shown.map((s, i) => (
            <li key={`${i}-${s}`}>
              <Chip label={s} size="sm" tone="surface" />
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

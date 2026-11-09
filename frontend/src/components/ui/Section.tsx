import clsx from "clsx";
import type { ReactNode } from "react";

type Tone = "plain" | "tint" | "muted" | "ink";

const toneClass: Record<Tone, string> = {
  plain: "bg-bg",
  tint: "bg-bg-tint",
  muted: "bg-bg-subtle",
  ink: "bg-ink text-on-ink",
};

interface SectionProps {
  tone?: Tone;
  children: ReactNode;
  className?: string;
  /** 스크린리더용 이름(제목이 없을 때) */
  "aria-label"?: string;
  id?: string;
}

/**
 * 가로 전체 배경 띠 + 가운데 site 폭 본문. 섹션 구분은 테두리 대신 띠 색으로 한다.
 * 한 페이지에서 같은 톤 띠를 연달아 두지 않는다(plain → tint → plain …).
 */
export function Section({ tone = "plain", children, className, ...rest }: SectionProps) {
  return (
    <section className={clsx(toneClass[tone], "py-14 md:py-24", className)} {...rest}>
      <div className="mx-auto w-full max-w-site px-5 md:px-8">{children}</div>
    </section>
  );
}

interface SectionHeadingProps {
  title: ReactNode;
  description?: ReactNode;
  eyebrow?: ReactNode;
}

/** 섹션 제목(h2) + 설명. ink 띠 위에서는 글자색을 물려받는다. */
export function SectionHeading({ title, description, eyebrow }: SectionHeadingProps) {
  return (
    <div className="mb-8 md:mb-12">
      {eyebrow && <p className="mb-2 text-caption font-semibold text-accent">{eyebrow}</p>}
      <h2 className="text-title1 whitespace-pre-line md:text-title1-lg">{title}</h2>
      {description && <p className="mt-3 text-body2 opacity-80 md:text-body1">{description}</p>}
    </div>
  );
}

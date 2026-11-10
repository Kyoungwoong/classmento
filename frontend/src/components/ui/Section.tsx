import clsx from "clsx";
import type { ReactNode } from "react";

export type SectionTone = "plain" | "tint" | "subtle" | "ink";

const toneClass: Record<SectionTone, string> = {
  plain: "bg-bg",
  tint: "bg-bg-tint",
  subtle: "bg-bg-subtle",
  ink: "bg-ink text-on-ink",
};

interface SectionProps {
  tone?: SectionTone;
  children: ReactNode;
  className?: string;
  /** 스크린리더용 이름(제목이 없을 때) */
  "aria-label"?: string;
  /** 띠 제목(`SectionHeading id`)으로 섹션 이름을 붙일 때 */
  "aria-labelledby"?: string;
  id?: string;
  /** 위아래 여백을 줄인다(리포트 머리처럼 작업 화면 맨 위 띠) */
  compact?: boolean;
}

/**
 * 가로 전체 배경 띠 + 가운데 site 폭 본문. 섹션 구분은 테두리 대신 띠 색으로 한다.
 * 한 페이지에서 같은 톤 띠를 연달아 두지 않는다(plain → tint → plain …).
 * tint 띠 안에는 `Panel tone="tint"`를 두지 않는다(면이 사라진다).
 */
export function Section({
  tone = "plain",
  compact = false,
  children,
  className,
  ...rest
}: SectionProps) {
  return (
    <section
      className={clsx(toneClass[tone], compact ? "py-10 md:py-14" : "py-14 md:py-24", className)}
      {...rest}
    >
      <div className="mx-auto w-full max-w-site px-5 md:px-8">{children}</div>
    </section>
  );
}

interface SectionHeadingProps {
  title: ReactNode;
  description?: ReactNode;
  eyebrow?: ReactNode;
  /** 띠 톤. ink(남색) 띠 위에서는 글자를 밝게 바꾼다(대비) */
  tone?: SectionTone;
  /** h2 id — `Section aria-labelledby`로 이어 섹션 이름을 제목만으로 읽히게 */
  id?: string;
  /** 아래 여백을 없앤다(띠 안에서 버튼과 한 줄로 둘 때) */
  flush?: boolean;
}

/** 섹션 제목(h2) + 설명. 띠 톤마다 대비를 지키는 글자색을 쓴다(design-system 대비 표). */
export function SectionHeading({
  id,
  flush = false,
  title,
  description,
  eyebrow,
  tone = "plain",
}: SectionHeadingProps) {
  const onInk = tone === "ink";
  return (
    <div className={flush ? undefined : "mb-8 md:mb-12"}>
      {eyebrow && (
        <p
          className={clsx(
            "mb-2 text-caption font-semibold",
            onInk ? "text-primary" : "text-accent",
          )}
        >
          {eyebrow}
        </p>
      )}
      <h2
        id={id}
        className={clsx(
          "text-title1 whitespace-pre-line md:text-title1-lg",
          onInk ? "text-on-ink" : "text-text-strong",
        )}
      >
        {title}
      </h2>
      {description && (
        <p className={clsx("mt-3 text-body2 md:text-body1", onInk ? "text-navy-100" : "text-text")}>
          {description}
        </p>
      )}
    </div>
  );
}

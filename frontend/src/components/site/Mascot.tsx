import clsx from "clsx";

export type MascotMood = "idle" | "listening" | "thinking" | "happy" | "oops";

interface MascotProps {
  mood?: MascotMood;
  /** 한 변 px(정사각형). 기본 120 */
  size?: number;
  /** 있으면 그림으로 읽힌다(role="img"). 없으면 장식(aria-hidden) */
  label?: string;
  className?: string;
}

/**
 * 면접 친구 "멘토리": 둥근 연두 몸통 + 남색 눈 + 손에 든 마이크.
 * 표정으로 상태를 말한다 — idle(대기) · listening(녹음 중) · thinking(처리 중) · happy(완료) · oops(문제).
 * 화면마다 장식으로 반복하지 않는다(design-system 원칙 7).
 */
export function Mascot({ mood = "idle", size = 120, label, className }: MascotProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 120 120"
      className={clsx("shrink-0", !label && "pointer-events-none", className)}
      {...(label ? { role: "img", "aria-label": label } : { "aria-hidden": true })}
      data-mood={mood}
    >
      {/* 그림자 바닥 */}
      <ellipse cx="58" cy="110" rx="30" ry="4" className="fill-navy-100" />
      {/* 몸통 */}
      <rect x="20" y="22" width="76" height="84" rx="34" className="fill-primary" />
      {/* 더듬이 */}
      <path d="M58 22 V12" strokeWidth="3" strokeLinecap="round" className="stroke-ink" />
      <circle cx="58" cy="10" r="4" className="fill-highlight" />
      {/* 볼 */}
      <ellipse cx="36" cy="70" rx="6" ry="3.5" className="fill-highlight" opacity="0.6" />
      <ellipse cx="80" cy="70" rx="6" ry="3.5" className="fill-highlight" opacity="0.6" />

      <Face mood={mood} />

      {/* 팔 + 마이크 */}
      <path
        d="M92 74 Q102 70 104 60"
        strokeWidth="4"
        strokeLinecap="round"
        fill="none"
        className="stroke-ink"
      />
      <rect x="99" y="40" width="12" height="20" rx="6" className="fill-ink" />
      <path d="M105 60 V66" strokeWidth="3" strokeLinecap="round" className="stroke-ink" />

      {mood === "listening" && (
        <g fill="none" strokeWidth="3" strokeLinecap="round" className="stroke-accent">
          <path d="M114 42 Q119 50 114 58" />
          <path d="M96 36 Q90 50 96 64" opacity="0.5" />
        </g>
      )}
      {mood === "thinking" && (
        <g className="fill-navy-600">
          <circle cx="92" cy="18" r="3" />
          <circle cx="101" cy="12" r="3.5" />
          <circle cx="111" cy="6" r="4" />
        </g>
      )}
      {mood === "happy" && (
        <path
          d="M14 20 l2.5 5.5 5.5 2.5 -5.5 2.5 -2.5 5.5 -2.5 -5.5 -5.5 -2.5 5.5 -2.5z"
          className="fill-highlight"
        />
      )}
      {mood === "oops" && (
        <path d="M90 30 q4 7 0 10 q-4 -3 0 -10z" className="fill-navy-100 stroke-navy-600" />
      )}
    </svg>
  );
}

function Face({ mood }: { mood: MascotMood }) {
  const eye = "fill-ink";
  const line = { fill: "none", strokeWidth: 3.5, strokeLinecap: "round" as const };
  switch (mood) {
    case "happy":
      return (
        <g className="stroke-ink" {...line}>
          <path d="M38 56 Q44 48 50 56" />
          <path d="M66 56 Q72 48 78 56" />
          <path d="M46 72 Q58 84 70 72" />
        </g>
      );
    case "thinking":
      return (
        <g>
          <circle cx="46" cy="52" r="5" className={eye} />
          <circle cx="72" cy="52" r="5" className={eye} />
          <path d="M50 76 H66" className="stroke-ink" {...line} />
        </g>
      );
    case "listening":
      return (
        <g>
          <circle cx="44" cy="56" r="5.5" className={eye} />
          <circle cx="72" cy="56" r="5.5" className={eye} />
          <ellipse cx="58" cy="76" rx="5" ry="6" className="fill-ink" />
        </g>
      );
    case "oops":
      return (
        <g className="stroke-ink" {...line}>
          <path d="M40 52 L48 60 M48 52 L40 60" />
          <path d="M68 52 L76 60 M76 52 L68 60" />
          <path d="M46 78 Q52 72 58 78 Q64 84 70 78" />
        </g>
      );
    default:
      return (
        <g>
          <circle cx="44" cy="56" r="5.5" className={eye} />
          <circle cx="72" cy="56" r="5.5" className={eye} />
          <path d="M50 72 Q58 80 66 72" className="stroke-ink" {...line} />
        </g>
      );
  }
}

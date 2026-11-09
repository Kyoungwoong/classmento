import { ChartColumn, ChevronRight, FileText, MessageCircleQuestion, Mic } from "lucide-react";
import type { ComponentType, SVGProps } from "react";

interface Stage {
  icon: ComponentType<SVGProps<SVGSVGElement> & { size?: number }>;
  step: string;
  title: string;
  note: string;
}

const STAGES: Stage[] = [
  { icon: FileText, step: "01 · 업로드", title: "내 이력서", note: "경험을 하나씩 정리해요" },
  {
    icon: MessageCircleQuestion,
    step: "02 · 질문",
    title: "맞춤 질문",
    note: "경험·기술·인성에서",
  },
  { icon: Mic, step: "03 · 녹음", title: "말로 답하기", note: "질문마다 3분까지" },
  { icon: ChartColumn, step: "04 · 리포트", title: "점수와 피드백", note: "기준별 점수·근거" },
];

/**
 * 히어로 오른쪽 패널의 과정 그림: 이력서 → 질문 → 녹음 → 리포트.
 * 4단계를 한 줄 흐름(화살표)으로 보여준다. 3열 기능 카드 그리드가 아니라 순서가 있는 흐름이다.
 */
export function ProcessIllustration() {
  return (
    <figure aria-labelledby="process-caption">
      <figcaption
        id="process-caption"
        className="mb-5 pr-24 text-caption font-semibold text-text-sub md:pr-28"
      >
        내 이력서가 면접 리포트가 되는 과정
      </figcaption>
      <ol className="grid grid-cols-2 gap-x-3 gap-y-6 md:grid-cols-4 md:gap-x-2">
        {STAGES.map((s, i) => {
          const Icon = s.icon;
          const last = i === STAGES.length - 1;
          return (
            <li key={s.step} className="relative flex flex-col items-center text-center">
              <span
                className={
                  last
                    ? "flex size-16 items-center justify-center rounded-lg bg-primary text-on-primary"
                    : "flex size-16 items-center justify-center rounded-lg border border-border bg-bg text-ink"
                }
              >
                <Icon size={28} aria-hidden />
              </span>
              {!last && (
                <ChevronRight
                  size={18}
                  aria-hidden
                  className="absolute top-6 -right-3 hidden text-text-hint md:block"
                />
              )}
              <span className="mt-3 text-caption font-semibold text-accent">{s.step}</span>
              <span className="mt-1 text-body2 font-semibold text-text-strong">{s.title}</span>
              <span className="mt-0.5 text-caption text-text-sub">{s.note}</span>
            </li>
          );
        })}
      </ol>
    </figure>
  );
}

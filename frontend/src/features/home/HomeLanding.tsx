import { Briefcase, Code, Shuffle, Users } from "lucide-react";
import { ButtonLink, Panel, Section, SectionHeading, StepList, TwoColumn } from "@/components/ui";
import { Mascot } from "@/components/site/Mascot";
import { RecentInterviews } from "@/features/interview/RecentInterviews";
import { ProcessIllustration } from "./ProcessIllustration";

const STEPS = [
  {
    title: "이력서 올리기",
    description:
      "PDF·DOCX를 올리면 경력과 프로젝트를 경험 단위로 정리해요. 이력서 없이도 시작할 수 있어요.",
  },
  {
    title: "면접 고르기",
    description: "경험 기반·기술·인성·혼합 중에서 고르고, 직무와 연차, 질문 수를 정해요.",
  },
  {
    title: "말로 답하기",
    description:
      "질문마다 녹음하고 다시 들어 본 뒤 올려요. 글자로 바꾸고 평가하는 건 그동안 알아서 해요.",
  },
  {
    title: "리포트 받기",
    description: "기준별 점수와 근거, 잘한 점·고칠 점, 이렇게 답해 보라는 예시를 받아요.",
  },
];

const TYPES = [
  {
    icon: Briefcase,
    title: "경험 기반 면접",
    description: "이력서의 경험 하나를 골라, 그 경험에서 나올 기술·CS 질문을 받아요.",
  },
  {
    icon: Code,
    title: "기술 면접",
    description: "Language·Framework·CS 질문 은행에서 직무와 연차에 맞게 골라요.",
  },
  {
    icon: Users,
    title: "인성 면접",
    description: "협업, 갈등, 실패 경험처럼 자주 나오는 인성 질문을 연습해요.",
  },
  {
    icon: Shuffle,
    title: "기술 + 인성",
    description: "실제 면접처럼 기술과 인성 질문을 번갈아 받아요.",
  },
];

/** 홈(/): 히어로 2단 → 준비 흐름(연한 면 띠) → 네 가지 면접(2단) → 마지막 행동(남색 띠). */
export function HomeLanding() {
  return (
    <main>
      <Section tone="plain" className="md:pt-20">
        <TwoColumn
          ratio="even"
          className="md:items-center"
          aside={
            <div>
              <p className="mb-4 inline-flex h-8 items-center rounded-full bg-highlight-weak px-3 text-caption font-semibold text-ink">
                말로 답하는 모의면접
              </p>
              <h1 className="text-display whitespace-pre-line text-ink md:text-hero">
                {"면접은 말로 하니까,\n연습도 말로 해요"}
              </h1>
              <p className="mt-5 text-body1 text-text-sub">
                이력서 한 장으로 나에게 맞는 질문을 받고, 직접 말해 보고, 바로 피드백을 받아요.
              </p>
              <div className="mt-8 flex flex-col gap-2 sm:flex-row">
                <ButtonLink href="/interview/new" className="sm:min-w-48">
                  면접 시작하기
                </ButtonLink>
                <ButtonLink href="/resume/upload" variant="secondary" className="sm:min-w-48">
                  이력서 올리기
                </ButtonLink>
              </div>
            </div>
          }
        >
          <div className="relative">
            <Mascot
              mood="happy"
              size={96}
              className="absolute -top-12 right-4 md:-top-14 md:size-28"
            />
            <Panel>
              <ProcessIllustration />
            </Panel>
          </div>
        </TwoColumn>
        <RecentInterviews />
      </Section>

      <Section tone="tint" aria-labelledby="flow-heading">
        <SectionHeading
          id="flow-heading"
          tone="tint"
          eyebrow="준비 흐름"
          title="준비는 단순하게, 연습은 실전처럼"
          description="네 단계면 충분해요. 처음이면 15분 정도 걸려요."
        />
        <StepList steps={STEPS} onTint />
      </Section>

      <Section tone="plain">
        <TwoColumn
          aside={
            <SectionHeading
              eyebrow="면접 종류"
              title={"네 가지 면접 중에\n골라서 연습해요"}
              description="이력서가 없어도 기술·인성 면접은 바로 볼 수 있어요."
            />
          }
        >
          <ul className="border-t border-border">
            {TYPES.map((t) => {
              const Icon = t.icon;
              return (
                <li key={t.title} className="flex gap-4 border-b border-border py-6">
                  <span className="flex size-12 shrink-0 items-center justify-center rounded-md bg-bg-tint text-accent">
                    <Icon size={24} aria-hidden />
                  </span>
                  <span>
                    <span className="block text-body1 font-semibold text-text-strong">
                      {t.title}
                    </span>
                    <span className="mt-1 block text-body2 text-text-sub">{t.description}</span>
                  </span>
                </li>
              );
            })}
          </ul>
        </TwoColumn>
      </Section>

      <Section tone="ink">
        <div className="flex flex-col items-start gap-6 md:flex-row md:items-center md:justify-between">
          <SectionHeading
            tone="ink"
            flush
            title="오늘은 질문 세 개만 답해 볼까요?"
            description="질문 세 개면 10분 안에 끝나요."
          />
          {/* 히어로에 주요 버튼이 있으니 여기는 보조 모양(주요 버튼은 화면에 하나) */}
          <ButtonLink href="/interview/new" variant="secondary" className="md:min-w-56">
            면접 시작하기
          </ButtonLink>
        </div>
      </Section>
    </main>
  );
}

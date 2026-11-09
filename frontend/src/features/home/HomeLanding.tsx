import clsx from "clsx";
import { ButtonLink } from "@/components/ui";
import {
  CheckList,
  GridPaper,
  Highlight,
  NoteSheet,
  Scribble,
  Stamp,
  StickyNote,
} from "@/components/note";
import { Mascot } from "@/components/site/Mascot";
import { RecentInterviews } from "@/features/interview/RecentInterviews";

const PREP = [
  {
    title: "이력서 올리기",
    description:
      "PDF·DOCX를 올리면 경력과 프로젝트를 경험 단위로 정리해요. 없어도 시작할 수 있어요.",
    done: true,
  },
  {
    title: "면접 고르기",
    description: "경험 기반·기술·인성·혼합 중에서 고르고, 직무와 연차, 질문 수를 정해요.",
  },
  {
    title: "말로 답하기",
    description: "질문마다 녹음하고 다시 들어 본 뒤 올려요. 글자로 바꾸고 평가하는 건 알아서 해요.",
  },
  {
    title: "채점 받기",
    description: "기준별 점수와 근거, 잘한 점·고칠 점, 이렇게 답해 보라는 예시를 받아요.",
  },
];

/** 색인 탭 색: 연두 · 연한 남색 · 연한 연두 · 남색 회색(모두 남색 글자, 노랑은 쓰지 않는다) */
const TYPES = [
  {
    tab: "경험",
    tabClass: "bg-primary text-on-primary",
    title: "경험 기반 면접",
    description: "이력서의 경험 하나를 골라, 그 경험에서 나올 기술·CS 질문을 받아요.",
  },
  {
    tab: "기술",
    tabClass: "bg-navy-100 text-ink",
    title: "기술 면접",
    description: "Language·Framework·CS 질문 은행에서 직무와 연차에 맞게 골라요.",
  },
  {
    tab: "인성",
    tabClass: "bg-lime-200 text-ink",
    title: "인성 면접",
    description: "협업, 갈등, 실패 경험처럼 자주 나오는 인성 질문을 연습해요.",
  },
  {
    tab: "혼합",
    tabClass: "bg-navy-200 text-ink",
    title: "기술 + 인성",
    description: "실제 면접처럼 기술과 인성 질문을 번갈아 받아요.",
  },
];

/** 녹음 파형 막대 높이(장식) */
const WAVE = [
  "h-2",
  "h-4",
  "h-6",
  "h-3",
  "h-7",
  "h-5",
  "h-8",
  "h-4",
  "h-6",
  "h-2",
  "h-5",
  "h-3",
  "h-7",
  "h-4",
  "h-2",
];

/** 히어로 오른쪽: 질문을 적고 말로 답하고 채점받은 노트 한 장(미리보기) */
function NotePreview() {
  return (
    <figure aria-label="면접 노트 미리보기" className="relative mx-auto max-w-md md:rotate-1">
      <Mascot
        mood="happy"
        size={88}
        className="absolute -top-14 -left-4 z-10 md:-top-16 md:-left-10 md:size-28"
      />
      <NoteSheet>
        <p className="text-caption font-semibold text-accent">질문 2</p>
        <p className="mt-1 text-body1 font-semibold text-text-strong md:text-title2">
          최근 프로젝트에서 가장 어려웠던 문제는 무엇이었나요?
        </p>
        <div className="mt-5 flex items-center gap-3 rounded-md bg-bg px-3 py-3 ring-1 ring-rule">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-ink">
            <span className="size-2.5 rounded-full bg-primary" />
          </span>
          <span aria-hidden="true" className="flex h-8 flex-1 items-center gap-1">
            {WAVE.map((h, i) => (
              <span key={i} className={clsx("w-1 rounded-full bg-ink", h)} />
            ))}
          </span>
          <span className="text-caption text-text-sub tabular-nums">0:42</span>
        </div>
        <p className="mt-5 pr-24 text-body2 text-text md:pr-28">
          <span className="font-semibold text-text-strong">잘한 점</span> · 원인부터 결과까지
          순서대로 말했어요.
        </p>
        <Stamp score={4.2} className="absolute -right-3 -bottom-8 bg-paper md:-right-8" />
      </NoteSheet>
    </figure>
  );
}

/** 홈(/): 모눈 표지(문구 + 채점된 노트) → 준비 체크리스트 → 면접 종류 색인 탭 → 칠판. ADR 0007 */
export function HomeLanding() {
  return (
    <main>
      <section aria-labelledby="hero-heading" className="relative isolate bg-paper">
        <GridPaper />
        {/* 2단은 lg부터: md에서 6칸에 hero 글자를 넣으면 4줄로 끊긴다 */}
        <div className="mx-auto grid w-full max-w-site gap-16 px-5 pt-12 pb-20 md:px-8 md:pt-20 md:pb-28 lg:grid-cols-12 lg:items-center lg:gap-12">
          <div className="lg:col-span-6">
            <p className="text-caption font-semibold text-accent">말로 답하는 모의면접</p>
            <h1
              id="hero-heading"
              className="mt-3 text-display whitespace-pre-line text-ink lg:text-hero"
            >
              면접은 말로 하니까,
              <br />
              연습도 <Highlight>말로</Highlight> 해요
            </h1>
            <p className="mt-5 text-body1 text-text">
              이력서 한 장으로 나에게 맞는 질문을 받고, 직접 말해 보고, 노트에 채점을 받아요.
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
          <div className="pt-6 lg:col-span-6 lg:pt-0">
            <NotePreview />
          </div>
        </div>
        <div className="mx-auto w-full max-w-site px-5 pb-16 empty:hidden md:px-8">
          <RecentInterviews />
        </div>
      </section>

      <section aria-labelledby="prep-heading" className="py-16 md:py-24">
        {/* 노트 8칸 + 포스트잇 4칸. 음수 위치로 밖에 붙이지 않는다(좁은 폭에서 가로 넘침) */}
        <div className="mx-auto grid w-full max-w-site gap-8 px-5 md:px-8 lg:grid-cols-12 lg:gap-10">
          <NoteSheet className="lg:col-span-8">
            <h2 id="prep-heading" className="text-title1 text-text-strong md:text-title1-lg">
              오늘의 면접 준비
            </h2>
            <p className="mt-2 text-body2 text-text-sub">네 칸만 채우면 돼요.</p>
            <div className="mt-4">
              <CheckList label="면접 준비 체크리스트" items={PREP} />
            </div>
          </NoteSheet>
          <StickyNote
            title="처음이라면"
            className="mx-auto w-full max-w-64 self-start lg:col-span-4 lg:mx-0 lg:mt-12"
          >
            질문 세 개면 10분 안에 끝나요.
          </StickyNote>
        </div>
      </section>

      <section aria-labelledby="types-heading" className="bg-bg-subtle py-16 md:py-24">
        <div className="mx-auto w-full max-w-site px-5 md:px-8">
          <h2 id="types-heading" className="text-title1 text-text-strong md:text-title1-lg">
            <span className="relative inline-block">
              네 가지 면접
              <Scribble className="absolute inset-x-0 -bottom-3" />
            </span>{" "}
            중에 골라요
          </h2>
          <p className="mt-5 text-body2 text-text md:text-body1">
            이력서가 없어도 기술·인성 면접은 바로 볼 수 있어요.
          </p>
          <ul className="mt-14 grid gap-12 md:grid-cols-2 md:gap-x-6 lg:grid-cols-4">
            {TYPES.map((t) => (
              <li key={t.title} className="relative">
                <span
                  className={clsx(
                    "absolute -top-8 left-0 inline-flex h-8 items-center rounded-t-md px-4 text-caption font-semibold",
                    t.tabClass,
                  )}
                >
                  {t.tab}
                </span>
                <div className="h-full rounded-lg rounded-tl-none border border-rule bg-paper p-5 md:p-6">
                  <h3 className="text-body1 font-semibold text-text-strong md:text-title2">
                    {t.title}
                  </h3>
                  <p className="mt-2 text-body2 text-text-sub">{t.description}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <section aria-labelledby="board-heading" className="bg-bg py-16 md:py-24">
        <div className="mx-auto w-full max-w-site px-5 md:px-8">
          {/* 칠판: 남색 면 + 테두리 + 분필 받침 */}
          <div className="rounded-xl border-8 border-navy-800 bg-ink px-6 pt-10 pb-8 md:px-14 md:pt-14 md:pb-10">
            <div className="flex flex-col items-start gap-8 md:flex-row md:items-end md:justify-between">
              <div>
                <h2 id="board-heading" className="font-hand text-display text-on-ink">
                  오늘은 질문 세 개만 답해 볼까요?
                </h2>
                <p className="mt-4 text-body1 text-navy-100">
                  이력서가 없어도 바로 시작할 수 있어요.
                </p>
              </div>
              {/* 히어로에 주요 버튼이 있으니 여기는 보조 모양(주요 버튼은 화면에 하나) */}
              <ButtonLink href="/interview/new" variant="secondary" className="md:min-w-56">
                면접 시작하기
              </ButtonLink>
            </div>
            <div aria-hidden="true" className="mt-8 flex justify-end gap-3">
              <span className="h-2 w-12 rounded-full bg-on-ink" />
              <span className="h-2 w-8 rounded-full bg-primary" />
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

import Link from "next/link";
import { Logo } from "./Logo";

/** 남색 띠 푸터. 화면에 하단 고정 CTA가 있으면(모바일) 가리지 않게 아래 여백을 넉넉히 둔다. */
export function SiteFooter() {
  return (
    <footer className="bg-ink pt-12 pb-12 text-on-ink group-has-data-bottom-cta/body:pb-36 md:group-has-data-bottom-cta/body:pb-12">
      <div className="mx-auto flex w-full max-w-site flex-col gap-6 px-5 md:flex-row md:items-start md:justify-between md:px-8">
        <div>
          <Logo tone="on-ink" />
          <p className="mt-3 text-body2 text-navy-100">
            말로 답하는 모의면접. 질문부터 피드백까지.
          </p>
        </div>
        <nav aria-label="바닥 메뉴" className="flex gap-6">
          <Link
            href="/interview/new"
            className="inline-flex h-11 items-center text-body2 text-navy-100"
          >
            면접 보기
          </Link>
          <Link
            href="/resume/upload"
            className="inline-flex h-11 items-center text-body2 text-navy-100"
          >
            이력서 올리기
          </Link>
        </nav>
      </div>
      <p className="mx-auto mt-8 w-full max-w-site px-5 text-caption text-navy-100 md:px-8">
        © classmento
      </p>
    </footer>
  );
}

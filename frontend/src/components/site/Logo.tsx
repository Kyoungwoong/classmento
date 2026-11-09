import Link from "next/link";

/** 로고: 연두 말풍선 마크 + 글자. 홈으로 간다. */
export function Logo({ tone = "ink" }: { tone?: "ink" | "on-ink" }) {
  return (
    <Link
      href="/"
      aria-label="classmento 홈"
      className="inline-flex h-11 shrink-0 items-center gap-2"
    >
      <svg width="28" height="28" viewBox="0 0 28 28" aria-hidden>
        <path
          d="M4 9a5 5 0 0 1 5-5h10a5 5 0 0 1 5 5v6a5 5 0 0 1-5 5h-6l-5 4v-4.2A5 5 0 0 1 4 15z"
          className="fill-primary"
        />
        <rect x="10" y="9" width="3" height="6" rx="1.5" className="fill-ink" />
        <rect x="15" y="9" width="3" height="6" rx="1.5" className="fill-ink" />
      </svg>
      <span
        className={
          tone === "ink"
            ? "text-title2 font-extrabold tracking-tight text-ink"
            : "text-title2 font-extrabold tracking-tight text-on-ink"
        }
      >
        classmento
      </span>
    </Link>
  );
}

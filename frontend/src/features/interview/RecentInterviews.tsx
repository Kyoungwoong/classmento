"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { api, type InterviewSummary } from "@/lib/api/client";
import { TYPE_LABEL } from "./labels";

const SHOWN = 3;

function date(iso: string) {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/** 홈의 "최근 면접"(로그인했고 면접이 있을 때만). 진행 중이면 이어서, 끝났으면 리포트로. */
export function RecentInterviews() {
  const [list, setList] = useState<InterviewSummary[]>([]);

  useEffect(() => {
    api
      .interviews()
      .then(setList)
      .catch(() => setList([])); // 로그인 전(401) 등은 조용히 숨긴다
  }, []);

  if (list.length === 0) return null;

  return (
    <section className="mt-10">
      <h2 className="text-body1 font-semibold text-text-strong">최근 면접</h2>
      <ul className="mt-2">
        {list.slice(0, SHOWN).map((s) => {
          const inProgress = s.status === "IN_PROGRESS";
          return (
            <li key={s.id}>
              <Link
                href={inProgress ? `/interview/${s.id}` : `/interview/${s.id}/report`}
                className="-mx-2 flex items-center gap-4 rounded-sm px-2 py-4 active:bg-bg-subtle"
              >
                <span className="min-w-0 flex-1">
                  <span className="block text-body1 font-medium text-text-strong">
                    {TYPE_LABEL[s.type]}
                  </span>
                  <span className="mt-0.5 block text-body2 text-text-sub">
                    {`${date(s.createdAt)} · 질문 ${s.questionCount}개`}
                  </span>
                </span>
                <span className="shrink-0 text-body2 text-primary">
                  {inProgress ? "이어서 하기" : "리포트 보기"}
                </span>
              </Link>
            </li>
          );
        })}
      </ul>
    </section>
  );
}

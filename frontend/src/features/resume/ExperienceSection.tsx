"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui";
import { api, type ExperienceView } from "@/lib/api/client";
import { ExperienceBanner } from "./ExperienceBanner";
import { Section } from "./ResumeSection";

/**
 * 경험(경력·프로젝트 하나씩) 배너 목록. 경험 기반 면접에서 고르는 단위(ADR 0005).
 * 이전 버전으로 정리된 이력서는 경험이 비어 있을 수 있어, 다시 나누는 행동을 준다.
 */
export function ExperienceSection({
  resumeId,
  version,
  onResync,
}: {
  resumeId: number;
  /** 바뀌면 다시 불러온다(예: 부모가 결과를 저장해 resume.parsed가 새 객체가 됨). */
  version: unknown;
  onResync: () => Promise<unknown>;
}) {
  const [items, setItems] = useState<ExperienceView[] | null>(null);
  const [error, setError] = useState<"load" | "resync" | null>(null);
  const [resyncing, setResyncing] = useState(false);
  const [resynced, setResynced] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    api
      .resumeExperiences(resumeId)
      .then((list) => {
        if (cancelled) return;
        setItems(list);
        setError(null);
      })
      .catch(() => !cancelled && setError("load"));
    return () => {
      cancelled = true;
    };
  }, [resumeId, version, reloadKey]);

  async function resync() {
    setResyncing(true);
    setError(null);
    try {
      await onResync();
      // 새 목록을 스스로 다시 불러온다(부모가 version을 바꾸든 말든). 그 전엔 "불러오는 중"으로 둬 거짓 "찾지 못했어요"를 막는다
      setItems(null);
      setResynced(true);
      setReloadKey((k) => k + 1);
    } catch {
      setError("resync");
    } finally {
      setResyncing(false);
    }
  }

  function reload() {
    setError(null);
    setItems(null);
    setReloadKey((k) => k + 1);
  }

  const retry = error === "resync" ? resync : reload;

  return (
    <Section
      title="경험"
      count={items?.length}
      description="곧 경험 하나를 골라 그 경험으로 면접을 볼 수 있어요."
      showEmpty={false}
    >
      {error ? (
        <div role="alert">
          <p className="text-body2 text-danger">
            {error === "load"
              ? "경험을 불러오지 못했어요."
              : "경험으로 나누지 못했어요. 다시 시도해 주세요."}
          </p>
          <Button variant="secondary" size="md" className="mt-3" onClick={retry}>
            다시 시도하기
          </Button>
        </div>
      ) : items === null ? (
        <p className="text-body2 text-text-hint">경험을 불러오고 있어요</p>
      ) : items.length === 0 ? (
        <div className="rounded-lg bg-bg-muted px-5 py-4">
          <p className="text-body2 text-text">
            {resynced
              ? "이력서에서 경력·프로젝트를 찾지 못했어요."
              : "이 이력서는 경험별로 아직 정리하지 않았어요."}
          </p>
          {!resynced && (
            <Button
              variant="secondary"
              size="md"
              className="mt-3"
              onClick={resync}
              loading={resyncing}
              loadingText="나누고 있어요"
            >
              경험으로 나누기
            </Button>
          )}
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {items.map((e) => (
            <li key={e.id}>
              <ExperienceBanner experience={e} />
            </li>
          ))}
        </ul>
      )}
    </Section>
  );
}

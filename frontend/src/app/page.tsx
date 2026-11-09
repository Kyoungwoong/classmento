import { BottomCTA, ButtonLink, ListRow, Screen, ScreenTitle } from "@/components/ui";
import { RecentInterviews } from "@/features/interview/RecentInterviews";

const steps = [
  {
    title: "이력서 올리기",
    description: "경력과 프로젝트를 자동으로 정리해요 (PDF·DOCX, 20MB까지)",
  },
  { title: "질문에 말로 답하기", description: "내 경험과 직무에 맞는 기술·인성 질문이에요" },
  { title: "답변별 피드백 받기", description: "잘한 점과 고칠 점을 바로 알려드려요" },
];

export default function Home() {
  return (
    <Screen
      cta={
        <BottomCTA>
          {/* 로그인 안 했으면 proxy.ts가 /login?next=<경로> 로 보낸다 */}
          <ButtonLink href="/interview/new" variant="secondary">
            바로 면접 보기
          </ButtonLink>
          <ButtonLink href="/resume/upload">이력서 올리기</ButtonLink>
        </BottomCTA>
      }
    >
      <ScreenTitle
        title={"이력서 한 장으로\n실전 면접을 준비해요"}
        description="질문부터 피드백까지 15분이면 충분해요"
      />
      <ol>
        {steps.map((step, i) => (
          <li key={step.title}>
            <ListRow
              leading={
                <span
                  aria-hidden
                  className="flex size-8 items-center justify-center rounded-full bg-primary-weak text-body2 font-semibold text-primary"
                >
                  {i + 1}
                </span>
              }
              title={step.title}
              description={step.description}
            />
          </li>
        ))}
      </ol>
      {/* 로그인했고 면접이 있을 때만 보인다 */}
      <RecentInterviews />
    </Screen>
  );
}

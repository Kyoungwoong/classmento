import { BottomCTA, Button, ListRow, Screen, ScreenTitle } from "@/components/ui";

const steps = [
  { title: "이력서 올리기", description: "경력과 프로젝트를 자동으로 정리해요" },
  { title: "질문에 말로 답하기", description: "내 이력서에서 나온 기술·인성 질문이에요" },
  { title: "답변별 피드백 받기", description: "잘한 점과 고칠 점을 바로 알려드려요" },
];

export default function Home() {
  return (
    <Screen
      cta={
        <BottomCTA caption="PDF, DOCX 파일 · 최대 20MB">
          <Button>이력서 올리기</Button>
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
    </Screen>
  );
}

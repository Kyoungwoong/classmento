import "@testing-library/jest-dom/vitest";
import { cleanup, configure } from "@testing-library/react";
import { afterEach } from "vitest";

// vitest globals를 쓰지 않으므로 Testing Library 자동 정리가 동작하지 않는다 → 테스트마다 화면을 비운다
afterEach(() => cleanup());

// 기본 1초는 개발 서버·백엔드를 함께 띄운 PC나 CI에서 가끔 모자라 무작위로 실패한다 → 3초
configure({ asyncUtilTimeout: 3000 });

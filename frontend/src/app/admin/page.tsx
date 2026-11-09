import type { Metadata } from "next";
import { AdminMenuScreen } from "@/features/admin/AdminMenuScreen";

export const metadata: Metadata = { title: "관리자 · 클래스멘토" };

export default function AdminPage() {
  return <AdminMenuScreen />;
}

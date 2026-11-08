import type { Metadata } from "next";
import { AdminUsersScreen } from "@/features/admin/AdminUsersScreen";

export const metadata: Metadata = { title: "관리자 · 클래스멘토" };

export default function AdminPage() {
  return <AdminUsersScreen />;
}

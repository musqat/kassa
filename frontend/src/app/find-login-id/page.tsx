"use client";

import { EmailRequestForm } from "@/components/form/EmailRequestForm";
import { findLoginId } from "@/lib/api";

export default function FindLoginIdPage() {
  return (
    <EmailRequestForm
      title="아이디 찾기"
      hint="가입할 때 쓴 이메일로 아이디를 보냅니다"
      doneMessage="가입된 이메일이면 아이디를 보냈습니다. 메일함을 확인해 주세요."
      submitLabel="아이디 받기"
      action={findLoginId}
    />
  );
}

"use client";

import { EmailRequestForm } from "@/components/form/EmailRequestForm";
import { requestPasswordReset } from "@/lib/api";

export default function ForgotPasswordPage() {
  return (
    <EmailRequestForm
      title="비밀번호 찾기"
      hint="가입할 때 쓴 이메일로 재설정 링크를 보냅니다"
      doneMessage="가입된 이메일이면 재설정 링크를 보냈습니다. 링크는 30분 동안 쓸 수 있습니다."
      submitLabel="재설정 링크 받기"
      action={requestPasswordReset}
    />
  );
}

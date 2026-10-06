"use client";

// 서버에서 데이터를 받다 실패하면 보인다. 백엔드가 아직 켜지는 중일 때가 많다
export default function Error({
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-4 px-5 py-14">
      <div className="flex flex-col gap-2">
        <p className="text-sm">화면을 불러오는 데 실패했습니다</p>
        <p className="text-muted text-xs">
          서버를 준비하는 중일 수 있습니다. 잠시 뒤 다시 시도해 주세요
        </p>
      </div>
      <button
        type="button"
        onClick={() => retry()}
        className="bg-ink h-11 rounded-[var(--radius-field)] px-6 text-sm text-white"
      >
        다시 시도
      </button>
    </main>
  );
}

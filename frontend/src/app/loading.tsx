// 서버에서 데이터를 받는 동안 보인다. 백엔드가 쉬고 있었다면 켜지는 데 40초쯤 걸린다
export default function Loading() {
  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-2 px-5 py-14">
      <p className="text-sm">불러오고 있습니다</p>
      <p className="text-muted text-xs">
        서버가 쉬고 있었다면 켜지는 데 40초쯤 걸립니다. 잠시만 기다려 주세요
      </p>
    </main>
  );
}

export function formatPrice(won: number): string {
  return `${won.toLocaleString("ko-KR")}원`;
}

// 서버가 주는 값은 UTC 기준 ISO 문자열이다. 보는 사람 시간대로 바꿔 보여준다
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

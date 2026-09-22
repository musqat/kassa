export function formatPrice(won: number): string {
  return `${won.toLocaleString("ko-KR")}원`;
}

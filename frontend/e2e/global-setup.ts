import { API_BASE_URL, MAILPIT_BASE_URL } from "./helpers/env";

// 백엔드와 메일핏은 여기서 띄우지 않는다. 안 떠 있으면 무엇을 실행해야 하는지 알리고 멈춘다
export default async function globalSetup() {
  await ensureUp("백엔드", `${API_BASE_URL}/actuator/health`, "backend 에서 ./gradlew bootRun");
  await ensureUp("메일핏", `${MAILPIT_BASE_URL}/api/v1/info`, "docker compose up -d");
}

async function ensureUp(name: string, url: string, howTo: string) {
  const reachable = await fetch(url)
    .then((res) => res.ok)
    .catch(() => false);

  if (!reachable) {
    throw new Error(`${name}(${url})에 닿지 못했습니다. ${howTo} 로 띄우고 다시 실행하세요`);
  }
}

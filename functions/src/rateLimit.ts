import { HttpsError } from "firebase-functions/v2/https";

const MAX_REQUESTS_PER_WINDOW = 10;
const WINDOW_MS = 60 * 1000; // 1 minute

const rateLimitMap = new Map<string, { count: number; resetAt: number }>();

export function checkRateLimit(ip: string): void {
  const now = Date.now();
  const entry = rateLimitMap.get(ip);

  if (!entry || now >= entry.resetAt) {
    rateLimitMap.set(ip, { count: 1, resetAt: now + WINDOW_MS });
    return;
  }

  entry.count++;
  if (entry.count > MAX_REQUESTS_PER_WINDOW) {
    throw new HttpsError(
      "resource-exhausted",
      "RATE_LIMITED: Too many requests. Try again later."
    );
  }
}

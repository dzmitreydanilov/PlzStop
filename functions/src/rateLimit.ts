import { HttpsError } from "firebase-functions/v2/https";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { createHash } from "crypto";
import { initializeApp, getApps } from "firebase-admin/app";

if (getApps().length === 0) {
  initializeApp();
}

const COLLECTION = "rateLimits";
const GLOBAL_DOC = "globalDailyCounter";
const MAX_PER_MINUTE = 10;
const WINDOW_MS = 60_000;
const MAX_PER_DAY = 3;
const MAX_GLOBAL_PER_DAY = 500;

function hashIp(ip: string): string {
  return createHash("sha256").update(ip).digest("hex");
}

function getNextMidnightUtc(): Timestamp {
  const now = new Date();
  const midnight = new Date(
    Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1)
  );
  return Timestamp.fromDate(midnight);
}

export async function checkRateLimit(ip: string): Promise<void> {
  const db = getFirestore();
  const docRef = db.collection(COLLECTION).doc(hashIp(ip));

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(docRef);
    const now = Timestamp.now();

    if (!snap.exists) {
      tx.set(docRef, {
        count: 1,
        resetAt: Timestamp.fromMillis(now.toMillis() + WINDOW_MS),
        dailyCount: 1,
        dailyResetAt: getNextMidnightUtc(),
      });
      return;
    }

    const data = snap.data()!;
    let count: number = data.count;
    let resetAt: Timestamp = data.resetAt;
    let dailyCount: number = data.dailyCount;
    let dailyResetAt: Timestamp = data.dailyResetAt;

    // Reset minute window if expired
    if (now.toMillis() >= resetAt.toMillis()) {
      count = 0;
      resetAt = Timestamp.fromMillis(now.toMillis() + WINDOW_MS);
    }

    // Reset daily window if expired
    if (now.toMillis() >= dailyResetAt.toMillis()) {
      dailyCount = 0;
      dailyResetAt = getNextMidnightUtc();
    }

    count++;
    dailyCount++;

    if (count > MAX_PER_MINUTE) {
      throw new HttpsError(
        "resource-exhausted",
        "Too many requests. Try again in a minute."
      );
    }

    if (dailyCount > MAX_PER_DAY) {
      throw new HttpsError(
        "resource-exhausted",
        "Daily limit reached. Try again tomorrow."
      );
    }

    tx.update(docRef, { count, resetAt, dailyCount, dailyResetAt });
  });
}

export async function checkGlobalDailyLimit(): Promise<void> {
  const db = getFirestore();
  const docRef = db.collection(COLLECTION).doc(GLOBAL_DOC);

  await db.runTransaction(async (tx) => {
    const snap = await tx.get(docRef);
    const now = Timestamp.now();

    if (!snap.exists) {
      tx.set(docRef, {
        dailyCount: 1,
        dailyResetAt: getNextMidnightUtc(),
      });
      return;
    }

    const data = snap.data()!;
    let dailyCount: number = data.dailyCount;
    let dailyResetAt: Timestamp = data.dailyResetAt;

    if (now.toMillis() >= dailyResetAt.toMillis()) {
      dailyCount = 0;
      dailyResetAt = getNextMidnightUtc();
    }

    dailyCount++;

    if (dailyCount > MAX_GLOBAL_PER_DAY) {
      throw new HttpsError(
        "resource-exhausted",
        "Service daily limit reached. Try again tomorrow."
      );
    }

    tx.update(docRef, { dailyCount, dailyResetAt });
  });
}

export async function cleanupExpiredRateLimits(): Promise<number> {
  const db = getFirestore();
  const now = Timestamp.now();
  const query = db
    .collection(COLLECTION)
    .where("dailyResetAt", "<", now)
    .limit(500);

  let totalDeleted = 0;
  let batch = db.batch();
  let batchSize = 0;

  // Paginate to handle large collections
  let snapshot = await query.get();
  while (!snapshot.empty) {
    for (const doc of snapshot.docs) {
      batch.delete(doc.ref);
      batchSize++;
      if (batchSize >= 500) {
        await batch.commit();
        totalDeleted += batchSize;
        batch = db.batch();
        batchSize = 0;
      }
    }
    if (batchSize > 0) {
      await batch.commit();
      totalDeleted += batchSize;
      batch = db.batch();
      batchSize = 0;
    }
    snapshot = await query.get();
  }

  return totalDeleted;
}

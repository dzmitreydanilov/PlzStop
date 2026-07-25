import * as functions from "firebase-functions/v1";
import { getFirestore } from "firebase-admin/firestore";
import {
  createDecipheriv,
  createHmac,
  timingSafeEqual,
} from "crypto";

const GOOGLE_OAUTH_COLLECTION = "googleOAuthAccounts";
const ENCRYPTED_REFRESH_TOKEN_FIELD = "encryptedRefreshToken";
const GOOGLE_REVOKE_URI = "https://oauth2.googleapis.com/revoke";

export const cleanupGoogleOAuthOnUserDelete = functions
  .runWith({ secrets: ["GOOGLE_TOKEN_ENCRYPTION_KEY"] })
  .region("europe-west1")
  .auth
  .user()
  .onDelete(async (user) => {
    const document = getFirestore()
      .collection(GOOGLE_OAUTH_COLLECTION)
      .doc(user.uid);
    const snapshot = await document.get();
    try {
      const encryptedToken = snapshot.data()?.[ENCRYPTED_REFRESH_TOKEN_FIELD];
      const encryptionKey = process.env.GOOGLE_TOKEN_ENCRYPTION_KEY;
      if (typeof encryptedToken === "string" && encryptionKey) {
        const refreshToken = decryptFernetToken(encryptedToken, encryptionKey);
        const response = await fetch(GOOGLE_REVOKE_URI, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: new URLSearchParams({ token: refreshToken }),
        });
        if (!response.ok) {
          console.warn(
            "Google authorization revocation returned status",
            response.status
          );
        }
      }
    } catch {
      console.warn("Google authorization revocation failed during user cleanup");
    } finally {
      await document.delete();
    }
  });

export function decryptFernetToken(token: string, encodedKey: string): string {
  const key = Buffer.from(padBase64(encodedKey), "base64url");
  const payload = Buffer.from(padBase64(token), "base64url");
  if (key.length !== 32 || payload.length < 73 || payload[0] !== 0x80) {
    throw new Error("Invalid Fernet token");
  }

  const signedPayload = payload.subarray(0, payload.length - 32);
  const expectedSignature = payload.subarray(payload.length - 32);
  const actualSignature = createHmac("sha256", key.subarray(0, 16))
    .update(signedPayload)
    .digest();
  if (!timingSafeEqual(actualSignature, expectedSignature)) {
    throw new Error("Invalid Fernet signature");
  }

  const iv = payload.subarray(9, 25);
  const ciphertext = payload.subarray(25, payload.length - 32);
  const decipher = createDecipheriv("aes-128-cbc", key.subarray(16), iv);
  return Buffer.concat([
    decipher.update(ciphertext),
    decipher.final(),
  ]).toString("utf8");
}

function padBase64(value: string): string {
  return value.padEnd(value.length + ((4 - (value.length % 4)) % 4), "=");
}

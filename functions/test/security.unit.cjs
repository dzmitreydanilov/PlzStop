const assert = require("node:assert/strict");
const { test } = require("node:test");
const { createCipheriv, createHmac } = require("node:crypto");

const {
  parseResponse,
  validateResponseData,
} = require("../lib/receipt");
const { validateAndSanitize } = require("../lib/validation");
const { decryptFernetToken } = require("../lib/userCleanup");

test("receipt validation rejects duplicate category IDs", () => {
  assert.throws(() =>
    validateAndSanitize({
      imageBase64: "AAAA",
      categories: [
        { id: 1, name: "Food" },
        { id: 1, name: "Transport" },
      ],
    })
  );
});

test("receipt output is bounded and enforces subcategory parent", () => {
  const parsed = parseResponse(
    JSON.stringify({
      status: "success",
      data: {
        merchantName: "A".repeat(300),
        totalAmount: 12.5,
        currency: "not-a-currency",
        date: "not-a-date",
        items: [
          {
            name: "Coffee",
            amount: 4.5,
            categoryId: 1,
            subcategoryId: 20,
          },
        ],
      },
    })
  );

  const validated = validateResponseData(
    parsed,
    new Set([1]),
    new Map([[20, 2]])
  );

  assert.equal(validated.data.merchantName.length, 200);
  assert.equal(validated.data.currency, null);
  assert.equal(validated.data.date, null);
  assert.equal(validated.data.items[0].subcategoryId, null);
});

test("Fernet ciphertext written by Python can be decrypted for cleanup", () => {
  const key = Buffer.from(Array.from({ length: 32 }, (_, index) => index + 1));
  const iv = Buffer.from(Array.from({ length: 16 }, (_, index) => index + 16));
  const version = Buffer.from([0x80]);
  const timestamp = Buffer.alloc(8);
  timestamp.writeBigUInt64BE(1n);
  const cipher = createCipheriv("aes-128-cbc", key.subarray(16), iv);
  const ciphertext = Buffer.concat([
    cipher.update(Buffer.from("refresh-token")),
    cipher.final(),
  ]);
  const signedPayload = Buffer.concat([version, timestamp, iv, ciphertext]);
  const signature = createHmac("sha256", key.subarray(0, 16))
    .update(signedPayload)
    .digest();
  const token = Buffer.concat([signedPayload, signature]).toString("base64url");

  assert.equal(
    decryptFernetToken(token, key.toString("base64url")),
    "refresh-token"
  );
});

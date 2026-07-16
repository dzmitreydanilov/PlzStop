const { after, before, test } = require("node:test");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const {
  assertFails,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const { doc, getDoc, setDoc } = require("firebase/firestore");

const PROJECT_ID = "demo-plzstop-token-rules";
const RULES_PATH = resolve(__dirname, "../../firestore.rules");

let testEnvironment;

before(async () => {
  testEnvironment = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync(RULES_PATH, "utf8"),
    },
  });
});

after(async () => {
  await testEnvironment.cleanup();
});

test("unauthenticated clients cannot read Google OAuth records", async () => {
  const database = testEnvironment.unauthenticatedContext().firestore();

  await assertFails(getDoc(doc(database, "googleOAuthAccounts/alice")));
});

test("authenticated clients cannot read their own Google OAuth record", async () => {
  const database = testEnvironment.authenticatedContext("alice").firestore();

  await assertFails(getDoc(doc(database, "googleOAuthAccounts/alice")));
});

test("authenticated clients cannot read another user's Google OAuth record", async () => {
  const database = testEnvironment.authenticatedContext("alice").firestore();

  await assertFails(getDoc(doc(database, "googleOAuthAccounts/bob")));
});

test("authenticated clients cannot write Google OAuth records", async () => {
  const database = testEnvironment.authenticatedContext("alice").firestore();

  await assertFails(
    setDoc(doc(database, "googleOAuthAccounts/alice"), {
      encryptedRefreshToken: "must-not-be-client-writable",
    }),
  );
});

const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-blockcru-9c7b304d-ba26-4cf3-bd77-606b95d8b7b4";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProgressPayload(uid, now) {
  return {
    userId: uid,
    displayName: "Pemain Alice",
    classicBestScore: 9419,
    classicBestCombo: 33,
    highestAdventureLevel: 1,
    completedLevels: [],
    totalStars: 0,
    totalLinesCleared: 42,
    soundEnabled: true,
    hapticsEnabled: true,
    welcomeLetterSeen: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validMatchPayload(uid, now) {
  return {
    userId: uid,
    mode: "classic",
    levelNumber: 0,
    score: 9419,
    bestCombo: 33,
    linesCleared: 42,
    won: false,
    createdAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user progress or matches", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProgressPayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("matches").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("matches").doc("m1").set(validMatchPayload(ALICE_UID, now))
  );
});

test("Owner can create, read, and partially update PlayerProgress while preserving immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProgressPayload(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      classicBestScore: 12050,
      classicBestCombo: 40,
      highestAdventureLevel: 2,
      completedLevels: [1],
      totalStars: 3,
      welcomeLetterSeen: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt overwrite
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      classicBestScore: 99999,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Owner can create and list MatchRecord in subcollection, updates are denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1").set(validMatchPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").get()
  );

  // Updates to immutable match records must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_1").update({
      score: 999999,
    })
  );

  // Invalid mode must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("matches").doc("match_2").set({
      ...validMatchPayload(ALICE_UID, now),
      mode: "hacked_mode",
    })
  );
});

test("Cross-user reads, writes, and user collection listing are denied", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(bobDb.collection("users").doc(BOB_UID).set(validProgressPayload(BOB_UID, now)));
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("matches").doc("bob_m1").set(validMatchPayload(BOB_UID, now))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertFails(aliceDb.collection("users").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ classicBestScore: 0, updatedAt: now }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("matches").get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validProgressPayload(BOB_UID, now))
  );
});

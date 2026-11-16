const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const ts = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function validProfile(uid) {
  return {
    userId: uid,
    callsign: "VanguardPrime",
    activeUniverseId: "sector_zero",
    activeMode: "zombies",
    activeRigId: "rig_operator",
    consolePreset: "universal_web",
    totalEssence: 1200,
    highScore: 4500,
    highestWave: 7,
    osWallpaperIndex: 0,
    createdAt: ts(),
    updatedAt: ts(),
  };
}

function validArmoryItem(uid) {
  return {
    userId: uid,
    name: "Juggernaut Core",
    category: "perk",
    universeTag: "Sector Zero",
    description: "Boosts armor plating and shield capacity.",
    tier: 1,
    maxTier: 5,
    cooldownSec: 0,
    powerValue: 150,
    accentColorHex: "#FF3D71",
    equipped: true,
    isCustom: false,
    createdAt: ts(),
    updatedAt: ts(),
  };
}

function validRigAsset(uid) {
  return {
    userId: uid,
    name: "Bio-Conduit Operator",
    archetype: "biomorph",
    activeClip: "idle",
    ikEnabled: true,
    rigScale: 1.0,
    playbackSpeed: 1.0,
    poseAngles: { torso: 5.0, lUpperArm: -25.0, rUpperArm: 28.0 },
    keyframeFrames: [0, 15, 30, 45, 60],
    isCompleted: true,
    createdAt: ts(),
    updatedAt: ts(),
  };
}

test("Unauthenticated user cannot read or write any user collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("armoryItems").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("customRigs").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Owner can create and update their OperatorProfile, cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      callsign: "QuantumWalker",
      updatedAt: ts(),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      callsign: "Hacked",
      updatedAt: ts(),
    })
  );
});

test("Shadow update with ghost field on OperatorProfile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: ts(),
    })
  );
});

test("Owner can CRUD ArmoryItem and RigAsset; invalid bounds are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("armoryItems").doc("perk_1").set(validArmoryItem(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customRigs").doc("rig_1").set(validRigAsset(ALICE_UID))
  );

  // Tier > maxTier fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("armoryItems").doc("perk_bad").set({
      ...validArmoryItem(ALICE_UID),
      tier: 5,
      maxTier: 3,
    })
  );

  // Cross-user list on subcollection fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("customRigs").get());
});

test("MatchRecord is append-only (update denied) and enforces owner isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const matchRef = aliceDb.collection("users").doc(ALICE_UID).collection("matchLogs").doc("m_001");

  await assertSucceeds(
    matchRef.set({
      userId: ALICE_UID,
      universeId: "sector_zero",
      mode: "dmz",
      score: 3200,
      waveReached: 4,
      eliminations: 28,
      extracted: true,
      durationSec: 195,
      createdAt: ts(),
    })
  );

  await assertFails(matchRef.update({ score: 999999 }));
});

test("LinkedAccount enforces owner isolation and valid providerId enum", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const linkRef = aliceDb.collection("users").doc(ALICE_UID).collection("linkedAccounts").doc("steam");

  await assertSucceeds(
    linkRef.set({
      userId: ALICE_UID,
      providerId: "steam",
      displayName: "Steam 76561198000000001",
      externalSubject: "76561198000000001",
      linkedAt: ts(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("linkedAccounts").doc("psn").set({
      userId: ALICE_UID,
      providerId: "psn",
      displayName: "PSN via Epic Bridge",
      externalSubject: "psn_998877",
      linkedAt: ts(),
    })
  );

  // Invalid providerId rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("linkedAccounts").doc("unknown_fake").set({
      userId: ALICE_UID,
      providerId: "unknown_fake",
      displayName: "Fake",
      externalSubject: "123",
      linkedAt: ts(),
    })
  );

  // Cross-user read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("linkedAccounts").get());
});


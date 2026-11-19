const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_aicsm_123";
const BOB_UID = "bob_aicsm_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: denied all reads and writes across users, records, and bookmarks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("records").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@aicsm.com",
      role: "STUDENT",
      preferredPortalMode: "IN_APP",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("UserProfile: owner create, partial update, cross-user denial, and immutable field protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Valid create matching Kotlin UserProfile.toCreateMap()
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sharma",
      email: "alice@aicsm.com",
      role: "STUDENT",
      preferredPortalMode: "IN_APP",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update with optional centerName & city
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "FRANCHISE_APPLICANT",
      centerName: "Apex Digital Academy",
      city: "Jaipur",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost/admin role injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject immutable email mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      email: "spoof@aicsm.com",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("PortalRecord: owner CRUD, terminal ARCHIVED lock, URL validation, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Valid create mirroring Kotlin PortalRecord payload (no @DocumentId id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").set({
      title: "Submit Center Franchise Enquiry",
      category: "FRANCHISE_ENQUIRY",
      status: "DRAFT",
      details: "Prepared lab floor plan for 20 computers.",
      linkedUrl: "https://aicsm.com/CustomerEnquiry.php",
      createdAt: created,
      updatedAt: created,
    })
  );

  // List query matching Kotlin observeRecords(uid)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").get()
  );

  // Reject invalid non-AICSM URL
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_bad_url").set({
      title: "Phishing link",
      category: "COURSE_NOTE",
      status: "DRAFT",
      details: "Bad URL",
      linkedUrl: "https://evil.example.com/phish",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject extra @DocumentId 'id' field in write payload
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_extra_id").set({
      id: "rec_extra_id",
      title: "Invalid ID field",
      category: "COURSE_NOTE",
      status: "DRAFT",
      details: "Has id key",
      linkedUrl: "https://www.aicsm.com",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update to SUBMITTED succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").update({
      status: "SUBMITTED",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Archive (terminal state) succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").update({
      status: "ARCHIVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Subsequent update after ARCHIVED is blocked by terminal state lock
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").update({
      title: "Mutate archived record",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user list/read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("records").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").delete());

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").delete()
  );
});

test("PortalBookmark: owner create, list, delete, and future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").set({
      title: "AICSM Franchise Enquiry",
      url: "https://aicsm.com/CustomerEnquiry.php",
      category: "FRANCHISE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_future").set({
      title: "Tampered Time",
      url: "https://www.aicsm.com",
      category: "PORTAL",
      createdAt: new Date(Date.now() + 120000),
      updatedAt: new Date(Date.now() + 120000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("bm_1").delete()
  );
});

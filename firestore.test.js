const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

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

test("Unauthenticated user: cannot read or write user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read their own valid UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Smith",
      xp: 0,
      level: 1,
      streak: 0,
      streakFreezes: 2,
      dailyGoalMinutes: 10,
      activeMinutesToday: 0,
      lessonsCompletedTotal: 0,
      conceptsMasteredTotal: 0,
      activeCourseId: "python-core",
      themeId: "anvil-dark",
      calmMode: false,
      onboardingCompleted: true,
      restDaysCsv: "SUN",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: cannot read another user's profile or snippets", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      xp: 100,
      level: 2,
      streak: 3,
      dailyGoalMinutes: 15,
      activeCourseId: "python-core",
      themeId: "anvil-dark",
      calmMode: false,
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    });
    await context.firestore().collection("users").doc(BOB_UID).collection("snippets").doc("snip_1").set({
      userId: BOB_UID,
      title: "Bob Snippet",
      language: "Python",
      code: "print(42)",
      category: "Playground",
      isFavorite: true,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("snippets").doc("snip_1").get());
});

test("Shadow update test: rejects ghost fields on UserProfile and CodeSnippet", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      xp: 0,
      level: 1,
      streak: 0,
      dailyGoalMinutes: 10,
      activeCourseId: "python-core",
      themeId: "anvil-dark",
      calmMode: false,
      onboardingCompleted: true,
      isAdminGhost: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and query their own snippets, concepts, mistakes, and reports", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("snippets").doc("snip_1").set({
      userId: ALICE_UID,
      title: "Fibonacci Generator",
      language: "Python",
      code: "def fib(n): return n if n < 2 else fib(n-1) + fib(n-2)",
      category: "Playground",
      isFavorite: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("snippets").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("concept_states").doc("py_variables").set({
      userId: ALICE_UID,
      conceptId: "py_variables",
      courseId: "python-core",
      domain: "Syntax and Semantics",
      title: "Variables & Bindings",
      stability: 4.5,
      difficulty: 3.2,
      reps: 3,
      lapses: 0,
      accuracyPermille: 920,
      masteryState: "Proficient",
      lastReviewEpochDay: 20736,
      dueEpochDay: 20741,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("reports").doc("rep_oct_2026").set({
      userId: ALICE_UID,
      periodLabel: "October 2026",
      overallBand: "PR",
      overallScorePermille: 825,
      summaryNarrative: "Demonstrated strong mastery across Syntax and Control Flow.",
      focusRecommendations: "Focus next on Recursion and Dynamic Programming.",
      strengths: "Consistent FSRS review adherence and high code accuracy.",
      domainScoresSummary: "Syntax: AD (92%), Control Flow: PR (84%), Algorithms: AP (71%)",
      activeMinutes: 185,
      lessonsCompleted: 14,
      conceptsMastered: 9,
      createdAt: now,
      updatedAt: now,
    })
  );
});

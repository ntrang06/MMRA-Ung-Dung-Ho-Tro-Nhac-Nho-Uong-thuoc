
const fs = require("node:fs");
const path = require("node:path");

const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");

const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
} = require("firebase/firestore");

const PROJECT_ID = "demo-mmra-rules";

const RULES_PATH = path.resolve(
  __dirname,
  "../firestore.rules"
);

let testEnv;
let passed = 0;
let failed = 0;

// --------------------------------------------------
// Helpers
// --------------------------------------------------

async function seedData(status = null) {
  await testEnv.clearFirestore();

  await testEnv.withSecurityRulesDisabled(
    async (context) => {
      const db = context.firestore();

      await setDoc(
        doc(db, "users", "user-001"),
        {
          displayName: "Test User",
        }
      );

      if (status !== null) {
        await setDoc(
          doc(
            db,
            "accountDeletionRequests",
            "user-001"
          ),
          {
            uid: "user-001",
            status,
          }
        );
      }
    }
  );
}

async function runTest(name, callback) {
  try {
    await callback();

    passed++;
    console.log(`PASS - ${name}`);
  } catch (error) {
    failed++;

    console.error(`FAIL - ${name}`);
    console.error(error.message);
  }
}

function userDatabase(uid = "user-001") {
  return testEnv
    .authenticatedContext(uid)
    .firestore();
}

// --------------------------------------------------
// Main
// --------------------------------------------------

async function main() {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: "127.0.0.1",
      port: 8080,
      rules: fs.readFileSync(
        RULES_PATH,
        "utf8"
      ),
    },
  });

  try {
    // 1. Chua co yeu cau xoa
    await runTest(
      "User can update profile without deletion request",
      async () => {
        await seedData();

        const db = userDatabase();

        await assertSucceeds(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Updated User",
            }
          )
        );
      }
    );

    // 2. Pending
    await runTest(
      "User can update profile when pending",
      async () => {
        await seedData("pending");

        const db = userDatabase();

        await assertSucceeds(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Pending User",
            }
          )
        );
      }
    );

    // 3. Confirmed
    await runTest(
      "User can update profile when confirmed",
      async () => {
        await seedData("confirmed");

        const db = userDatabase();

        await assertSucceeds(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Confirmed User",
            }
          )
        );
      }
    );

    // 4. Processing
    await runTest(
      "User cannot update profile when processing",
      async () => {
        await seedData("processing");

        const db = userDatabase();

        await assertFails(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Should Be Blocked",
            }
          )
        );
      }
    );

    // 5. Completed
    await runTest(
      "User cannot update profile when completed",
      async () => {
        await seedData("completed");

        const db = userDatabase();

        await assertFails(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Should Be Blocked",
            }
          )
        );
      }
    );

    // 6. FCM
    await runTest(
      "FCM registration blocked during deletion",
      async () => {
        await seedData("processing");

        const db = userDatabase();

        await assertFails(
          setDoc(
            doc(
              db,
              "users",
              "user-001",
              "devices",
              "device-test"
            ),
            {
              fcmToken: "fake-token",
            }
          )
        );
      }
    );

    // 7. Cross-user access
    await runTest(
      "Another user cannot update profile",
      async () => {
        await seedData();

        const db = userDatabase("other-user");

        await assertFails(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Unauthorized",
            }
          )
        );
      }
    );

    // 8. Medicine direct write
    await runTest(
      "Android cannot directly create medicine",
      async () => {
        await seedData();

        const db = userDatabase();

        await assertFails(
          setDoc(
            doc(
              db,
              "users",
              "user-001",
              "medicines",
              "medicine-test"
            ),
            {
              name: "Test Medicine",
            }
          )
        );
      }
    );

    // 9. Profile read during deletion
    await runTest(
      "Owner can read profile during deletion",
      async () => {
        await seedData("processing");

        const db = userDatabase();

        await assertSucceeds(
          getDoc(
            doc(db, "users", "user-001")
          )
        );
      }
    );

    // 10. Anonymous user
    await runTest(
      "Anonymous user cannot update profile",
      async () => {
        await seedData();

        const db = testEnv
          .unauthenticatedContext()
          .firestore();

        await assertFails(
          updateDoc(
            doc(db, "users", "user-001"),
            {
              displayName: "Anonymous",
            }
          )
        );
      }
    );
  } finally {
    await testEnv.cleanup();
  }

  console.log("\n-----------------------------");
  console.log(`PASSED: ${passed}`);
  console.log(`FAILED: ${failed}`);
  console.log("-----------------------------");

  if (failed > 0) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error("TEST SETUP FAILED:");
  console.error(error);
  process.exitCode = 1;
});

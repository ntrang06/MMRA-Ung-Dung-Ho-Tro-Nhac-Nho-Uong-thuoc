
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


// ============================================================
// 1. HELPERS
// ============================================================

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
    console.error(error);
  }
}


function userDatabase(uid = "user-001") {
  return testEnv
    .authenticatedContext(uid)
    .firestore();
}


// ============================================================
// 2. MAIN
// ============================================================

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

    // TEST 1: Chua co yeu cau xoa
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


    // TEST 2: Pending van duoc ghi
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


    // TEST 3: Confirmed bi chan
    await runTest(
      "User cannot update profile when confirmed",
      async () => {
        await seedData("confirmed");

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


    // TEST 4: Processing bi chan
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


    // TEST 5: Failed bi chan
    await runTest(
      "User cannot update profile when deletion failed",
      async () => {
        await seedData("failed");

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


    // TEST 6: Completed bi chan
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


    // TEST 7: FCM khong duoc dang ky khi xoa
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


    // TEST 8: Khong duoc ghi ho so nguoi khac
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


    // TEST 9: Android khong duoc ghi thuoc truc tiep
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
              medicineName: "Test Medicine",
            }
          )
        );
      }
    );


    // TEST 10: Chu tai khoan duoc doc khi dang xoa
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


    // TEST 11: Anonymous khong duoc ghi
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


    // TEST 12: Trang thai xoa khong hop le
    await runTest(
      "Invalid deletion status blocks profile writes",
      async () => {
        await seedData("unknown");

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


    // TEST 13: UID trong yeu cau xoa khong khop
    await runTest(
      "Wrong deletion request UID blocks writes",
      async () => {
        await seedData("pending");

        await testEnv.withSecurityRulesDisabled(
          async (context) => {
            const adminDb = context.firestore();

            await updateDoc(
              doc(
                adminDb,
                "accountDeletionRequests",
                "user-001"
              ),
              {
                uid: "other-user",
              }
            );
          }
        );

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


    // TEST 14: Thieu status trong yeu cau xoa
    await runTest(
      "Missing deletion status blocks writes",
      async () => {
        await seedData();

        await testEnv.withSecurityRulesDisabled(
          async (context) => {
            const adminDb = context.firestore();

            await setDoc(
              doc(
                adminDb,
                "accountDeletionRequests",
                "user-001"
              ),
              {
                uid: "user-001",
              }
            );
          }
        );

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

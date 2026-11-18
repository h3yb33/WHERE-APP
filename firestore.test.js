const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

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

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("active_rides").where("broadcasting", "==", true).where("status", "in", ["AVAILABLE", "FULL"]).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Anon",
      preferredMode: "COMMUTER",
      homeCity: "Lagos",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("UserProfile: owner can create and update preferredMode; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      preferredMode: "COMMUTER",
      homeCity: "Lagos",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredMode: "DRIVER",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      preferredMode: "DRIVER",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt cannot change
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("SavedRoutes: owner CRUD succeeds without @DocumentId id field; cross-user and extra keys fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_routes").doc("route_1").set({
      userId: ALICE_UID,
      originName: "Yaba Bus Stop",
      destinationName: "Ikeja Underbridge",
      distanceKm: 11.4,
      estimatedMinutes: 35,
      fareSummary: "NGN 400 - NGN 600",
      trekRecommended: false,
      stepsSummary: "Walk 200m -> Board Keke -> Board BRT",
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_routes").get()
  );

  // Reject ghost `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_routes").doc("route_2").set({
      id: "route_2",
      userId: ALICE_UID,
      originName: "Obalende",
      destinationName: "CMS",
      distanceKm: 1.1,
      estimatedMinutes: 14,
      fareSummary: "Free Trek",
      trekRecommended: true,
      stepsSummary: "Walk 1.1 km via Marina",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user list & delete rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saved_routes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("saved_routes").doc("route_1").delete());
});

test("ActiveRides: driver broadcasts & increments capacity; commuters run filtered query; unfiltered fails", async () => {
  const driverDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const commuterDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    driverDb.collection("active_rides").doc(ALICE_UID).set({
      driverId: ALICE_UID,
      driverName: "Tunde",
      vehicleType: "KEKE",
      corridorLabel: "Yaba -> Ikeja",
      originName: "Yaba Terminal",
      destinationName: "Ikeja Underbridge",
      fareEstimateNaira: 350,
      currentPassengers: 2,
      maxCapacity: 4,
      status: "AVAILABLE",
      broadcasting: true,
      latitude: 6.5095,
      longitude: 3.3711,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Driver updates seat count to 4/4 FULL
  await assertSucceeds(
    driverDb.collection("active_rides").doc(ALICE_UID).update({
      currentPassengers: 4,
      status: "FULL",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Exceeding maxCapacity fails
  await assertFails(
    driverDb.collection("active_rides").doc(ALICE_UID).update({
      currentPassengers: 5,
      status: "FULL",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Commuter cannot hijack another driver's active ride
  await assertFails(
    commuterDb.collection("active_rides").doc(ALICE_UID).update({
      currentPassengers: 1,
      status: "AVAILABLE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Commuter filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    commuterDb
      .collection("active_rides")
      .where("broadcasting", "==", true)
      .where("status", "in", ["AVAILABLE", "FULL"])
      .get()
  );
  await assertFails(commuterDb.collection("active_rides").get());
});

test("RideRequests: commuter requests seat on AVAILABLE ride; driver accepts; terminal state locked", async () => {
  const driverDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const commuterDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    driverDb.collection("active_rides").doc(ALICE_UID).set({
      driverId: ALICE_UID,
      driverName: "Tunde",
      vehicleType: "SHUTTLE",
      corridorLabel: "Lekki Phase 1 -> CMS",
      originName: "Admiralty Way",
      destinationName: "CMS Terminal",
      fareEstimateNaira: 500,
      currentPassengers: 2,
      maxCapacity: 4,
      status: "AVAILABLE",
      broadcasting: true,
      latitude: 6.4474,
      longitude: 3.4723,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    commuterDb.collection("ride_requests").doc("req_1").set({
      commuterId: BOB_UID,
      commuterName: "Chidi",
      driverId: ALICE_UID,
      driverName: "Tunde",
      corridorLabel: "Lekki Phase 1 -> CMS",
      pickupNote: "Waiting at Admiralty Gate",
      fareEstimateNaira: 500,
      status: "PENDING",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Scoped queries succeed; unfiltered fails
  await assertSucceeds(commuterDb.collection("ride_requests").where("commuterId", "==", BOB_UID).get());
  await assertSucceeds(driverDb.collection("ride_requests").where("driverId", "==", ALICE_UID).get());
  await assertFails(commuterDb.collection("ride_requests").get());

  // Driver accepts request
  await assertSucceeds(
    driverDb.collection("ride_requests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state locking: cannot update once ACCEPTED
  await assertFails(
    driverDb.collection("ride_requests").doc("req_1").update({
      status: "DECLINED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

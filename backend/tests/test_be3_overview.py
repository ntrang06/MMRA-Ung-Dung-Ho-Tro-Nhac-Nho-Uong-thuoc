from datetime import date, datetime, timedelta, timezone
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import Mock, patch

from fastapi import FastAPI
from fastapi.testclient import TestClient
from google.api_core.exceptions import ServiceUnavailable

from app.api.deps import CurrentUser, get_current_user
from app.api.v1.endpoints import home, medicines
from app.repositories.medication_repository import MedicationRepository
from app.repositories import medication_repository as repository


DAY = date(2026, 11, 14)


def dose(number, status="PENDING", minute=480, **changes):
    at = datetime(
        2026, 11, 14, tzinfo=timezone.utc
    ) + timedelta(minutes=minute - 420)

    return dict(
        doseId=f"{number:064x}",
        scheduleId="s",
        scheduleVersion=1,
        medicineId="m",
        localDate=DAY.isoformat(),
        minuteOfDay=minute,
        timeZone="Asia/Ho_Chi_Minh",
        scheduledAt=at,
        doseQuantity="1",
        doseUnit="TABLET",
        mealInstruction="",
        medicineSnapshot=dict(
            medicineName="Thuốc",
            strength="500",
            strengthUnit="mg",
            dosageForm="Viên nén",
        ),
        status=status,
        actionAt=None,
        nextReminderAt=None,
        createdAt=at,
        updatedAt=at,
    ) | changes


def snapshot(name, data, exists=True):
    return SimpleNamespace(
        id=name,
        reference=name,
        exists=exists,
        to_dict=lambda: data,
    )


class HomeTests(TestCase):
    def setUp(self):
        self.app = FastAPI()
        self.app.include_router(home.router, prefix="/api/v1")
        self.app.dependency_overrides[get_current_user] = (
            lambda: CurrentUser("A")
        )
        self.client = TestClient(self.app)

    def test_totals_exclude_cancelled_and_keep_it_for_alarm_sync(self):
        rows = [
            dose(1, "TAKEN"),
            dose(2, "SKIPPED"),
            dose(3, "MISSED"),
            dose(4),
            dose(5, "CANCELLED"),
        ]

        with patch.object(
            home.DoseService,
            "get_doses_for_date",
            return_value=rows,
        ) as read:
            response = self.client.get(
                "/api/v1/home?date=2026-11-14&uid=B"
            )

        self.assertEqual(response.status_code, 200)
        read.assert_called_once_with(CurrentUser("A"), DAY)

        body = response.json()

        self.assertEqual(
            [
                body[key]
                for key in [
                    "totalDoses",
                    "takenDoses",
                    "skippedDoses",
                    "missedDoses",
                    "pendingDoses",
                    "progressPercent",
                ]
            ],
            [4, 1, 1, 1, 1, 25],
        )

        self.assertEqual(len(body["doses"]), 5)
        self.assertEqual(
            body["nextDose"]["doseId"],
            f"{4:064x}",
        )
        self.assertEqual(
            response.headers["cache-control"],
            "no-store",
        )

    def test_empty_and_finalized_day_has_no_next_dose(self):
        for rows in [
            [],
            [dose(1, "TAKEN")],
            [dose(2, "CANCELLED")],
        ]:
            result = home.build_home_summary(
                rows,
                DAY,
                datetime.now(timezone.utc),
            )

            self.assertIsNone(result.nextDose)
            self.assertEqual(result.pendingDoses, 0)

    def test_snooze_order_and_original_time_are_preserved(self):
        first = dose(
            1,
            minute=480,
            nextReminderAt=datetime(
                2026, 11, 14, 3, tzinfo=timezone.utc
            ),
        )
        second = dose(2, minute=540)

        result = home.build_home_summary(
            [first, second],
            DAY,
            datetime.now(timezone.utc),
        )

        self.assertEqual(
            result.nextDose.doseId,
            second["doseId"],
        )
        self.assertEqual(
            result.doses[0].scheduledAt,
            first["scheduledAt"],
        )

    def test_auth_validation_failure_and_default_vietnam_date(self):
        with patch.object(
            home.DoseService,
            "get_doses_for_date",
            return_value=[],
        ) as read:
            self.assertEqual(
                self.client.get(
                    "/api/v1/home?date=bad"
                ).status_code,
                422,
            )
            read.assert_not_called()

            self.assertEqual(
                self.client.get(
                    "/api/v1/home"
                ).status_code,
                200,
            )

            self.assertEqual(
                read.call_args.args[1],
                datetime.now(
                    home.ZoneInfo(home.HOME_TIME_ZONE)
                ).date(),
            )

            self.app.dependency_overrides.clear()

            self.assertEqual(
                self.client.get(
                    "/api/v1/home"
                ).status_code,
                401,
            )

        self.app.dependency_overrides[get_current_user] = (
            lambda: CurrentUser("A")
        )

        with patch.object(
            home.DoseService,
            "get_doses_for_date",
            side_effect=ServiceUnavailable("offline"),
        ):
            self.assertEqual(
                self.client.get(
                    "/api/v1/home"
                ).status_code,
                503,
            )


class DeletionTests(TestCase):
    def setUp(self):
        self.app = FastAPI()
        self.app.include_router(
            medicines.router,
            prefix="/api/v1",
        )
        self.app.dependency_overrides[get_current_user] = (
            lambda: CurrentUser("A")
        )

        self.client = TestClient(self.app)
        self.transaction = Mock()
        self.events = []

        self.transaction.update.side_effect = (
            lambda *args: self.events.append("write")
        )
        self.transaction.delete.side_effect = (
            lambda *args: self.events.append("write")
        )

        self.medicine = snapshot("m", {})
        self.schedule = {
            "medicineId": "m",
            "version": 2,
            "enabled": True,
        }
        self.rows = [snapshot("s", self.schedule)]

        now = datetime.now(timezone.utc)

        self.doses = [
            snapshot(
                "future",
                {
                    "status": "PENDING",
                    "scheduledAt": now + timedelta(days=1),
                },
            ),
            snapshot(
                "past",
                {
                    "status": "PENDING",
                    "scheduledAt": now - timedelta(days=1),
                    "nextReminderAt": (
                        now + timedelta(minutes=10)
                    ),
                },
            ),
            snapshot(
                "taken",
                {
                    "status": "TAKEN",
                    "scheduledAt": now - timedelta(days=1),
                },
            ),
        ]
        self.scopes = []

    def document(self, user, name, document_id):
        self.scopes.append(
            (user.uid, name, document_id)
        )

        def get(**kwargs):
            self.events.append("read")
            return self.medicine

        return SimpleNamespace(get=get)

    def collection(self, user, name):
        self.scopes.append((user.uid, name))

        def stream(**kwargs):
            self.events.append("read")
            return iter(
                self.rows
                if name == "schedules"
                else self.doses
            )

        query = Mock()
        query.where.return_value = query
        query.stream.side_effect = stream

        return query

    def remove(self):
        with (
            patch.object(
                medicines,
                "user_document",
                self.document,
            ),
            patch.object(
                medicines,
                "user_collection",
                self.collection,
            ),
            patch.object(
                medicines,
                "get_firebase_app",
                return_value=None,
            ),
            patch.object(
                medicines.firestore,
                "transactional",
                lambda fn: fn,
            ),
            patch.object(
                medicines.firestore,
                "client",
                return_value=SimpleNamespace(
                    transaction=lambda: self.transaction
                ),
            ),
        ):
            return self.client.delete(
                "/api/v1/medicines/m?uid=B"
            )

    def test_deletion_keeps_history_and_cancels_reminders(self):
        response = self.remove()

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            self.events,
            [
                "read", "read", "read",
                "write", "write", "write", "write",
            ],
        )
        self.assertTrue(
            all(scope[0] == "A" for scope in self.scopes)
        )

        writes = {
            call.args[0]: call.args[1]
            for call in self.transaction.update.call_args_list
        }

        self.assertEqual(writes["s"]["version"], 3)
        self.assertEqual(
            writes["future"]["status"],
            "CANCELLED",
        )
        self.assertIsNone(
            writes["past"]["nextReminderAt"]
        )
        self.assertNotIn("status", writes["past"])
        self.assertNotIn("taken", writes)

        self.transaction.delete.assert_called_once()

        self.assertEqual(
            response.json()["doseIdsToCancel"],
            ["future", "past", "taken"],
        )

    def test_missing_medicine_and_invalid_data_do_not_write(self):
        self.medicine.exists = False

        self.assertEqual(
            self.remove().status_code,
            404,
        )

        self.transaction.update.assert_not_called()
        self.transaction.delete.assert_not_called()

        self.medicine.exists = True
        self.doses[0].to_dict = lambda: {
            "status": "PENDING",
            "scheduledAt": "bad",
        }

        self.assertEqual(
            self.remove().status_code,
            503,
        )

        self.transaction.delete.assert_not_called()

    def test_generation_checks_medicine_and_current_schedule_in_transaction(self):
        stored_medicine = {}
        stored_schedule = {
            "medicineId": "m",
            "version": 1,
            "enabled": True,
            "createdAt": datetime(
                2026, 11, 14, tzinfo=timezone.utc
            ),
        }

        med = snapshot("m", stored_medicine)
        sched = snapshot("s", stored_schedule)

        def document(user, name, key):
            target = med if name == "medicines" else sched

            return SimpleNamespace(
                get=lambda **kwargs: target
            )

        with (
            patch.object(
                repository,
                "user_document",
                document,
            ),
            patch.object(
                repository,
                "get_firebase_app",
                return_value=None,
            ),
            patch.object(
                repository.google_firestore,
                "transactional",
                lambda fn: fn,
            ),
            patch.object(
                repository.firestore,
                "client",
                return_value=SimpleNamespace(
                    transaction=lambda: self.transaction
                ),
            ),
        ):
            self.assertTrue(
                MedicationRepository.create_dose_if_absent(
                    CurrentUser("A"),
                    "d",
                    dose(1),
                )
            )

            self.transaction.reset_mock()

            stored_schedule["updatedAt"] = (
                dose(1)["scheduledAt"]
                + timedelta(seconds=1)
            )

            self.assertFalse(
                MedicationRepository.create_dose_if_absent(
                    CurrentUser("A"),
                    "d",
                    dose(1),
                )
            )

            stored_schedule.pop("updatedAt")
            stored_schedule["version"] = 2

            self.assertFalse(
                MedicationRepository.create_dose_if_absent(
                    CurrentUser("A"),
                    "d",
                    dose(1),
                )
            )

            stored_schedule["version"] = 1
            med.exists = False

            self.assertFalse(
                MedicationRepository.create_dose_if_absent(
                    CurrentUser("A"),
                    "d",
                    dose(1),
                )
            )

            self.transaction.create.assert_not_called()
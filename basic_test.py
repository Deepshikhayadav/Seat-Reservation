import requests
import sys
import uuid

BASE_URL = "http://localhost:8082"


def check(condition, message):
    if not condition:
        print(f"❌ FAIL: {message}")
        sys.exit(1)

    print(f"✅ PASS: {message}")


def main():
    print("========================================")
    print("STEP 4 - BASIC RESERVATION API TESTS")
    print("========================================")

    # ---------------------------------------------------------
    # 1. Create a show
    # ---------------------------------------------------------
    show_name = f"test-show-{uuid.uuid4()}"

    response = requests.post(
        f"{BASE_URL}/shows",
        json={
            "name": show_name,
            "seats": ["A1", "A2", "A3", "A4", "A5"],
            "price_paise": 25000
        }
    )

    print("Create show:", response.status_code, response.text)

    check(
        response.status_code in (200, 201),
        "show can be created"
    )

    show = response.json()
    show_id = show["id"]

    print(f"Show ID: {show_id}")

    # ---------------------------------------------------------
    # 2. Reserve A1
    # ---------------------------------------------------------
    idempotency_key = f"basic-{uuid.uuid4()}"

    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": idempotency_key
        },
        json={
            "seats": ["A1"]
        }
    )

    print("First reservation:", response.status_code, response.text)

    check(
        response.status_code == 201,
        "first reservation returns 201"
    )

    reservation = response.json()

    # Current entity response may use these names.
    reservation_id = reservation.get("id") or reservation.get("reservation_id")

    check(
        reservation_id is not None,
        "reservation ID is returned"
    )

    # ---------------------------------------------------------
    # 3. Retry SAME request with SAME idempotency key
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": idempotency_key
        },
        json={
            "seats": ["A1"]
        }
    )

    print("Idempotent retry:", response.status_code, response.text)

    check(
        response.status_code == 201,
        "same idempotency key returns original reservation"
    )

    retry_reservation = response.json()

    retry_id = (
        retry_reservation.get("id")
        or retry_reservation.get("reservation_id")
    )

    check(
        retry_id == reservation_id,
        "idempotent retry returns same reservation"
    )

    # ---------------------------------------------------------
    # 4. Same idempotency key + DIFFERENT body
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": idempotency_key
        },
        json={
            "seats": ["A2"]
        }
    )

    print("Different body with same key:", response.status_code, response.text)

    check(
        response.status_code == 409,
        "same idempotency key with different body returns 409"
    )

    # ---------------------------------------------------------
    # 5. New key attempting already reserved A1
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": f"taken-{uuid.uuid4()}"
        },
        json={
            "seats": ["A1"]
        }
    )

    print("Already reserved seat:", response.status_code, response.text)

    check(
        response.status_code == 409,
        "already reserved seat returns 409"
    )

    # ---------------------------------------------------------
    # 6. Reserve multiple seats
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": f"multi-{uuid.uuid4()}"
        },
        json={
            "seats": ["A2", "A3"]
        }
    )

    print("Multi-seat reservation:", response.status_code, response.text)

    check(
        response.status_code == 201,
        "multi-seat reservation succeeds"
    )

    # ---------------------------------------------------------
    # 7. Try reserving one available seat
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": f"single-{uuid.uuid4()}"
        },
        json={
            "seats": ["A4"]
        }
    )

    print("A4 reservation:", response.status_code, response.text)

    check(
        response.status_code == 201,
        "another available seat can be reserved"
    )

    # ---------------------------------------------------------
    # 8. Per-user limit
    #
    # Controller currently uses:
    #     userId = "user-1"
    #
    # Default limit should be 4.
    #
    # We already reserved:
    #     A1 = 1
    #     A2,A3 = 2
    #     A4 = 1
    #
    # Total = 4
    #
    # A5 should therefore fail.
    # ---------------------------------------------------------
    response = requests.post(
        f"{BASE_URL}/shows/{show_id}/reserve",
        headers={
            "Idempotency-Key": f"limit-{uuid.uuid4()}"
        },
        json={
            "seats": ["A5"]
        }
    )

    print("Per-user limit:", response.status_code, response.text)

    check(
        response.status_code == 409,
        "per-user limit returns 409"
    )

    # ---------------------------------------------------------
    # 9. GET show
    # ---------------------------------------------------------
    response = requests.get(
        f"{BASE_URL}/shows/{show_id}"
    )

    print("Show state:", response.status_code, response.text)

    check(
        response.status_code == 200,
        "show can be retrieved"
    )

    show_state = response.json()

    print("\nFinal show state:")
    print(show_state)

    print("\n========================================")
    print("ALL STEP 4 TESTS PASSED")
    print("========================================")


if __name__ == "__main__":
    main()
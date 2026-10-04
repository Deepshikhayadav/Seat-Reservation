import requests
import concurrent.futures
import uuid
import sys

BASE_URL = "http://localhost:8080"


def reserve(show_id, index):
    try:
        response = requests.post(
            f"{BASE_URL}/shows/{show_id}/reserve",
            headers={
                "Idempotency-Key": f"concurrent-{index}-{uuid.uuid4()}"
            },
            json={
                "seats": ["A1"]
            },
            timeout=10
        )

        return response.status_code, response.text

    except Exception as e:
        return "ERROR", str(e)


def main():
    if len(sys.argv) != 2:
        print("Usage:")
        print("python concurrency_test.py SHOW_ID")
        sys.exit(1)

    show_id = sys.argv[1]

    number_of_requests = 50

    print("========================================")
    print("STEP 5 - CONCURRENCY TEST")
    print("========================================")

    print(f"Show ID: {show_id}")
    print(f"Requests: {number_of_requests}")
    print("Seat: A1")
    print()

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=number_of_requests
    ) as executor:

        futures = [
            executor.submit(
                reserve,
                show_id,
                i
            )
            for i in range(number_of_requests)
        ]

        results = [
            future.result()
            for future in futures
        ]

    status_counts = {}

    for status, body in results:
        status_counts[status] = status_counts.get(status, 0) + 1

    print("========================================")
    print("RESULTS")
    print("========================================")

    for status, count in sorted(status_counts.items()):
        print(f"{status}: {count}")

    print()

    success_count = status_counts.get(201, 0)
    conflict_count = status_counts.get(409, 0)
    error_count = status_counts.get(500, 0)

    # ---------------------------------------------------------
    # The important requirement:
    #
    # Exactly ONE request should successfully reserve A1.
    # The rest should get 409.
    #
    # No request should get 500.
    # ---------------------------------------------------------

    if success_count != 1:
        print(
            f"❌ FAIL: expected exactly 1 successful reservation, "
            f"got {success_count}"
        )
        sys.exit(1)

    if error_count != 0:
        print(
            f"❌ FAIL: expected 0 HTTP 500 responses, "
            f"got {error_count}"
        )
        sys.exit(1)

    if conflict_count != number_of_requests - 1:
        print(
            f"❌ FAIL: expected {number_of_requests - 1} conflicts, "
            f"got {conflict_count}"
        )
        sys.exit(1)

    print("✅ Exactly one request succeeded.")
    print("✅ All other requests received 409.")
    print("✅ No 500 responses.")
    print()
    print("========================================")
    print("STEP 5 PASSED")
    print("========================================")


if __name__ == "__main__":
    main()
import concurrent.futures
import sys
import time
import uuid

import requests

from jwt_token import create_token


BASE_URL = "http://localhost:8080"
NUMBER_OF_REQUESTS = 50
MAX_WORKERS = 20


def reserve(show_id, index, jwt_token):
    try:
        response = requests.post(
            f"{BASE_URL}/shows/{show_id}/reserve",
            headers={
                "Authorization": f"Bearer {jwt_token}",
                "Idempotency-Key": f"burst-{index}-{uuid.uuid4()}",
                "Content-Type": "application/json",
            },
            json={
                "seats": [f"A{index + 1}"]
            },
            timeout=10,
        )

        return response.status_code, response.text

    except Exception as exc:
        return "ERROR", str(exc)


def main():
    if len(sys.argv) != 2:
        print("Usage:")
        print("python burst_test.py SHOW_ID")
        sys.exit(1)

    show_id = sys.argv[1]

    jwt_token = create_token("burst-test-user")

    print("========================================")
    print("BURST TEST")
    print("========================================")
    print(f"URL: {BASE_URL}")
    print(f"Show ID: {show_id}")
    print(f"Requests: {NUMBER_OF_REQUESTS}")
    print(f"Workers: {MAX_WORKERS}")
    print()

    start = time.time()

    with concurrent.futures.ThreadPoolExecutor(
        max_workers=MAX_WORKERS
    ) as executor:

        futures = [
            executor.submit(
                reserve,
                show_id,
                index,
                jwt_token,
            )
            for index in range(NUMBER_OF_REQUESTS)
        ]

        results = [
            future.result()
            for future in futures
        ]

    duration = time.time() - start

    status_counts = {}

    for status, body in results:
        status_counts[status] = status_counts.get(status, 0) + 1

    print("========================================")
    print("RESULTS")
    print("========================================")

    for status, count in sorted(status_counts.items(), key=lambda item: str(item[0])):
        print(f"{status}: {count}")

    print()
    print(f"Total time: {duration:.2f} seconds")

    successful = status_counts.get(201, 0)
    conflicts = status_counts.get(409, 0)
    server_errors = status_counts.get(500, 0)
    errors = status_counts.get("ERROR", 0)

    print()
    print("========================================")
    print("SUMMARY")
    print("========================================")
    print(f"201 Created: {successful}")
    print(f"409 Conflict: {conflicts}")
    print(f"500 Errors: {server_errors}")
    print(f"Client Errors: {errors}")

    if server_errors > 0 or errors > 0:
        print()
        print("❌ Burst test found errors.")
        sys.exit(1)

    print()
    print("✅ Burst test completed without HTTP 500 errors.")


if __name__ == "__main__":
    main()
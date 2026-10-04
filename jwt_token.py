import jwt
import sys
from datetime import datetime, timedelta, timezone

# Must match the JWT_SECRET used by your Spring Boot app.
SECRET = "change-this-local-secret-to-at-least-32-characters-long"


def create_token(user_id):
    now = datetime.now(timezone.utc)

    payload = {
        "sub": user_id,
        "iat": now,
        "exp": now + timedelta(hours=2)
    }

    return jwt.encode(
        payload,
        SECRET,
        algorithm="HS256"
    )


def main():
    if len(sys.argv) != 2:
        print("Usage:")
        print("python jwt_token.py user-1")
        sys.exit(1)

    user_id = sys.argv[1]

    token = create_token(user_id)

    print()
    print("USER:", user_id)
    print()
    print("JWT:")
    print(token)
    print()


if __name__ == "__main__":
    main()
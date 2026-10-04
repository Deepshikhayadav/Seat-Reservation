import jwt
import sys
from datetime import datetime, timedelta, timezone

# ---------------------------------------------------------
# Must match app.jwt.secret in application.yml
# ---------------------------------------------------------
SECRET = "change-this-local-secret-to-at-least-32-characters-long"


def main():

    if len(sys.argv) != 2:
        print("Usage:")
        print("python jwt_token.py user-1")
        sys.exit(1)

    user_id = sys.argv[1]

    now = datetime.now(timezone.utc)

    payload = {
        # IMPORTANT:
        # Spring Security uses this as authentication.getName()
        "sub": user_id,

        "iat": now,
        "exp": now + timedelta(hours=2)
    }

    token = jwt.encode(
        payload,
        SECRET,
        algorithm="HS256"
    )

    print()
    print("USER:", user_id)
    print()
    print("JWT:")
    print(token)
    print()


if __name__ == "__main__":
    main()
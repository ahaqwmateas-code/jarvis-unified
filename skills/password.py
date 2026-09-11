import secrets, string

SKILL = {
    "name": "password",
    "description": "password <length> - strong random password",
    "keywords": ["password", "passwd", "passgen"],
}


def run(args, ctx):
    n = 16
    try:
        n = int(args.strip()) if args.strip() else 16
    except Exception:
        n = 16
    n = max(8, min(64, n))
    chars = string.ascii_letters + string.digits + "!@#$%^&*"
    pwd = (
        secrets.choice(string.ascii_lowercase)
        + secrets.choice(string.ascii_uppercase)
        + secrets.choice(string.digits)
        + secrets.choice("!@#$%^&*")
        + "".join(secrets.choice(chars) for _ in range(n - 4))
    )
    return pwd

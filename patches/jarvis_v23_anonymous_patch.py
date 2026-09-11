import re, os

CORE = os.path.expanduser("~/.jarvis/core.py")
src = open(CORE).read()

NEW_FN = '''

def ask_anonymous(prompt):
    """Free anonymous cloud fallback (Pollinations /openai, gpt-oss-20b) - no key needed."""
    try:
        import requests
        ctx = "\\n".join(f"{r}: {t}" for r, t in HISTORY)
        ctx = ctx[-3000:]
        full = JARVIS_SYS + ("\\n\\nRecent conversation:\\n" + ctx if ctx else "") + "\\n\\nUser: " + prompt
        r = requests.post("https://text.pollinations.ai/openai",
                          json={"model": "openai", "messages": [
                              {"role": "system", "content": JARVIS_SYS},
                              {"role": "user", "content": full}]},
                          timeout=90)
        if r.status_code == 200:
            c = (r.json().get("choices") or [{}])
            if c and c[0].get("message", {}).get("content"):
                return c[0]["message"]["content"].strip()
    except Exception:
        pass
    return None
'''

if "def ask_anonymous(" not in src:
    anchor = "\ndef ask_llm("
    if anchor in src:
        src = src.replace(anchor, NEW_FN + anchor, 1)
        print("core.py: ask_anonymous added (Pollinations /openai fallback)")
    else:
        print("WARN: ask_llm anchor not found")
else:
    print("core.py: ask_anonymous already present")

old = '''    ans = ask_llm(t)
    if ans is not None:
        return ans
    return "No AI online and no skill matched. Say 'help'."'''
new = '''    ans = ask_llm(t)
    if ans is None:
        ans = ask_anonymous(t)
    if ans is not None:
        return ans
    return "No AI online and no skill matched. Say 'help'."'''
if old in src:
    src = src.replace(old, new, 1)
    print("core.py: route() now falls back to the anonymous brain")
elif "ask_anonymous(t)" not in src:
    print("WARN: route fallback anchor not found (core may differ)")

open(CORE, "w").write(src)
print("v23 done")

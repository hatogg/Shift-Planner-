"""Помилки збірки Gradle/Kotlin — анотаціями на сторінці запуску GitHub Actions (журнал читати не доводиться)."""
import os
import re
import sys

LIMIT = 10
log = open(sys.argv[1], encoding="utf-8", errors="replace").read() if os.path.exists(sys.argv[1]) else ""
root = os.getcwd() + "/"


def esc(s, prop=False):
    s = s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
    return s.replace(":", "%3A").replace(",", "%2C") if prop else s


out = []
for m in re.finditer(r"^e: (?:file://)?(/[^:]+):(\d+):(\d+) (.+)$", log, re.M):
    path, line, col, msg = m.groups()
    out.append(f"::error file={esc(path.replace(root, ''), True)},line={line},col={col}::{esc(msg)}")
for m in re.finditer(r"^(?:ERROR|AAPT: error|error): (.+)$", log, re.M):
    out.append(f"::error title=build::{esc(m.group(1)[:600])}")
w = re.search(r"\* What went wrong:\n(.+?)(?:\n\* Try:|\Z)", log, re.S)
if w:
    out.append(f"::error title=What went wrong::{esc(w.group(1).strip()[-1500:])}")
if not out:
    out.append(f"::error title=build failed::{esc(log[-1500:])}")
for line in out[:LIMIT]:
    print(line)
if len(out) > LIMIT:
    print(f"::error title=and {len(out) - LIMIT} more::see build.log")

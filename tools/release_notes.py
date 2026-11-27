"""Validate the current changelog and extract the matching release notes."""
from datetime import date
from pathlib import Path
import argparse
import re

ROOT = Path(__file__).resolve().parents[1]
CATEGORIES = {"Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"}


def release_notes() -> str:
    properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    version = re.search(r"^mod_version=(.+)$", properties, re.MULTILINE).group(1).strip()
    changelog = (ROOT / "CHANGELOG.md").read_text(encoding="utf-8")
    body = re.split(r"^\[[^\n]+\]: ", changelog, maxsplit=1, flags=re.MULTILINE)[0]
    sections = re.split(r"^## ", body, flags=re.MULTILINE)[1:]
    if not sections or sections[0].splitlines()[0] != "[Unreleased]":
        raise ValueError("CHANGELOG.md must start with an [Unreleased] section")
    seen = set()
    selected = None
    previous_date = date.max
    for section in sections[1:]:
        heading, _, notes = section.strip().partition("\n")
        release = re.fullmatch(r"\[([^\]]+)\] - (\d{4}-\d{2}-\d{2})", heading)
        if release is None:
            raise ValueError(f"Expected a dated release heading, found {heading!r}")
        number, day = release.groups()
        release_date = date.fromisoformat(day)
        if number in seen or release_date > previous_date:
            raise ValueError(f"Duplicate or out-of-order changelog release: {number}")
        seen.add(number)
        previous_date = release_date
        categories = re.split(r"^### ", notes.strip(), flags=re.MULTILINE)
        if categories[0] or len(categories) == 1:
            raise ValueError(f"Release {number} needs categorized notes")
        used = set()
        for category in categories[1:]:
            label, _, entries = category.strip().partition("\n")
            if label not in CATEGORIES or label in used or not re.search(r"^- \S", entries, re.MULTILINE):
                raise ValueError(f"Invalid or empty category {label!r} in release {number}")
            used.add(label)
        if number == version:
            selected = f"## {number} - {day}\n\n{notes.strip()}\n"
    if selected is None:
        raise ValueError(f"CHANGELOG.md is missing a dated [{version}] release section")
    return selected


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Write the validated release notes to this file")
    args = parser.parse_args()
    try:
        notes = release_notes()
    except ValueError as error:
        parser.exit(1, f"{error}\n")
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(notes, encoding="utf-8", newline="\n")
    else:
        print(f"Validated changelog for {notes.splitlines()[0][3:]}")

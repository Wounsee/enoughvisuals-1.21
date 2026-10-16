#!/usr/bin/env python3
"""Generate the five Inter 18pt MTSDF atlases used by EnoughVisuals."""

from __future__ import annotations

import os
import argparse
import json
import re
import subprocess
from pathlib import Path

from fontTools.ttLib import TTFont


WEIGHTS = {
    "regular": "Inter_18pt-Regular.ttf",
    "medium": "Inter_18pt-Medium.ttf",
    "semibold": "Inter_18pt-SemiBold.ttf",
    "bold": "Inter_18pt-Bold.ttf",
    "extrabold": "Inter_18pt-ExtraBold.ttf",
}

# Predictable input, console, currency, navigation and formatting characters.
BASE_CHARACTERS = (
    "".join(chr(codepoint) for codepoint in range(0x20, 0x7F))
    # Latin alphabets used by on-demand machine-translated catalogs, including
    # Vietnamese precomposed tone marks. Keeping these in the atlas prevents
    # a newly generated language from degrading into fallback question marks.
    + "".join(chr(codepoint) for codepoint in range(0x00C0, 0x0180))
    + "".join(chr(codepoint) for codepoint in range(0x1EA0, 0x1EFA))
    + "".join(chr(codepoint) for codepoint in range(0x0400, 0x0530))
    + "№—–…«»„“”’•→←↑↓×÷±°₽€$₴₸₺₾₿✓✕★☆♥❤⚠"
)

STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\])*"', re.DOTALL)


def java_string_characters(source: str) -> set[str]:
    result: set[str] = set()
    for match in STRING_LITERAL.finditer(source):
        try:
            result.update(json.loads(match.group(0)))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
    return result


def collect_characters(root: Path) -> set[str]:
    characters = set(BASE_CHARACTERS)
    for path in (root / "src" / "main" / "java").rglob("*.java"):
        characters.update(java_string_characters(path.read_text(encoding="utf-8", errors="ignore")))

    lang_root = root / "src" / "main" / "resources" / "assets" / "enough" / "lang"
    for path in lang_root.glob("*.json"):
        try:
            value = json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            continue
        stack = [value]
        while stack:
            item = stack.pop()
            if isinstance(item, str):
                characters.update(item)
            elif isinstance(item, list):
                stack.extend(item)
            elif isinstance(item, dict):
                stack.extend(item.keys())
                stack.extend(item.values())

    return {
        character
        for character in characters
        if len(character) == 1 and character.isprintable() and not 0xE000 <= ord(character) <= 0xF8FF
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--inter-dir", type=Path, default=Path(os.environ.get("INTER_FONT_DIR", "inter")))
    parser.add_argument(
        "--generator",
        type=Path,
        default=Path(os.environ.get("MSDF_ATLAS_GEN", "msdf-atlas-gen")),
    )
    args = parser.parse_args()

    root = args.root.resolve()
    output = root / "src" / "main" / "resources" / "assets" / "enough" / "fonts" / "msdf"
    output.mkdir(parents=True, exist_ok=True)

    requested = collect_characters(root)
    common_cmap: set[int] | None = None
    for filename in WEIGHTS.values():
        cmap = set(TTFont(args.inter_dir / filename).getBestCmap())
        common_cmap = cmap if common_cmap is None else common_cmap & cmap
    assert common_cmap is not None

    supported = sorted(ord(character) for character in requested if ord(character) in common_cmap)
    unsupported = sorted(ord(character) for character in requested if ord(character) not in common_cmap)
    charset_path = Path(__file__).with_name("inter_charset.txt")
    charset_path.write_text(",\n".join(f"0x{codepoint:04X}" for codepoint in supported) + "\n", encoding="utf-8")
    Path(__file__).with_name("inter_unsupported.txt").write_text(
        "\n".join(f"U+{codepoint:04X} {chr(codepoint)!r}" for codepoint in unsupported) + "\n",
        encoding="utf-8",
    )

    for atlas_name, filename in WEIGHTS.items():
        command = [
            str(args.generator),
            "-font", str(args.inter_dir / filename),
            "-fontname", f"Inter 18pt {atlas_name}",
            "-charset", str(charset_path),
            "-type", "mtsdf",
            "-size", "64",
            "-pxrange", "10",
            "-square4",
            "-imageout", str(output / f"{atlas_name}.png"),
            "-json", str(output / f"{atlas_name}.json"),
        ]
        subprocess.run(command, check=True)

    print(f"Generated {len(supported)} glyphs in {len(WEIGHTS)} Inter atlases")
    if unsupported:
        print(f"Skipped {len(unsupported)} unsupported characters; see inter_unsupported.txt")


if __name__ == "__main__":
    main()

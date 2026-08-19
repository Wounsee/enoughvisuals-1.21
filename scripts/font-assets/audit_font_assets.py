#!/usr/bin/env python3
"""Fail when a generated font/icon asset and its Java usage drift apart."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
MSDF = ROOT / "src/main/resources/assets/enough/fonts/msdf"
JAVA = ROOT / "src/main/java/fun/enoughvisuals"
WEIGHTS = ("regular", "medium", "semibold", "bold", "extrabold")
LEGACY_WEIGHTS = ("LIGHT", "MARK", "EVENT", "DRAG", "POTIONS", "BINDS", "HEART", "CLIENT", "WP", "VC", "BOOK", "TIME")


def glyphs(name: str) -> set[int]:
    data = json.loads((MSDF / f"{name}.json").read_text(encoding="utf-8"))
    return {int(glyph["unicode"]) for glyph in data["glyphs"]}


def empty_glyphs(name: str) -> set[int]:
    data = json.loads((MSDF / f"{name}.json").read_text(encoding="utf-8"))
    return {
        int(glyph["unicode"])
        for glyph in data["glyphs"]
        if not glyph.get("planeBounds") or not glyph.get("atlasBounds")
    }


def fail(message: str) -> None:
    raise SystemExit(f"font asset audit failed: {message}")


def main() -> None:
    expected_text = {
        int(line.rstrip(","), 16)
        for line in (Path(__file__).with_name("inter_charset.txt")).read_text(encoding="utf-8").splitlines()
        if line.strip()
    }
    baseline = glyphs(WEIGHTS[0])
    if baseline != expected_text:
        fail("regular Inter atlas does not match the generated charset")
    for weight in WEIGHTS[1:]:
        if glyphs(weight) != baseline:
            fail(f"Inter {weight} atlas has a different glyph set")

    manifest = json.loads((Path(__file__).with_name("solar-icons.json")).read_text(encoding="utf-8"))
    expected_icons = {int(item["codepoint"], 16) for item in manifest["icons"]}
    if glyphs("solar_icons") != expected_icons:
        fail("Solar atlas does not match the pinned semantic manifest")
    empty_icons = empty_glyphs("solar_icons")
    if empty_icons:
        fail("Solar atlas contains empty glyphs: " + ", ".join(f"U+{value:04X}" for value in sorted(empty_icons)))

    constants_source = (JAVA / "utils/font/SolarIcons.java").read_text(encoding="utf-8")
    constants = {
        name: int(codepoint, 16)
        for name, codepoint in re.findall(r'String\s+(\w+)\s*=\s*"\\u([0-9A-Fa-f]{4})"', constants_source)
    }
    manifest_constants = {item["constant"]: int(item["codepoint"], 16) for item in manifest["icons"]}
    if constants != manifest_constants:
        fail("SolarIcons.java does not match solar-icons.json")

    hud_manifest = json.loads((Path(__file__).with_name("hud-icons.json")).read_text(encoding="utf-8"))
    expected_hud_icons = {int(item["codepoint"], 16) for item in hud_manifest["icons"]}
    if glyphs("hud_icons") != expected_hud_icons:
        fail("HUD atlas does not match the pinned Figma manifest")
    empty_hud_icons = empty_glyphs("hud_icons")
    if empty_hud_icons:
        fail("HUD atlas contains empty glyphs: "
             + ", ".join(f"U+{value:04X}" for value in sorted(empty_hud_icons)))

    hud_constants_source = (JAVA / "utils/font/HudIcons.java").read_text(encoding="utf-8")
    hud_constants = {
        name: int(codepoint, 16)
        for name, codepoint in re.findall(r'String\s+(\w+)\s*=\s*"\\u([0-9A-Fa-f]{4})"', hud_constants_source)
    }
    hud_manifest_constants = {
        item["constant"]: int(item["codepoint"], 16) for item in hud_manifest["icons"]
    }
    if hud_constants != hud_manifest_constants:
        fail("HudIcons.java does not match hud-icons.json")

    all_java = "\n".join(path.read_text(encoding="utf-8", errors="ignore") for path in JAVA.rglob("*.java"))
    used_constants = set(re.findall(r"SolarIcons\.(\w+)", all_java))
    missing = used_constants - constants.keys()
    if missing:
        fail("missing Solar glyph constants: " + ", ".join(sorted(missing)))
    used_hud_constants = set(re.findall(r"HudIcons\.(\w+)", all_java))
    missing_hud = used_hud_constants - hud_constants.keys()
    if missing_hud:
        fail("missing HUD glyph constants: " + ", ".join(sorted(missing_hud)))
    for legacy in LEGACY_WEIGHTS:
        if f"FontWeight.{legacy}" in all_java:
            fail(f"legacy font weight is still referenced: {legacy}")

    click_gui_uses = []
    for path in JAVA.rglob("*.java"):
        source = path.read_text(encoding="utf-8", errors="ignore")
        if "FontWeight.CLICK_GUI" in source:
            click_gui_uses.append(path.name)
    if set(click_gui_uses) - {"CustomMainMenuScreen.java", "CustomFontRenderer.java"}:
        fail("CLICK_GUI icon font leaked outside the preserved main menu")

    print(f"Font asset audit passed: {len(baseline)} Inter glyphs x 5, "
          f"{len(expected_icons)} Solar glyphs, {len(expected_hud_icons)} HUD glyphs")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Build the Figma HUD icon TTF and high-resolution MTSDF atlas."""

from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

import pathops
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.svgLib.path import parse_path


def sanitize_svg(source: Path, destination: Path) -> None:
    ET.register_namespace("", "http://www.w3.org/2000/svg")
    tree = ET.parse(source)
    root = tree.getroot()

    view_box = [float(value) for value in root.attrib.get("viewBox", "0 0 24 24").split()]
    if len(view_box) == 4:
        x, y, width, height = view_box
        side = max(width, height)
        root.set("viewBox", f"{x - (side - width) / 2:g} {y - (side - height) / 2:g} {side:g} {side:g}")
        root.set("width", f"{side:g}")
        root.set("height", f"{side:g}")

    for parent in list(root.iter()):
        for child in list(parent):
            if child.tag.rsplit("}", 1)[-1] in {"defs", "clipPath", "filter"}:
                parent.remove(child)

    for element in root.iter():
        element.attrib.pop("filter", None)
        element.attrib.pop("clip-path", None)
        element.attrib.pop("style", None)
        tag = element.tag.rsplit("}", 1)[-1]
        if tag != "path" or not element.get("d"):
            continue

        fill_rule = (element.get("fill-rule") or element.get("clip-rule") or "").lower()
        if fill_rule == "evenodd":
            source_path = pathops.Path()
            parse_path(element.get("d"), source_path.getPen())
            source_path.fillType = pathops.FillType.EVEN_ODD
            normalized = pathops.simplify(source_path, fix_winding=True)
            pen = SVGPathPen(None)
            normalized.draw(pen)
            element.set("d", pen.getCommands())

        element.set("fill", "#000000")
        element.attrib.pop("fill-opacity", None)
        element.attrib.pop("fill-rule", None)
        element.attrib.pop("clip-rule", None)

    destination.parent.mkdir(parents=True, exist_ok=True)
    tree.write(destination, encoding="unicode")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--tools", type=Path, default=Path(".codex-temp/font-tools"))
    parser.add_argument(
        "--generator",
        type=Path,
        default=Path(r"C:\Users\<user>\Desktop\msdf-atlas-gen\msdf-atlas-gen.exe"),
    )
    args = parser.parse_args()

    root = args.root.resolve()
    tools = (root / args.tools).resolve() if not args.tools.is_absolute() else args.tools.resolve()
    fantasticon = tools / "node_modules" / ".bin" / "fantasticon.cmd"
    fantasticon_cli = tools / "node_modules" / "fantasticon" / "dist" / "cli" / "index.cjs"
    source_dir = Path(__file__).with_name("hud-icons")
    manifest = json.loads(Path(__file__).with_name("hud-icons.json").read_text(encoding="utf-8"))

    if not args.generator.is_file():
        raise SystemExit(f"msdf-atlas-gen is missing: {args.generator}")
    if not fantasticon.is_file() or not fantasticon_cli.is_file():
        tools.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["npm.cmd", "install", "--no-fund", "--no-audit", "--prefix", str(tools),
             "fantasticon@4.1.0"],
            check=True,
            cwd=root,
        )

    work = root / ".codex-temp" / "hud-font"
    svg_dir = work / "svg"
    font_dir = work / "font"
    shutil.rmtree(work, ignore_errors=True)
    svg_dir.mkdir(parents=True)
    font_dir.mkdir(parents=True)

    codepoints: dict[str, int] = {}
    for icon in manifest["icons"]:
        icon_id = icon["constant"].lower()
        sanitize_svg(source_dir / icon["file"], svg_dir / f"{icon_id}.svg")
        codepoints[icon_id] = int(icon["codepoint"], 16)

    config = {
        "inputDir": str(svg_dir),
        "outputDir": str(font_dir),
        "name": "hud-icons",
        "fontTypes": ["ttf"],
        "assetTypes": ["json"],
        "fontHeight": 1024,
        "normalize": True,
        "round": 1000000000000,
        "codepoints": codepoints,
        "formatOptions": {"json": {"indent": 2}},
    }
    config_path = work / "fantasticonrc.json"
    config_path.write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding="utf-8")

    cli_source = fantasticon_cli.read_text(encoding="utf-8")
    glob_line = "const globPath = (0, import_path6.join)(dir, `**/*.${ASSETS_EXTENSION}`);"
    fixed_glob_line = glob_line[:-1] + '.replace(/\\\\/g, "/");'
    if glob_line in cli_source:
        fantasticon_cli.write_text(cli_source.replace(glob_line, fixed_glob_line), encoding="utf-8")

    subprocess.run([str(fantasticon), "-c", str(config_path)], check=True, cwd=root)

    charset = work / "hud_charset.txt"
    charset.write_text(",\n".join(f"0x{value:04X}" for value in codepoints.values()) + "\n", encoding="utf-8")
    output = root / "src" / "main" / "resources" / "assets" / "enough" / "fonts" / "msdf"
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            str(args.generator),
            "-font", str(font_dir / "hud-icons.ttf"),
            "-fontname", "Enough HUD Icons",
            "-charset", str(charset),
            "-type", "mtsdf",
            "-size", "192",
            "-pxrange", "24",
            "-coloringstrategy", "distance",
            "-square4",
            "-imageout", str(output / "hud_icons.png"),
            "-json", str(output / "hud_icons.json"),
        ],
        check=True,
    )
    shutil.copyfile(font_dir / "hud-icons.ttf", Path(__file__).with_name("hud-icons.ttf"))
    print(f"Generated {len(codepoints)} Figma HUD glyphs at 192px MTSDF resolution")


if __name__ == "__main__":
    main()

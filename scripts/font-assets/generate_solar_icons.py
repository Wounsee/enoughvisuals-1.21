#!/usr/bin/env python3
"""Build the stable Enough UI icon TTF and MTSDF atlas.

User-provided SVG files take priority. Missing glyphs keep their pinned Solar
fallback so a partial custom delivery can never produce an incomplete atlas.
"""

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


CUSTOM_FILES = {
    "VISUAL": "visuals.svg",
    "WAYPOINT": "waypoint.svg",
    "SETTINGS": "settings.svg",
    "UTILS": "utils.svg",
    "FRIENDS": "friends.svg",
    "BIND": "keyboard.svg",
    "CLIENT": "settings.svg",
    "PUZZLE": "client(puzzle).svg",
    "SEARCH": "search.svg",
    "EVENTS": "events.svg",
    "INVENTORY": "inventory.svg",
    "COSMETICS": "cosmetic.svg",
    "MACROS": "macros.svg",
    "BOOK": "book.svg",
    "SAVE": "save.svg",
    "DELETE": "delete.svg",
    "FOLDER": "folder.svg",
    "ADD": "add.svg",
    "CLOSE": "close.svg",
    "PLAY": "play.svg",
    "LOCATION": "location.svg",
    "CLOCK": "cooldown.svg",
    "HEART": "heart.svg",
    "POTION": "potion.svg",
    "COOLDOWN": "cooldown.svg",
    "DRAG": "drag.svg",
    "DOWNLOAD": "download.svg",
    "REFRESH": "refresh.svg",
    "CHECK": "check.svg",
    "LINK": "link.svg",
    "USER": "people.svg",
    "SERVER": "server.svg",
    "TARGET": "target.svg",
    "LAYERS": "client.svg",
    "PIN": "pin.svg",
    "ARROW_RIGHT": "arrow_right.svg",
    "CHEVRON_DOWN": "chevron_down.svg",
    "CHEVRON_RIGHT": "chevron_right.svg",
    "CHEVRON_LEFT": "chevron_left.svg",
    "ARROW_LEFT": "arrow_left.svg",
    "ARROW_UP": "arrow_up.svg",
    "ARROW_DOWN": "arrow_down.svg",
    "MINUS": "minus.svg",
    "CHEVRON_UP": "chevron_up.svg",
    "MORE_HORIZONTAL": "more_horizontal.svg",
    "LANGUAGE": "language.svg",
    "DISCORD": "discord.svg",
    "TELEGRAM": "telegram.svg",
    "ROBOT": "robot.svg",
    "MEDAL_RIBBON_STAR": "esc_medal_ribbon_star.svg",
    "CHAT_SQUARE_STATS": "esc_chat_square_stats.svg",
    "FEEDBACK_STAR": "esc_feedback_star.svg",
    "BUG": "esc_bug.svg",
    "NETWORK_GLOBAL": "esc_global.svg",
}


def normalize_evenodd_paths(svg_path: Path) -> None:
    """Convert SVG even-odd contours to font-compatible non-zero winding.

    OpenType glyph outlines use the non-zero winding rule. Copying an SVG path
    that relies on ``fill-rule=evenodd`` directly into a TTF fills its holes
    (calendar dots, eye pupil gap, search lens, gear center). PathOps resolves
    the same geometry and rewinds the inner contours before Fantasticon sees it.
    """
    ET.register_namespace("", "http://www.w3.org/2000/svg")
    tree = ET.parse(svg_path)
    changed = False
    for element in tree.getroot().iter():
        if element.tag.rsplit("}", 1)[-1] != "path" or not element.get("d"):
            continue
        fill_rule = (element.get("fill-rule") or element.get("clip-rule") or "").lower()
        style = element.get("style", "").replace(" ", "").lower()
        if fill_rule != "evenodd" and "fill-rule:evenodd" not in style:
            continue

        source = pathops.Path()
        parse_path(element.get("d"), source.getPen())
        source.fillType = pathops.FillType.EVEN_ODD
        normalized = pathops.simplify(source, fix_winding=True)
        pen = SVGPathPen(None)
        normalized.draw(pen)
        element.set("d", pen.getCommands())
        element.attrib.pop("fill-rule", None)
        element.attrib.pop("clip-rule", None)
        if "fill-rule:evenodd" in style:
            element.set("style", ";".join(
                declaration for declaration in element.get("style", "").split(";")
                if not declaration.replace(" ", "").lower().startswith("fill-rule:")
            ))
        changed = True

    if changed:
        tree.write(svg_path, encoding="unicode")


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
    package = tools / "node_modules" / "@iconify-json" / "solar" / "icons.json"
    fantasticon = tools / "node_modules" / ".bin" / "fantasticon.cmd"
    fantasticon_cli = tools / "node_modules" / "fantasticon" / "dist" / "cli" / "index.cjs"
    manifest_path = Path(__file__).with_name("solar-icons.json")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    icon_set = json.loads(package.read_text(encoding="utf-8"))
    icons = icon_set["icons"]
    custom_dir = Path(__file__).with_name("custom-icons")
    stroke_outliner = Path(__file__).with_name("outline_svg_strokes.cjs")
    outline_module = tools / "node_modules" / "svg-outline-stroke"

    work = root / ".codex-temp" / "solar-font"
    svg_dir = work / "svg"
    font_dir = work / "font"
    shutil.rmtree(work, ignore_errors=True)
    svg_dir.mkdir(parents=True)
    font_dir.mkdir(parents=True)

    codepoints: dict[str, int] = {}
    custom_count = 0
    for mapping in manifest["icons"]:
        icon_id = mapping["constant"].lower()
        custom_name = CUSTOM_FILES.get(mapping["constant"], f"{icon_id}.svg")
        custom_source = custom_dir / custom_name
        if custom_source.is_file():
            custom_svg = custom_source.read_text(encoding="utf-8")
            destination = svg_dir / f"{icon_id}.svg"
            if "stroke=" in custom_svg and "none" in custom_svg:
                if not outline_module.is_dir():
                    raise FileNotFoundError(
                        "stroke-only SVG support is missing; install the pinned build dependency with "
                        f"`npm install --prefix {tools} --save-exact svg-outline-stroke@1.3.1`"
                    )
                subprocess.run(
                    ["node", str(stroke_outliner), str(custom_source), str(destination), str(tools)],
                    check=True,
                    cwd=root,
                )
            else:
                shutil.copyfile(custom_source, destination)
            normalize_evenodd_paths(destination)
            custom_count += 1
        else:
            solar_name = mapping["solar"]
            if solar_name not in icons:
                raise KeyError(f"Solar icon does not exist: {solar_name}")
            icon = icons[solar_name]
            width = icon.get("width", icon_set.get("width", 24))
            height = icon.get("height", icon_set.get("height", 24))
            body = icon["body"].replace("currentColor", "#000000")
            svg = (
                f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {height}" '
                f'width="{width}" height="{height}">{body}</svg>'
            )
            (svg_dir / f"{icon_id}.svg").write_text(svg, encoding="utf-8")
        codepoints[icon_id] = int(mapping["codepoint"], 16)

    config = {
        "inputDir": str(svg_dir),
        "outputDir": str(font_dir),
        "name": "solar-icons",
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

    # Fantasticon 4.1.0 combines the input path with a glob using Windows
    # backslashes, while its glob dependency expects forward slashes. Patch the
    # ignored, locally installed tool copy so the pinned version is reproducible
    # on Windows as well as Unix.
    cli_source = fantasticon_cli.read_text(encoding="utf-8")
    glob_line = "const globPath = (0, import_path6.join)(dir, `**/*.${ASSETS_EXTENSION}`);"
    fixed_glob_line = glob_line[:-1] + '.replace(/\\\\/g, "/");'
    if glob_line in cli_source:
        fantasticon_cli.write_text(cli_source.replace(glob_line, fixed_glob_line), encoding="utf-8")
    subprocess.run([str(fantasticon), "-c", str(config_path)], check=True, cwd=root)

    charset = work / "solar_charset.txt"
    charset.write_text(",\n".join(f"0x{value:04X}" for value in codepoints.values()) + "\n", encoding="utf-8")
    output = root / "src" / "main" / "resources" / "assets" / "enough" / "fonts" / "msdf"
    output.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            str(args.generator),
            "-font", str(font_dir / "solar-icons.ttf"),
            "-fontname", "Enough UI Icons",
            "-charset", str(charset),
            "-type", "mtsdf",
            "-size", "128",
            "-pxrange", "16",
            "-coloringstrategy", "distance",
            "-square4",
            "-imageout", str(output / "solar_icons.png"),
            "-json", str(output / "solar_icons.json"),
        ],
        check=True,
    )
    print(f"Generated {len(codepoints)} UI icons ({custom_count} custom, {len(codepoints) - custom_count} Solar fallbacks)")


if __name__ == "__main__":
    main()

from __future__ import annotations

import argparse
import json
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image


FRAME_SIZE = 96
FRAME_COUNT = 60
COLUMNS = 10
ROWS = 6


FLAGS = {
    "russia": "Flag Russia.webp",
    "ukraine": "Flag Ukraine.webp",
    "united_states": "Flag United States.webp",
    "germany": "Flag Germany.webp",
    "france": "Flag France.webp",
    "spain": "Flag Spain.webp",
    "italy": "Flag Italy.webp",
    "brazil": "Flag Brazil.webp",
    "poland": "Flag Poland.webp",
    "netherlands": "Flag Netherlands.webp",
    "czechia": "Flag Czechia.webp",
    "romania": "Flag Romania.webp",
    "hungary": "Flag Hungary.webp",
    "bulgaria": "Flag Bulgaria.webp",
    "croatia": "Flag Croatia.webp",
    "china": "Flag China.webp",
    "japan": "Flag Japan.webp",
    "south_korea": "Flag South Korea.webp",
    "saudi_arabia": "Flag Saudi Arabia.webp",
    "india": "Flag India.webp",
    "indonesia": "Flag Indonesia.webp",
    "vietnam": "Flag Vietnam.webp",
    "belarus": "Flag Belarus.webp",
    "lithuania": "Flag Lithuania.webp",
    "estonia": "Flag Estonia.webp",
    "ireland": "Flag Ireland.webp",
    "philippines": "Flag Philippines.webp",
    "armenia": "Flag Armenia.webp",
    "uzbekistan": "Flag Uzbekistan.webp",
    # The source pack has no Nordic entries. Reuse its waving flag silhouette
    # and recolor it to the official designs below.
    "sweden": "Flag Russia.webp",
    "denmark": "Flag Russia.webp",
    "finland": "Flag Russia.webp",
    "norway": "Flag Russia.webp",
}


def official_belarus_frame(frame: Image.Image) -> Image.Image:
    """Recolor the repository's historical Belarus flag to the current red-green flag."""
    rgba = frame.convert("RGBA")
    alpha = rgba.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        return rgba
    left, top, right, bottom = bounds
    width = max(1, right - left)
    height = max(1, bottom - top)
    pixels = rgba.load()
    for py in range(top, bottom):
        ny = (py - top) / height
        for px in range(left, right):
            opacity = pixels[px, py][3]
            if opacity == 0:
                continue
            nx = (px - left) / width
            shade = 0.88 + 0.12 * (1.0 - abs(nx * 2.0 - 1.0))
            if nx < 0.17:
                cell = max(2, width // 34)
                gx = (px - left) // cell
                gy = (py - top) // cell
                ornament = (gx + gy) % 4 in (0, 1) or (gx - gy) % 5 == 0
                base = (196, 28, 51) if ornament else (245, 242, 228)
            else:
                base = (200, 32, 54) if ny < 0.66 else (0, 125, 75)
            pixels[px, py] = tuple(round(channel * shade) for channel in base) + (opacity,)
    return rgba


def official_nordic_frame(frame: Image.Image, country: str) -> Image.Image:
    rgba = frame.convert("RGBA")
    alpha = rgba.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        return rgba
    left, top, right, bottom = bounds
    width = max(1, right - left)
    height = max(1, bottom - top)
    palettes = {
        "sweden": ((0, 106, 167), (254, 204, 0), None),
        "denmark": ((198, 12, 48), (255, 255, 255), None),
        "finland": ((255, 255, 255), (0, 53, 128), None),
        "norway": ((186, 12, 47), (255, 255, 255), (0, 32, 91)),
    }
    base, cross, inner = palettes[country]
    pixels = rgba.load()
    for py in range(top, bottom):
        ny = (py - top) / height
        for px in range(left, right):
            opacity = pixels[px, py][3]
            if opacity == 0:
                continue
            nx = (px - left) / width
            vertical = 0.285 <= nx <= 0.405
            horizontal = 0.42 <= ny <= 0.58
            color = cross if vertical or horizontal else base
            if inner is not None:
                inner_vertical = 0.315 <= nx <= 0.375
                inner_horizontal = 0.465 <= ny <= 0.535
                if inner_vertical or inner_horizontal:
                    color = inner
            shade = 0.88 + 0.12 * (1.0 - abs(nx * 2.0 - 1.0))
            pixels[px, py] = tuple(round(channel * shade) for channel in color) + (opacity,)
    return rgba


def main() -> None:
    parser = argparse.ArgumentParser(description="Import Telegram animated flags as tiny GPU sprite sheets")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()

    root = args.root.resolve()
    temporary = root / ".codex-temp" / "language-flags"
    output = root / "src/main/resources/assets/enough/textures/language_flags"
    temporary.mkdir(parents=True, exist_ok=True)
    output.mkdir(parents=True, exist_ok=True)

    base = "https://raw.githubusercontent.com/Tarikul-Islam-Anik/Telegram-Animated-Emojis/main/Flags/"
    for asset, source_name in FLAGS.items():
        source = temporary / f"{asset}.webp"
        url = base + urllib.parse.quote(source_name)
        if not source.is_file():
            urllib.request.urlretrieve(url, source)
        image = Image.open(source)
        source_frames = max(1, getattr(image, "n_frames", 1))
        decoded = []
        for source_index in range(source_frames):
            image.seek(source_index)
            frame = image.convert("RGBA")
            if asset == "belarus":
                frame = official_belarus_frame(frame)
            elif asset in {"sweden", "denmark", "finland", "norway"}:
                frame = official_nordic_frame(frame, asset)
            decoded.append(frame)

        atlas = Image.new("RGBA", (FRAME_SIZE * COLUMNS, FRAME_SIZE * ROWS), (0, 0, 0, 0))
        for frame_index in range(FRAME_COUNT):
            position = frame_index * (source_frames - 1) / max(1, FRAME_COUNT - 1)
            lower = int(position)
            upper = min(source_frames - 1, lower + 1)
            mix = position - lower
            frame = decoded[lower] if upper == lower else Image.blend(decoded[lower], decoded[upper], mix)
            frame.thumbnail((FRAME_SIZE, FRAME_SIZE), Image.Resampling.LANCZOS)
            cell = Image.new("RGBA", (FRAME_SIZE, FRAME_SIZE), (0, 0, 0, 0))
            cell.alpha_composite(frame, ((FRAME_SIZE - frame.width) // 2, (FRAME_SIZE - frame.height) // 2))
            atlas.alpha_composite(cell, ((frame_index % COLUMNS) * FRAME_SIZE,
                                          (frame_index // COLUMNS) * FRAME_SIZE))
        atlas.save(output / f"{asset}.png", optimize=True)

    metadata = {
        "source": "Tarikul-Islam-Anik/Telegram-Animated-Emojis/Flags",
        "frameWidth": FRAME_SIZE,
        "frameHeight": FRAME_SIZE,
        "columns": COLUMNS,
        "rows": ROWS,
        "frames": FRAME_COUNT,
        "fps": 60,
        "idleFrame": FRAME_COUNT - 1,
        "playback": "once_on_selection",
        "assets": sorted(FLAGS),
    }
    (output / "atlas.json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(f"Imported {len(FLAGS)} flags as {FRAME_SIZE * COLUMNS}x{FRAME_SIZE * ROWS} sprite sheets")


if __name__ == "__main__":
    main()

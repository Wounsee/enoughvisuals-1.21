"""Bake an isolated Gaussian privacy mask from the bundled Inter atlas.

The source is synthetic: actual player coordinates never enter this texture.
Requires Pillow. Run from any directory; no platform fonts are needed.
"""
import json
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
FONT = ROOT / "src/main/resources/assets/enough/fonts/msdf"
OUTPUT = ROOT / "src/main/resources/assets/enough/textures/hud/coordinate_blur.png"
SCALE = 8
WIDTH, HEIGHT = 84 * SCALE, 16 * SCALE
SIZE = 7 * SCALE
TEXT = "x: 1286  y: 64  z: -357"


def main():
    data = json.loads((FONT / "medium.json").read_text(encoding="utf-8"))
    atlas = Image.open(FONT / "medium.png").convert("RGBA").getchannel("A")
    glyphs = {chr(g["unicode"]): g for g in data["glyphs"]}
    mask = Image.new("L", (WIDTH, HEIGHT))
    pen = (WIDTH - sum(glyphs[c]["advance"] for c in TEXT) * SIZE) / 2
    baseline = HEIGHT / 2 + SIZE * 0.36
    distance_range = data["atlas"]["distanceRange"]
    coverage = [round(255 * min(1, max(0, (v / 255 - 0.5) * distance_range + 0.5)))
                for v in range(256)]
    for char in TEXT:
        glyph = glyphs[char]
        if "atlasBounds" in glyph:
            a, p = glyph["atlasBounds"], glyph["planeBounds"]
            tile = atlas.crop((round(a["left"]), round(atlas.height - a["top"]),
                               round(a["right"]), round(atlas.height - a["bottom"])))
            tile = tile.point(coverage).resize(
                (round((p["right"] - p["left"]) * SIZE),
                 round((p["top"] - p["bottom"]) * SIZE)), Image.Resampling.LANCZOS)
            mask.paste(tile, (round(pen + p["left"] * SIZE), round(baseline - p["top"] * SIZE)))
        pen += glyph["advance"] * SIZE

    # Convolve the complete alpha image, rather than repeating glyphs with
    # quantized draw alpha (which leaves a visible lattice at small HUD sizes).
    blurred = mask.filter(ImageFilter.GaussianBlur(2.6 * SCALE))
    blurred = blurred.point(lambda value: min(255, round(value * 1.65)))
    result = Image.new("RGBA", mask.size, (218, 218, 224, 0))
    result.putalpha(blurred)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    result.save(OUTPUT)


if __name__ == "__main__":
    main()

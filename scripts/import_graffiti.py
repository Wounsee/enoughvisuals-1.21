from __future__ import annotations

import argparse
import hashlib
import json
import math
import shutil
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageSequence


STATIC_SIZE = 256
ANIM_SIZE = 128
ANIM_FPS = 15.0
WEBP_FPS = 20.0
MAX_FRAMES = 60


def asset_id(path: Path) -> str:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()[:12]
    return f"g_{digest}"


def title_for(root: Path, path: Path) -> str:
    return path.parent.name if path.name.lower() == "image.png" else path.stem


def contain(image: Image.Image, size: int) -> tuple[Image.Image, float]:
    image = image.convert("RGBA")
    width, height = image.size
    aspect = width / max(1, height)
    scale = min(size / max(1, width), size / max(1, height))
    resized = image.resize((max(1, round(width * scale)), max(1, round(height * scale))), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size - resized.width) // 2, (size - resized.height) // 2))
    return canvas, aspect


def decode_frames(path: Path) -> tuple[list[Image.Image], float]:
    suffix = path.suffix.lower()
    if suffix == ".webm":
        with tempfile.TemporaryDirectory(prefix="enough-graffiti-") as temp:
            pattern = str(Path(temp) / "%04d.png")
            subprocess.run([
                "ffmpeg", "-v", "error", "-i", str(path), "-vf",
                f"fps={ANIM_FPS}", "-frames:v", str(MAX_FRAMES), pattern,
            ], check=True)
            frames = [Image.open(item).convert("RGBA").copy() for item in sorted(Path(temp).glob("*.png"))]
        return frames, ANIM_FPS

    image = Image.open(path)
    if getattr(image, "is_animated", False):
        frames = [frame.convert("RGBA").copy() for frame in ImageSequence.Iterator(image)][:MAX_FRAMES]
        durations = []
        for index in range(len(frames)):
            image.seek(index)
            duration = image.info.get("duration")
            if isinstance(duration, (int, float)) and duration > 0:
                durations.append(float(duration))
        fps = 1000.0 / (sum(durations) / len(durations)) if durations else WEBP_FPS
        return frames, max(1.0, min(60.0, fps))
    return [image.convert("RGBA").copy()], 0.0


def build_atlas(frames: list[Image.Image], cell: int) -> tuple[Image.Image, int, int, float]:
    columns = max(1, math.ceil(math.sqrt(len(frames))))
    rows = max(1, math.ceil(len(frames) / columns))
    atlas = Image.new("RGBA", (columns * cell, rows * cell), (0, 0, 0, 0))
    aspect = 1.0
    for index, frame in enumerate(frames):
        ready, frame_aspect = contain(frame, cell)
        if index == 0:
            aspect = frame_aspect
        atlas.alpha_composite(ready, ((index % columns) * cell, (index // columns) * cell))
    return atlas, columns, rows, aspect


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    source = args.source.resolve()
    output = args.output.resolve()
    textures = output / "textures"
    if textures.exists():
        shutil.rmtree(textures)
    textures.mkdir(parents=True, exist_ok=True)

    entries = []
    files = sorted((item for item in source.rglob("*") if item.is_file()), key=lambda item: str(item).casefold())
    for order, path in enumerate(files):
        frames, fps = decode_frames(path)
        if not frames:
            continue
        animated = len(frames) > 1
        cell = ANIM_SIZE if animated else STATIC_SIZE
        atlas, columns, rows, aspect = build_atlas(frames, cell)
        identifier = asset_id(path)
        texture_name = f"{identifier}.png"
        atlas.save(textures / texture_name, optimize=True)
        entries.append({
            "id": identifier,
            "title": title_for(source, path),
            "texture": f"graffiti/textures/{texture_name}",
            "frames": len(frames),
            "fps": round(fps, 4) if animated else 0,
            "columns": columns,
            "rows": rows,
            "aspect": round(aspect, 6),
            "order": order,
        })

    (output / "catalog.json").write_text(
        json.dumps({"version": 1, "entries": entries}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Imported {len(entries)} graffiti assets into {output}")


if __name__ == "__main__":
    main()

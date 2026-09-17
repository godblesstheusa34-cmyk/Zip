#!/usr/bin/env python3
"""Create a source archive suitable for Android Studio or command-line builds."""
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

ROOT = Path(__file__).resolve().parent
OUTPUT = ROOT / "FluidLauncher_S24Ultra.zip"
SKIP_PARTS = {".git", ".gradle", ".idea", "build", "__pycache__"}

with ZipFile(OUTPUT, "w", ZIP_DEFLATED) as archive:
    for path in sorted(ROOT.rglob("*")):
        if path == OUTPUT or not path.is_file() or any(part in SKIP_PARTS for part in path.parts):
            continue
        archive.write(path, Path("FluidLauncher") / path.relative_to(ROOT))

print(f"Created {OUTPUT.name}")

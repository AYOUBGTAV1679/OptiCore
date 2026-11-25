#!/usr/bin/env python3
"""Verify the packaged Forge JAR contains its Mixin registration and refmap."""
import argparse
from pathlib import Path
from zipfile import BadZipFile, ZipFile


def manifest_attributes(raw_manifest: str) -> dict[str, str]:
    """Parse manifest attributes, including RFC-style continuation lines."""
    logical_lines = []
    for line in raw_manifest.replace("\r\n", "\n").replace("\r", "\n").split("\n"):
        if line.startswith(" ") and logical_lines:
            logical_lines[-1] += line[1:]
        else:
            logical_lines.append(line)

    attrs = {}
    for line in logical_lines:
        if ": " in line:
            key, value = line.split(": ", 1)
            attrs[key.lower()] = value.strip()
    return attrs


def inspect_jars(directory: Path) -> list[str]:
    jars = sorted(directory.glob("*.jar"))
    if not jars:
        return [f"No mod JAR found under {directory}"]

    errors = []
    for jar_path in jars:
        try:
            with ZipFile(jar_path) as jar:
                names = set(jar.namelist())
                manifest_name = "META-INF/MANIFEST.MF"
                if manifest_name not in names:
                    errors.append(f"{jar_path.name}: missing {manifest_name}")
                    continue

                raw_manifest = jar.read(manifest_name).decode("utf-8", errors="replace")
                attrs = manifest_attributes(raw_manifest)
                configs = [
                    item.strip()
                    for item in attrs.get("mixinconfigs", "").split(",")
                    if item.strip()
                ]
                if "opticore.mixins.json" not in configs:
                    errors.append(f"{jar_path.name}: manifest lacks MixinConfigs=opticore.mixins.json")
                if "opticore.mixins.json" not in names:
                    errors.append(f"{jar_path.name}: missing opticore.mixins.json resource")
                if "opticore.refmap.json" not in names:
                    errors.append(f"{jar_path.name}: missing opticore.refmap.json")
        except (BadZipFile, OSError, UnicodeError) as exc:
            errors.append(f"{jar_path.name}: cannot inspect archive: {exc}")
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", nargs="?", type=Path, default=Path("build/libs"))
    args = parser.parse_args()
    errors = inspect_jars(args.directory)
    if errors:
        raise SystemExit("\n".join(errors))
    count = len(list(args.directory.glob("*.jar")))
    print(f"Packaged Mixin check: OK ({count} JAR(s), manifest/config/refmap present)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

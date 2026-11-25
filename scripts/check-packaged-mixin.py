#!/usr/bin/env python3
"""Verify the packaged Forge JAR contains its Mixin registration and refmap."""
from pathlib import Path
from zipfile import ZipFile

jars = sorted(Path("build/libs").glob("*.jar"))
assert jars, "No mod JAR found under build/libs"

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
            # Manifest continuation lines begin with one space.
            logical_lines = []
            for line in raw_manifest.replace("\r\n", "\n").split("\n"):
                if line.startswith(" ") and logical_lines:
                    logical_lines[-1] += line[1:]
                else:
                    logical_lines.append(line)
            attrs = {}
            for line in logical_lines:
                if ": " in line:
                    key, value = line.split(": ", 1)
                    attrs[key.lower()] = value.strip()

            configs = [item.strip() for item in attrs.get("mixinconfigs", "").split(",") if item.strip()]
            if "opticore.mixins.json" not in configs:
                errors.append(f"{jar_path.name}: manifest lacks MixinConfigs=opticore.mixins.json")
            if "opticore.mixins.json" not in names:
                errors.append(f"{jar_path.name}: missing opticore.mixins.json resource")
            if "opticore.refmap.json" not in names:
                errors.append(f"{jar_path.name}: missing opticore.refmap.json")
    except Exception as exc:
        errors.append(f"{jar_path.name}: cannot inspect archive: {exc}")

assert not errors, "\n".join(errors)
print(f"Packaged Mixin check: OK ({len(jars)} JAR(s), manifest/config/refmap present)")

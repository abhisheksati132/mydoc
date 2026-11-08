"""Prepare pinned LibreOffice Android binaries and compile the small worker network guard.

The APKs are upstream F-Droid builds, not installed or executed by this script.
Only native libraries and engine assets are included; none of the upstream app UI is used.
"""
import argparse
import hashlib
import io
import json
import os
import re
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

REPO = Path(__file__).resolve().parents[1]
LOCK = REPO / "third_party/libreoffice/engine.lock.json"
OUTPUT = REPO / "app/build/generated/word-engine"


def digest(path):
    result = hashlib.sha256()
    with path.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            result.update(chunk)
    return result.hexdigest()


def package(spec, cache):
    target = cache / f"lo-{spec['code']}.apk"
    if target.is_file() and digest(target) == spec["sha256"]:
        return target
    url = f"https://f-droid.org/repo/org.documentfoundation.libreoffice_{spec['code']}.apk"
    for attempt in range(3):
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(dir=cache, suffix=".download", delete=False) as out:
                temporary = Path(out.name)
                with urllib.request.urlopen(url, timeout=120) as response:
                    total = 0
                    while chunk := response.read(1024 * 1024):
                        total += len(chunk)
                        if total > 150 * 1024 * 1024:
                            raise RuntimeError("Word engine package exceeds its expected size")
                        out.write(chunk)
            if digest(temporary) != spec["sha256"]:
                raise RuntimeError("Word engine package checksum mismatch")
            os.replace(temporary, target)
            return target
        except Exception:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
            if attempt == 2:
                raise
            time.sleep(2)


def asset_digest(name, data):
    # Upstream icon archives differ only in ZIP metadata across architectures.
    if name.endswith("images_colibre.zip"):
        with zipfile.ZipFile(io.BytesIO(data)) as icons:
            return sorted((n, hashlib.sha256(icons.read(n)).hexdigest()) for n in icons.namelist())
    if name.endswith("share/registry/main.xcd"):
        root = ET.fromstring(data)
        # The ARMv7 build has an additional empty component-data element (a no-op).
        for child in list(root):
            if child.tag.endswith("component-data") and not len(child) and not (child.text or "").strip():
                root.remove(child)
        return ET.canonicalize(ET.tostring(root, encoding="unicode"), strip_text=True)
    return hashlib.sha256(data).hexdigest()


def safe_target(root, relative):
    parts = PurePosixPath(relative).parts
    if not parts or ".." in parts or "\\" in relative or relative.startswith("/"):
        raise RuntimeError("Unsafe engine package path")
    target = root.joinpath(*parts)
    if not target.resolve().is_relative_to(root.resolve()):
        raise RuntimeError("Engine package path escapes its output")
    target.parent.mkdir(parents=True, exist_ok=True)
    return target


def prepare(sdk):
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    cache = Path(os.environ.get("MYDOC_WORD_ENGINE_CACHE", str(Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle"))) / "caches/mydoc-word-engine")))
    cache.mkdir(parents=True, exist_ok=True)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    common = None
    for spec in lock["packages"]:
        print(f"Preparing Word engine {lock['version']} for {spec['abi']}", flush=True)
        apk = package(spec, cache)
        signers = sorted((sdk / "build-tools").glob("*/apksigner.bat" if sys.platform == "win32" else "*/apksigner"))
        if not signers:
            raise RuntimeError("Android build-tools/apksigner is required to verify the Word engine")
        verification = subprocess.run([str(signers[-1]), "verify", "--print-certs", str(apk)], check=True, capture_output=True, text=True)
        certificates = re.findall(r"certificate SHA-256 digest: ([0-9a-f]{64})", verification.stdout)
        if certificates != [lock["certificateSha256"]]:
            raise RuntimeError("Word engine package signing certificate mismatch")
        with zipfile.ZipFile(apk) as archive:
            assets = {}
            for entry in archive.infolist():
                if entry.is_dir():
                    continue
                name = entry.filename
                if name.startswith(f"lib/{spec['abi']}/"):
                    target = safe_target(OUTPUT / "lib", name.removeprefix("lib/"))
                    with archive.open(entry) as source, target.open("wb") as out:
                        shutil.copyfileobj(source, out)
                elif name.startswith("assets/") and not name.startswith("assets/dexopt/"):
                    data = archive.read(entry)
                    assets[name] = asset_digest(name, data)
                    if common is None:
                        safe_target(OUTPUT / "assets", name.removeprefix("assets/")).write_bytes(data)
            if common is None:
                common = assets
            elif common != assets:
                raise RuntimeError("Word engine assets differ semantically across architectures")

    ndk = sdk / "ndk/29.0.14206865"
    host = "windows-x86_64" if sys.platform == "win32" else ("darwin-x86_64" if sys.platform == "darwin" else "linux-x86_64")
    clang = ndk / f"toolchains/llvm/prebuilt/{host}/bin" / ("clang.exe" if sys.platform == "win32" else "clang")
    if not clang.is_file():
        raise RuntimeError("Install Android NDK 29.0.14206865 before preparing the Word engine")
    targets = {"x86_64": "x86_64-linux-android26", "armeabi-v7a": "armv7a-linux-androideabi26", "arm64-v8a": "aarch64-linux-android26"}
    for abi, target in targets.items():
        subprocess.run([str(clang), f"--target={target}", "-shared", "-fPIC", "-Oz", "-Wall", "-Wextra", "-Werror",
                        "-fstack-protector-strong", "-D_FORTIFY_SOURCE=2", "-Wl,-z,max-page-size=16384,-z,relro,-z,now",
                        str(REPO / "app/src/main/cpp/word_guard.c"), "-o", str(OUTPUT / "lib" / abi / "libwordguard.so")], check=True)
    (OUTPUT / "prepared.json").write_text(json.dumps({"version": lock["version"], "lockSha256": digest(LOCK)}), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--sdk", required=True, type=Path)
    parser.add_argument("--install-ndk", action="store_true")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    options = parser.parse_args()
    OUTPUT = options.output.resolve()
    sdk = options.sdk.resolve()
    if options.install_ndk and not (sdk / "ndk/29.0.14206865/source.properties").is_file():
        tools = sdk / "cmdline-tools/latest/bin"
        modern = tools / ("android.exe" if sys.platform == "win32" else "android")
        command = [str(modern), "sdk", "install", "ndk/29.0.14206865"] if modern.is_file() else [str(tools / ("sdkmanager.bat" if sys.platform == "win32" else "sdkmanager")), "ndk;29.0.14206865"]
        subprocess.run(command, check=True)
    prepare(sdk)

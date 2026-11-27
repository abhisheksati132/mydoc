"""Build pinned official 7-Zip as Android executables, with no network access."""
import argparse
import hashlib
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tarfile

SOURCE_SHA256 = "a4e374c8f864f51df48c2cfd0e1413ec3b2ea030deae852d6a655402ebb61ac4"
TARGETS = {
    "arm64-v8a": "aarch64-linux-android26",
    "armeabi-v7a": "armv7a-linux-androideabi26",
    "x86": "i686-linux-android26",
    "x86_64": "x86_64-linux-android26",
}
OBJECT_DIRS = {"arm64-v8a": "a64", "armeabi-v7a": "a32", "x86": "x32", "x86_64": "x64"}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--sdk", required=True)
    parser.add_argument("--ndk-version", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--abi", action="append", choices=TARGETS)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parent.parent
    archive = repo / "third_party/7zip/7z2606-src.tar.xz"
    if hashlib.sha256(archive.read_bytes()).hexdigest() != SOURCE_SHA256:
        raise RuntimeError("7-Zip source digest mismatch")
    output = Path(args.output).resolve()
    build_root = (repo / "app/build").resolve()
    if not output.is_relative_to(build_root):
        raise RuntimeError("Engine output must stay inside app/build")
    work = output.parent / "archive-engine-work"
    source = work / "source"
    marker = work / "source.sha256"
    if not marker.exists() or marker.read_text() != SOURCE_SHA256:
        if source.exists():
            if not source.resolve().is_relative_to(build_root) or source.is_symlink():
                raise RuntimeError("Invalid engine build directory")
            shutil.rmtree(source)
        source.mkdir(parents=True)
        with tarfile.open(archive) as entries:
            entries.extractall(source, filter="data")
        marker.write_text(SOURCE_SHA256)
    # Bionic's stat.st_nlink is wider than nlink_t on x86_64. Preserve the full
    # value rather than suppressing Clang's narrowing diagnostic.
    file_info = source / "CPP/Windows/FileFind.h"
    before = b"  nlink_t nlink;"
    after = b"  UInt64 nlink; // MyDoc Android: retain stat.st_nlink without narrowing."
    content = file_info.read_bytes()
    if before in content:
        if content.count(before) != 1:
            raise RuntimeError("Unexpected upstream link-count declaration")
        file_info.write_bytes(content.replace(before, after))
    elif after not in content:
        raise RuntimeError("Upstream link-count patch no longer applies")
    patches = {
        "CPP/Windows/FileFind.h": [
            (b"  mode_t mode;", b"  UInt32 mode; // MyDoc Android: preserve stat mode width."),
            (b"  dev_t dev;", b"  UInt64 dev;"),
            (b"  ino_t ino;", b"  UInt64 ino;"),
            (b"  ino_t iNode;", b"  UInt64 iNode;"),
            (b"  dev_t rdev;", b"  UInt64 rdev;")],
        "CPP/Windows/FileDir.cpp": [
            (b"  mode = st.st_mode;", b"  mode = static_cast<mode_t>(st.st_mode & 0xFFFFu);"),
            (b"    mode = attrib >> 16;", b"    mode = static_cast<mode_t>(attrib >> 16);")],
        "CPP/7zip/Archive/MslzHandler.cpp": [
            (b"(size_t)(outPtr - outBuf - k_WinSize)", b"static_cast<size_t>(outPtr - outBuf) - k_WinSize")],
    }
    for relative, replacements in patches.items():
        path = source / relative
        data = path.read_bytes()
        for old, new in replacements:
            if old in data:
                if data.count(old) != 1:
                    raise RuntimeError("Unexpected upstream mode declaration")
                data = data.replace(old, new)
            elif new not in data:
                raise RuntimeError("Upstream mode patch no longer applies")
        path.write_bytes(data)
    windows = platform.system() == "Windows"
    host = {"Windows": "windows-x86_64", "Linux": "linux-x86_64", "Darwin": "darwin-x86_64"}[platform.system()]
    ndk = Path(args.sdk) / "ndk" / args.ndk_version
    tools = ndk / "toolchains/llvm/prebuilt" / host / "bin"
    suffix = ".exe" if windows else ""
    compiler = tools / ("clang++" + suffix)
    make = ndk / "prebuilt" / host / "bin" / ("make" + suffix)
    make_path = str(make) if make.exists() else shutil.which("make")
    if not compiler.exists() or not make_path:
        raise RuntimeError("Install the pinned Android NDK and GNU make")
    env = os.environ.copy()
    for key in list(env):
        if key.lower() == "systemdrive":
            env.pop(key)
    shell_args = []
    if windows:
        git = Path(shutil.which("git") or "C:/Program Files/Git/cmd/git.exe").resolve().parent.parent
        bash = git / "bin/bash.exe"
        if not bash.exists():
            raise RuntimeError("Git for Windows bash is required for the upstream makefiles")
        env["PATH"] = str(git / "usr/bin") + os.pathsep + env["PATH"]
        shell_args = ["SHELL=" + bash.as_posix()]
    bundle = source / "CPP/7zip/Bundles/Alone2"
    for abi in args.abi or TARGETS:
        target = TARGETS[abi]
        # Upstream expands every object on the link line. Short names keep the
        # Windows command below its 8191-character shell limit.
        object_dir = OBJECT_DIRS[abi]
        objects = bundle / object_dir
        fingerprint = hashlib.sha256(Path(__file__).read_bytes() +
            (repo / "app/src/main/cpp/archive_entry.cpp").read_bytes() +
            (repo / "app/src/main/cpp/archive_bridge.h").read_bytes() +
            (repo / "app/src/main/cpp/archive_files.c").read_bytes() +
            (ndk / "source.properties").read_bytes() + target.encode()).hexdigest()
        object_stamp = objects / "build.sha256"
        if objects.exists() and (not object_stamp.exists() or object_stamp.read_text() != fingerprint):
            if not objects.resolve().is_relative_to(build_root) or objects.is_symlink():
                raise RuntimeError("Invalid native object directory")
            shutil.rmtree(objects)
        objects.mkdir(exist_ok=True)
        shutil.copy2(repo / "app/src/main/cpp/archive_bridge.h", objects / "mydoc-bridge.h")
        shim = objects / "mydoc-entry.o"
        subprocess.run([str(compiler), "--target=" + target, "-O2", "-Wall", "-Wextra", "-Werror", "-c",
                        str(repo / "app/src/main/cpp/archive_entry.cpp"), "-o", str(shim)], check=True)
        command = [make_path, "-j2", "-f", "../../cmpl_clang.mak",
                   "O=" + object_dir,
                   'CC="' + (tools / ("clang" + suffix)).as_posix() + '" --target=' + target,
                   'CXX="' + compiler.as_posix() + '" --target=' + target,
                   "CXXFLAGS_EXTRA=-DZ7_AFFINITY_DISABLE -Dmain=mydoc_engine_main -include " + object_dir + "/mydoc-bridge.h",
                   "LIB2=-ldl", 'MY_LIBS="' + shim.as_posix() + '"',
                   "LDFLAGS_STATIC_2=-static-libstdc++", "LFLAGS_NOEXECSTACK=-Wl,-z,noexecstack",
                   "LFLAGS_STRIP=-Wl,-z,max-page-size=16384", *shell_args]
        print("Building official 7-Zip 26.06 for " + abi, flush=True)
        subprocess.run(command, cwd=bundle, env=env, check=True)
        native = output / abi / "libmydoc7z.so"
        native.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(objects / "7zz", native)
        subprocess.run([str(tools / ("llvm-strip" + suffix)), "--strip-unneeded", str(native)], check=True)
        subprocess.run([str(tools / ("clang" + suffix)), "--target=" + target,
                        "-shared", "-fPIC", "-O2", "-Wall", "-Wextra", "-Werror",
                        "-Wl,-z,max-page-size=16384", "-Wl,-z,relro", "-Wl,-z,now",
                        str(repo / "app/src/main/cpp/archive_files.c"), "-o",
                        str(native.parent / "libmydocarchiveio.so")], check=True)
        object_stamp.write_text(fingerprint)


if __name__ == "__main__":
    main()

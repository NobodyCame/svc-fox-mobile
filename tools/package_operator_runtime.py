"""Create portable operator runtimes for the Fabric and Purpur JARs."""
import hashlib
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request
import zipfile
from concurrent.futures import ThreadPoolExecutor

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / "build" / "operator-downloads"
OUTPUT = ROOT / "build" / "operator-runtimes"
RELEASE = "20261110"
RUNTIME = "3.11.18"
PLATFORMS = {
    "windows-x64": ("x86_64-pc-windows-msvc",
                    "f31751f4a8483f5ac7cd77ad83f8dfb80d9f2a7245bff1585e27516c777fb46f",
                    ["win_amd64"]),
    "linux-x64": ("x86_64-unknown-linux-gnu",
                  "3288c54cea3db350052019b3f8b558dd8a3da8afa6fda573f5fd2bcb37635b4a",
                  ["manylinux_2_28_x86_64", "manylinux2014_x86_64"]),
    "linux-arm64": ("aarch64-unknown-linux-gnu",
                    "7e71f26a003949b57757e287f4f4b6689da07e7a229f5bbee7919c19ed77155d",
                    ["manylinux_2_28_aarch64", "manylinux2014_aarch64"]),
}


def sha256(path):
    if not path.exists():
        return ""
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def get_archive(target, expected):
    archive = CACHE / (target + ".tar.gz")
    if sha256(archive) != expected:
        name = f"cpython-{RUNTIME}%2B{RELEASE}-{target}-install_only_stripped.tar.gz"
        url = f"https://github.com/astral-sh/python-build-standalone/releases/download/{RELEASE}/{name}"
        temp = archive.with_suffix(".download")
        with urllib.request.urlopen(url, timeout=90) as source, temp.open("wb") as output:
            shutil.copyfileobj(source, output)
        if sha256(temp) != expected:
            raise ValueError(f"CPython checksum mismatch for {target}")
        temp.replace(archive)
    return archive


def make_runtime(platform):
    target, checksum, wheel_platforms = PLATFORMS[platform]
    archive = get_archive(target, checksum)
    windows = platform.startswith("windows")
    packages = "Lib/site-packages" if windows else "lib/python3.11/site-packages"
    with tempfile.TemporaryDirectory() as temporary:
        wheels = Path(temporary) / "wheels"
        flags = [part for wheel in wheel_platforms for part in ("--platform", wheel)]
        subprocess.run([sys.executable, "-m", "pip", "download", "--only-binary=:all:", "--python-version", "3.11",
                        "--implementation", "cp", "--abi", "cp311", "--dest", str(wheels), *flags,
                        "cryptography==46.0.7", "cffi==2.0.0", "pycparser==2.23"], check=True)
        name = f"{platform}-{RELEASE}.zip"
        temp_zip = OUTPUT / (name + ".tmp")
        with tarfile.open(archive) as base, zipfile.ZipFile(temp_zip, "w", zipfile.ZIP_DEFLATED) as out:
            for entry in base:
                path = PurePosixPath(entry.name)
                if len(path.parts) < 2 or path.parts[0] != "python" or ".." in path.parts:
                    continue
                rel = PurePosixPath(*path.parts[1:])
                if packages.lower() in rel.as_posix().lower() or rel.parts[0] in ("include", "share"):
                    continue
                if {"test", "tests", "__pycache__", "ensurepip", "idlelib"}.intersection(rel.parts):
                    continue
                if not (entry.isfile() or entry.islnk() or entry.issym()):
                    continue
                data = base.extractfile(entry)
                if data is not None:
                    with data:
                        out.writestr(rel.as_posix(), data.read())
            for wheel in wheels.glob("*.whl"):
                with zipfile.ZipFile(wheel) as package:
                    for entry in package.infolist():
                        path = PurePosixPath(entry.filename)
                        if entry.is_dir() or path.is_absolute() or ".." in path.parts or ".data" in path.parts[0]:
                            continue
                        out.writestr(f"{packages}/{entry.filename}", package.read(entry))
        temp_zip.replace(OUTPUT / name)
        print(f"Created {name}", flush=True)


if __name__ == "__main__":
    CACHE.mkdir(parents=True, exist_ok=True)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with ThreadPoolExecutor(max_workers=3) as pool:
        list(pool.map(make_runtime, sys.argv[1:] or PLATFORMS))

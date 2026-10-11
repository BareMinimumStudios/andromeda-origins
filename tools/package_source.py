"""Create a flat, portable source ZIP and release checksums without development caches."""
from pathlib import Path
import hashlib, re, zipfile

ROOT=Path(__file__).resolve().parents[1]
properties=(ROOT/'gradle.properties').read_text(encoding='utf-8')
version=re.search(r'^mod_version=(.+)$',properties,re.M)[1].strip()
minecraft=re.search(r'^minecraft_version=(.+)$',properties,re.M)[1].strip()
release=ROOT/'build/release'
release.mkdir(parents=True,exist_ok=True)
source=release/f'andromeda-origins-{version}+{minecraft}-source.zip'
exclude={'.git','.idea','.gradle','.kotlin','build','run','out','__pycache__'}
paths=[p for p in ROOT.rglob('*') if p.is_file() and not p.is_symlink() and not any(part in exclude for part in p.relative_to(ROOT).parts)]
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for path in sorted(paths):
        name=path.relative_to(ROOT).as_posix()
        info=zipfile.ZipInfo.from_file(path,name)
        info.compress_type=zipfile.ZIP_DEFLATED
        if name in ('gradlew','build.sh'):info.external_attr=0o100755<<16
        archive.writestr(info,path.read_bytes())
with zipfile.ZipFile(source) as archive:
    names=archive.namelist()
    assert {'build.gradle.kts','README.md','LICENSE'}.issubset(names)
    assert not any(name.endswith('.java') for name in names)
    assert not any(name.startswith(('.git/','build/','run/')) for name in names)
    assert archive.testzip() is None
files=sorted([p for p in release.glob('*.jar') if f'{version}+{minecraft}' in p.name]+[source])
(release/'SHA256SUMS.txt').write_bytes(('\n'.join(f'{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}' for path in files)+'\n').encode('utf-8'))
print(f'Flat source ZIP: {len(paths)} files, {source.stat().st_size} bytes')

"""Validate packaged data independently of Gradle and catch known migration regressions."""
from pathlib import Path
import hashlib, json, re, sys

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
errors = []
def check(condition, message):
    if not condition: errors.append(message)
def unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result: raise ValueError(f'duplicate key: {key}')
        result[key] = value
    return result
def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():yield from walk(child)
    elif isinstance(value, list):
        for child in value:yield from walk(child)
data = {}
for path in RES.rglob('*.json'):
    try:data[path] = json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique)
    except (ValueError, UnicodeError) as error: errors.append(f'{path.relative_to(ROOT)}: {error}')
powers_dir = RES / 'data/andromeda_origins/powers'
powers = {f'andromeda_origins:{p.relative_to(powers_dir).as_posix()[:-5]}':v for p,v in data.items() if p.is_relative_to(powers_dir)}
origins = [(p,v) for p,v in data.items() if p.is_relative_to(RES/'data/andromeda_origins/origins')]
check(len(powers)==194, f'Expected 194 active root powers, found {len(powers)}')
check(len(origins)==26, f'Expected 26 Origins, found {len(origins)}')
known = set(powers)
for identifier, definition in powers.items():
    if definition.get('type') in ('origins:multiple','apoli:multiple'):
        known.update(f'{identifier}_{key}' for key,value in definition.items() if isinstance(value,dict) and 'type' in value)
for path,origin in origins:
    check(bool(origin.get('tags')), f'{path.name}: missing native Origin tags')
    for power in origin.get('powers',[]):
        if power.startswith('andromeda_origins:'):check(power in known,f'{path.name}: missing {power}')
for path,definition in data.items():
    for node in walk(definition):
        kind = node.get('type','')
        if kind in ('origins:feed','apoli:feed'):
            check('food' in node and 'nutrition' not in node,f'{path.name}: obsolete feed schema')
        if kind in ('origins:invulnerability','apoli:invulnerability'):
            check('damage_condition' in node,f'{path.name}: missing invulnerability condition')
        for key in ('resource','power'):
            identifier=node.get(key)
            if isinstance(identifier,str) and identifier.startswith('*:') and '*' not in identifier[2:]:
                identifier='andromeda_origins:'+identifier[2:]
            if isinstance(identifier,str) and identifier.startswith('andromeda_origins:'):
                check(identifier in known,f'{path.relative_to(RES)}: missing {key} {identifier}')
        command=node.get('command','')
        if isinstance(command,str):
            check('tp @s @p[tag=mantileap' not in command,f'{path.name}: unsafe caster selection')
            for name in re.findall(r'\bfunction\s+andromeda_origins:([\w/.-]+)',command):
                check((RES/f'data/andromeda_origins/function/{name}.mcfunction').exists(), f'{path.name}: missing function {name}')
# Follow the Gorgon power graph, including granted powers and multiple-power children.
# Both variants must remain free of wall climbing.
def root_power(identifier):
    if identifier.startswith('*:'):
        identifier = 'andromeda_origins:' + identifier[2:]
    if identifier in powers: return identifier
    return next((root for root in sorted(powers, key=len, reverse=True)
                 if identifier.startswith(root + '_')), None)

for path,origin in origins:
    if path.stem not in ('gorgon', 'champion_gorgon'): continue
    pending = list(origin.get('powers', []))
    visited = set()
    while pending:
        identifier = pending.pop()
        check('climb' not in identifier.lower(), f'{path.stem}: climbing power reference {identifier}')
        root = root_power(identifier)
        if root is None or root in visited: continue
        visited.add(root)
        for node in walk(powers[root]):
            check(node.get('type') not in ('origins:climbing', 'apoli:climbing'),
                  f'{path.stem}: wall climbing found in {root}')
            if isinstance(node.get('power'), str): pending.append(node['power'])

# The native Selkie reserve is measured in seconds, not ticks: 300 units,
# losing one unit every 20 ticks while outside water and rain.
wet = powers['andromeda_origins:selkie/helper/selkiewet']
check(wet['remove']['max'] == 300 and wet['remove']['start_value'] == 300,
      'Selkie hydration must start full and cap at five minutes')
check(wet['timer']['interval'] == 20, 'Selkie hydration must update once per second')
dry = wet['timer']['entity_action']['actions'][0]['actions'][1]
check(dry['action']['change'] == -1 and dry['action']['resource'] == '*:*_remove',
      'Selkie dry-land hydration must drain one second per second')
conditions = dry['condition']['conditions']
check(any(c.get('type') == 'origins:fluid_height' and c.get('compare_to') == 0
          and c.get('comparison') == '==' for c in conditions)
      and any(c.get('type') == 'origins:in_rain' and c.get('inverted') for c in conditions),
      'Selkie hydration must drain only outside water and rain')

# Lichling shares the same primary across standard and Champion Origins. Downed state
# must win over sneak targeting, or the cooldown can be consumed without self-revival.
lichling=powers['andromeda_origins:lichling/primary']
first=lichling['primary']['entity_action']['actions'][0]['condition']
check(first.get('type')=='origins:or' and
      first.get('conditions',[{}])[0].get('type')=='andromeda_origins:incapacitated',
      'Lichling self-revival must take priority while incapacitated')
for path,origin in origins:
    if path.stem in ('lichling','champion_lichling'):
        check('andromeda_origins:lichling/primary' in origin['powers'],
              f'{path.name}: shared self-revival power is missing')
sounds=data.get(RES/'assets/andromeda_origins/sounds.json',{})
referenced=set()
for definition in sounds.values():
    for entry in definition['sounds']:
        name=entry if isinstance(entry,str) else entry['name']
        namespace,name=name.split(':',1) if ':' in name else ('andromeda_origins',name)
        if namespace=='andromeda_origins':referenced.add(name+'.ogg')
actual={p.relative_to(RES/'assets/andromeda_origins/sounds').as_posix() for p in (RES/'assets/andromeda_origins/sounds').rglob('*.ogg')}
check(referenced==actual,f'Sound registry/files mismatch: missing={referenced-actual}, unregistered={actual-referenced}')
for cfg in (ROOT/'src/main').glob('*.mixins.json'):
    definition=json.loads(cfg.read_text())
    for name in definition.get('mixins',[])+definition.get('client',[]):
        package=definition['package']+'.'+name
        check((ROOT/'src/main/kotlin'/Path(package.replace('.','/')+'.kt')).exists(),f'Missing Kotlin Mixin {package}')
check(not list((ROOT/'src').rglob('*.java')),'Java sources remain in Kotlin project')
badge_dir=RES/'assets/andromeda_origins/textures/gui/badge'
check({p.relative_to(badge_dir).as_posix() for p in badge_dir.rglob('*.png')}=={'fallback.png'},'Unexpected copied badge assets or missing original fallback')
if (badge_dir/'fallback.png').exists():
    import struct
    check(struct.unpack('>II',(badge_dir/'fallback.png').read_bytes()[16:24])==(9,9),'Incorrect fallback badge dimensions')
for path in (ROOT/'src/main/kotlin/andromeda/origins/mixin').rglob('*.kt'):
    check('companion object' not in path.read_text(),f'Unsafe singleton scaffold in {path.name}')
spheres=data.get(RES/'assets/andromeda_origins/particle_spheres.json',{})
check(set(spheres)=={'soul','water'},'Particle sphere presets missing')
for name,points in spheres.items():check(len(points)==200 and all(len(p)==3 for p in points),f'{name}: incorrect sphere geometry')
manifest=json.loads((ROOT/'tools/selkie-baseline.json').read_text())
for name,digest in manifest.items():
    path=ROOT/name
    check(path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==digest,f'Selkie data changed: {name}')
for name in ('big_sphere','big_sphere2','web_nest','fireball','dragon_charge'):
    check(not list((RES/'data/andromeda_origins/function').rglob(name+'.mcfunction')),f'Retired helper remains: {name}')
if errors:
    print('\n'.join(errors),file=sys.stderr)
    sys.exit(1)
print(f'Validated {len(data)} JSON files, {len(powers)} powers, {len(origins)} Origins, {len(actual)} sounds, and {len(manifest)} unchanged Selkie files.')

"""Regenerates app/src/main/assets/knowledge.json (the assistant's knowledge base).
Edit part1.py / part2.py (topics) or extra.py (extra keywords), then run:  python3 build_knowledge.py
Each topic: e(id, title, "keyword, phrase, ...", "answer", action_feature_key, "related,ids", priority)."""
import json, os, sys
sys.path.insert(0, os.path.dirname(__file__))
from part1 import E
import part2
from extra import EXTRA, EXTRA2
by = {x['id']: x for x in E}
for D in (EXTRA, EXTRA2):
    for k, v in D.items():
        by[k]['kw'] += [s.strip() for s in v.split(',') if s.strip()]
for x in E:
    x['kw'] = list(dict.fromkeys(k.lower() for k in x['kw']))
ids = set(by)
for x in E:
    for r in x['rel']:
        assert r in ids, (x['id'], r)
out = os.path.join(os.path.dirname(__file__), '..', '..', 'app', 'src', 'main', 'assets', 'knowledge.json')
json.dump(E, open(out, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
print(len(E), 'topics written to', os.path.abspath(out))

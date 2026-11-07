#!/usr/bin/env python3
"""Fetch and report upstream changes; never merge, reset, install or alter app sources."""
import argparse,fnmatch,json,subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def git(*args):return subprocess.check_output(['git',*args],cwd=root,text=True).strip()
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--no-fetch',action='store_true');args=p.parse_args()
c=json.loads((root/'config/toto-upstream.json').read_text())
remote=c['remote'];branch=c['branch'];base=c['review_base'];ref=f'refs/remotes/{remote}/{branch}'
if git('remote','get-url',remote).removesuffix('.git').rstrip('/') != c['repository'].removesuffix('.git').rstrip('/'):
 raise SystemExit('Remote differs from the reviewed upstream. Review config before fetching.')
if not args.no_fetch:
 subprocess.run(['git','fetch','--no-tags',remote,f'refs/heads/{branch}:{ref}'],cwd=root,check=True)
head=git('rev-parse',ref)
if subprocess.run(['git','merge-base','--is-ancestor',base,head],cwd=root).returncode:
 raise SystemExit('Upstream history diverged from review_base. Stop for manual review.')
changed=git('diff','--name-only',base,head).splitlines()
protected=[x for x in changed if any(fnmatch.fnmatch(x,g) for g in c['protected_paths'])]
local=set(git('diff','--name-only',base,'HEAD').splitlines())|set(git('diff','--name-only','HEAD').splitlines())|set(git('ls-files','--others','--exclude-standard').splitlines())
conflicts=sorted(set(changed)&local)
commits=git('log','--reverse','--format=%h %s',f'{base}..{head}').splitlines()
report=['# Review cập nhật Mochy gốc','',f'- Mốc đã ghi nhận: `{base}`',f'- Upstream hiện tại: `{head}`',f'- Commit cần review: {len(commits)}',f'- Working tree có thay đổi: {bool(git("status","--porcelain"))}', '', 'Công cụ chỉ fetch/so sánh; không merge, reset, cài APK hoặc cập nhật mốc.', '', '## Commit',*['- '+x for x in commits], '', '## File upstream chạm phần bảo vệ',*['- `'+x+'`' for x in protected], '', '## File cả upstream lẫn bản cá nhân đã sửa',*['- `'+x+'`' for x in conflicts], '', '## Tất cả file upstream thay đổi',*['- `'+x+'`' for x in changed]]
out=root/'docs/verification/upstream-review.md';out.parent.mkdir(parents=True,exist_ok=True);out.write_text('\n'.join(report)+'\n')
print(f'Review ready: {len(commits)} commits; {len(changed)} changed files; {len(protected)} protected paths; {len(conflicts)} overlaps.')
print('Report: docs/verification/upstream-review.md. No updates applied.')

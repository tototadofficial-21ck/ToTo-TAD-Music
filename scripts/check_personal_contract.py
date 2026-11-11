#!/usr/bin/env python3
"""Regression guard for personal identity and integration points, not an authorization proof."""
import hashlib,json,xml.etree.ElementTree as E
from pathlib import Path
root=Path(__file__).resolve().parents[1];c=json.loads((root/'config/toto-upstream.json').read_text())
def read(p):return (root/p).read_text()
assert 'applicationId = "vn.ToTo.music"' in read('app/build.gradle.kts'), 'Application ID changed'
for p,h in c['asset_sha256'].items():assert hashlib.sha256((root/p).read_bytes()).hexdigest()==h, f'Personal asset changed: {p}'
for loc in ['values','values-vi']:
 p=root/f'app/src/main/res/{loc}/toto_strings.xml';assert p.exists()
assert 'ToTo TAD Music' in read('app/src/main/res/values/app_name.xml')
a='{http://schemas.android.com/apk/res/android}'
manifest=E.parse(root/'app/src/main/AndroidManifest.xml').getroot()
aliases=manifest.findall('application/activity-alias');assert len(aliases)==50, 'Launcher aliases changed; review explicitly'
assert all(x.get(a+'label')=='@string/app_name' for x in aliases), 'Launcher alias branding changed'
for p in ['auth/PersonalAuth.kt','auth/AuthGate.kt','auth/AccountProfile.kt','branding/OwnerSheet.kt']:
 assert (root/'app/src/main/kotlin/com/mochyy/mochymusic'/p).exists(), f'Personal integration missing: {p}'
assert 'PersonalAuth.restore(this)' in read('app/src/main/kotlin/com/mochyy/mochymusic/App.kt'), 'Cached-session startup removed'
assert 'PersonalAuth.logout(context)' in read('app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/settings/ProfileScreen.kt'), 'Profile sign-out removed'
assert 'PersonalAuth.allowed' in read('app/src/main/kotlin/com/mochyy/mochymusic/playback/MusicService.kt'), 'Playback authorization guard removed'
assert (root/'supabase/migrations/202611070001_account_profiles.sql').exists()
assert read('LICENSE').find('GNU GENERAL PUBLIC LICENSE')>=0, 'Original GPL license missing'
assert 'R.string.toto_ui_more_mochy_music_github_u2192_a5cc1a' not in read('app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/update/AboutSheet.kt'), 'Hidden upstream source action was restored'
assert 'LeadDevs.forEach' not in read('app/src/main/kotlin/com/mochyy/mochymusic/ui/screens/update/AboutSheet.kt'), 'Replaced upstream profile cards were restored'
assert 'signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null' in read('app/build.gradle.kts'), 'Release debug-signing fallback was restored'
print('PASS: personal ID, approved assets, aliases, auth/profile integrations and GPL preserved. Review and runtime tests still required.')

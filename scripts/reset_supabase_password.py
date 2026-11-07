#!/usr/bin/env python3
"""Optional admin-only password reset on the owner's computer; never package in APK."""
import getpass
import json
import uuid
import urllib.request
import urllib.error

url = input('Project URL (https://…supabase.co): ').strip().rstrip('/')
if not url.startswith('https://'):
    raise SystemExit('Cần Project URL HTTPS.')
user_id = str(uuid.UUID(input('UUID tài khoản từ Dashboard: ').strip()))
key = getpass.getpass('Secret hoặc service_role key (chỉ dùng trên máy admin): ')
password = getpass.getpass('Mật khẩu mới: ')
if password != getpass.getpass('Nhập lại mật khẩu: '):
    raise SystemExit('Mật khẩu không khớp.')
request = urllib.request.Request(
    f'{url}/auth/v1/admin/users/{user_id}',
    data=json.dumps({'password': password}).encode(),
    method='PUT',
    headers={'apikey': key, 'Authorization': f'Bearer {key}', 'Content-Type': 'application/json'},
)
try:
    with urllib.request.urlopen(request, timeout=20) as response:
        response.read()  # Never print user response, credentials or tokens.
except urllib.error.HTTPError as error:
    raise SystemExit(f'Không đổi được mật khẩu (HTTP {error.code}). Không in phản hồi để bảo vệ thông tin.')
except urllib.error.URLError:
    raise SystemExit('Không kết nối được Supabase.')
print('Đã đặt lại mật khẩu. Không lưu thông tin xác thực trên đĩa.')

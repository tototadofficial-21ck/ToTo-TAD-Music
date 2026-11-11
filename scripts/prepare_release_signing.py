#!/usr/bin/env python3
"""Run in your own Terminal. Passwords never go into command arguments, logs or the repository."""
import argparse,getpass,hashlib,json,os,shutil,subprocess,sys,tempfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PRIVATE=Path.home()/'.config/toto-tad-music'
KEY=PRIVATE/'toto-tad-music-release.p12'
PROPERTIES=PRIVATE/'release-signing.properties'
ALIAS='toto-tad-music'

def property_value(text):
    out=[]
    data=text.encode('utf-16-be')
    for i in range(0,len(data),2):
        c=int.from_bytes(data[i:i+2],'big')
        if c==92:out.append('\\\\')
        elif c<33 or c>126:out.append('\\u%04x'%c)
        else:out.append(chr(c))
    return ''.join(out)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check',action='store_true',help='Only check preparation status; no passwords or files are changed')
    args=parser.parse_args()
    if args.check:
        print('Keystore exists:',KEY.is_file())
        print('Private Gradle configuration exists:',PROPERTIES.is_file())
        return
    if not sys.stdin.isatty():raise SystemExit('Run this command in your own Terminal to enter the password privately.')
    if KEY.exists() or PROPERTIES.exists() or (ROOT/'config/toto-release-certificate.json').exists():raise SystemExit('A signing key or configuration already exists. Nothing was overwritten; preserve the existing key.')
    candidate=Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool')
    tool=str(candidate) if candidate.is_file() else shutil.which('keytool')
    if not tool:raise SystemExit('keytool was not found. Install/configure a JDK first.')
    print('Tạo khóa ký phát hành riêng cho ToTo TAD Music. Không gửi mật khẩu vào chat.')
    password=getpass.getpass('Mật khẩu keystore (ít nhất 12 ký tự): ')
    if len(password)<12 or any(ord(c)<32 for c in password):raise SystemExit('Use at least 12 characters, without control characters. No key was created.')
    if password!=getpass.getpass('Nhập lại mật khẩu: '):raise SystemExit('Passwords differ. No key was created.')
    PRIVATE.mkdir(parents=True,exist_ok=True,mode=0o700);PRIVATE.chmod(0o700)
    env=os.environ.copy();env['TOTO_KEYSTORE_PASSWORD']=password
    with tempfile.TemporaryDirectory(prefix='prepare-',dir=PRIVATE) as tmp:
        staged=Path(tmp)/'release.p12'
        cmd=[tool,'-genkeypair','-noprompt','-storetype','PKCS12','-keystore',str(staged),
             '-alias',ALIAS,'-keyalg','RSA','-keysize','3072','-validity','10000',
             '-dname','CN=ToTo TAD Music','-storepass:env','TOTO_KEYSTORE_PASSWORD','-keypass:env','TOTO_KEYSTORE_PASSWORD']
        r=subprocess.run(cmd,env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if r.returncode:raise SystemExit('Key generation failed. No output was logged; the destination key was not replaced.')
        r=subprocess.run([tool,'-exportcert','-keystore',str(staged),'-alias',ALIAS,'-storepass:env','TOTO_KEYSTORE_PASSWORD'],
                         env=env,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
        if r.returncode:raise SystemExit('Certificate verification failed; no destination key was replaced.')
        fingerprint=hashlib.sha256(r.stdout).hexdigest()
        staged.chmod(0o600)
        os.link(staged,KEY)  # Exclusive creation: never overwrite a signing identity.
        values={'storeFile':str(KEY),'storePassword':password,'keyAlias':ALIAS,'keyPassword':password}
        fd=os.open(PROPERTIES,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'w',encoding='ascii') as f:
            f.write(''.join(k+'='+property_value(v)+'\n' for k,v in values.items()))
    # Certificate fingerprints are public; only this metadata may go into Git.
    metadata=ROOT/'config/toto-release-certificate.json'
    if not metadata.exists():
        metadata.write_text(json.dumps({'applicationId':'vn.ToTo.music','alias':ALIAS,'certificate_sha256':fingerprint},indent=2)+'\n')
    env.pop('TOTO_KEYSTORE_PASSWORD',None);password=None
    print('Đã tạo và xác minh khóa ký. Không cài APK hoặc thay đổi app trên điện thoại.')
    print('Thư mục cần sao lưu an toàn:',PRIVATE)
    print('Certificate SHA-256 (công khai):',fingerprint)
    print('Giữ keystore, file cấu hình và mật khẩu; không gửi chúng qua chat hoặc đưa vào Git.')

if __name__=='__main__':main()

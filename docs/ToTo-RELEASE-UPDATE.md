# Phát hành và cập nhật trong app ToTo TAD Music

## Bước 1: khóa ký riêng

Chưa phát hành APK cho bạn bè, chưa bật trình cập nhật và chưa cài bản release lên điện thoại trong bước này. App trên điện thoại hiện vẫn là debug.

Mở Terminal trên Mac và chạy:

```sh
python3 '/Users/thoanhdo/Documents/Mochy-Music/scripts/prepare_release_signing.py'
```

Tự nhập mật khẩu ít nhất 12 ký tự và nhập lại. Terminal không hiện ký tự khi nhập. Không gửi mật khẩu, keystore hay file cấu hình qua chat.

Công cụ tạo khóa RSA 3072 / PKCS12 với alias toto-tad-music, kiểm tra certificate và ghi:

- `~/.config/toto-tad-music/toto-tad-music-release.p12`: khóa ký riêng.
- `~/.config/toto-tad-music/release-signing.properties`: cấu hình Gradle có mật khẩu, quyền đọc/ghi chỉ cho chủ máy (0600). Thư mục chứa khóa là 0700.
- `config/toto-release-certificate.json`: chỉ fingerprint certificate công khai, được phép lưu Git để đối chiếu các bản sau.

Hai file riêng tư nằm ngoài repo; không được đưa vào Git/GitHub Releases hoặc gửi cho người dùng app. Công cụ không in mật khẩu, không đưa mật khẩu vào đối số lệnh keytool, không ghi đè khóa/config đã có. Nếu đã có khóa hoặc metadata thì dừng để review, không tự tạo khóa mới thay thế.

Sao lưu keystore và cấu hình vào nơi riêng an toàn; lưu mật khẩu trong nơi quản lý mật khẩu của bạn. Đây là dữ liệu cần giữ cho các bản cập nhật sau. Không chỉ giữ một bản trên Mac.

Kiểm tra có file (không đọc/in mật khẩu):

```sh
python3 scripts/prepare_release_signing.py --check
```

Gradle đọc mặc định file cấu hình trên. Có thể đặt `TOTO_SIGNING_PROPERTIES` tới file riêng tương đương trên máy build/CI. Release không còn dự phòng ký bằng debug key; nếu thiếu cấu hình, lệnh assemble/bundle/package Release dừng với hướng dẫn.

## Sau bước 1

1. Chốt versionName/versionCode riêng và build bản release ký bằng khóa trên; kiểm tra certificate/APK.
2. Chuẩn bị repo GitHub riêng và GitHub Releases làm nguồn cập nhật. Chưa có link riêng thì không tự tạo đường dẫn hoặc dùng APK Mochy gốc.
3. Thêm kiểm tra bản mới, thông tin phiên bản, tải APK an toàn và mở xác nhận cài đặt của Android; giữ nguyên applicationId và khóa ký, kiểm tra versionCode và chữ ký trước khi mở installer. Không tự cập nhật âm thầm hoặc bỏ qua xác nhận hệ thống.
4. Thử cài đè release v1 → release v2 trên cùng khóa và kiểm tra dữ liệu/phiên. Chỉ sau khi qua mới phát cho bạn bè.

Khóa release mới khác khóa debug đang dùng. Chưa có phương án chuyển debug → release được thử trên máy này; không gỡ app, clear dữ liệu hay hứa cài đè trực tiếp giữa hai khóa khác nhau. Giữ nguyên bản debug của chủ app trong lúc chuẩn bị. Điều này không ảnh hưởng khả năng cài đè các bản release sau nếu ngay từ bản đầu chia sẻ dùng cùng khóa.

Theo dõi tiến độ/thử nghiệm tại ToTo-TEST-RESULTS.md. Giữ GPL-3.0 và ghi công dự án gốc như phạm vi đã thống nhất.

## Nguồn phát hành cá nhân

Repo do chủ app cung cấp: https://github.com/tototadofficial-21ck/ToTo-TAD-Music (public).
Remote `personal` trỏ repo này; `origin` vẫn là upstream Mochy, tuyệt đối không push bản cá nhân vào origin.

Phiên bản riêng đầu tiên: `config/toto-version.properties`, versionCode 1000, versionName 1.0.0. Mỗi lần phát hành tăng VERSION_CODE; không tái dùng số đã phát hành.

Trong Hồ sơ → Giới thiệu ứng dụng → Kiểm tra cập nhật:

- Chỉ đọc GitHub Releases của repo cá nhân, không cần GitHub token trong app.
- Chỉ dùng latest stable release (không draft/prerelease). Tag theo **v<versionCode>**, ví dụ `v1000`, `v1001`; tên release có thể là `ToTo TAD Music 1.0.1`.
- APK đính kèm phải có tên chính xác **ToTo-TAD-Music.apk**. GitHub API phải trả digest `sha256:` cho asset; thiếu digest sẽ từ chối tải/cài. Không lấy asset ở repo khác.
- Giới hạn APK 512MiB; kiểm tra kích thước và SHA256, applicationId, versionCode cao hơn bản hiện tại, certificate khớp khóa release đã chốt và bản đang cài. Chỉ theo redirect HTTPS đến các host GitHub được cho phép. Không log nội dung mạng.
- FOSS xin quyền “Cài ứng dụng không rõ nguồn gốc” khi cần. Sau khi cấp quyền, quay lại bấm Cài đặt; Android vẫn hỏi xác nhận cài đặt. Nếu hủy có thể bấm lại. F-Droid tiếp tục loại permission này.
- Bản debug hoặc bản ký khác không mời cài release. Không gỡ app hay xóa dữ liệu để vượt lỗi chữ ký.
- Hiện kiểm tra thủ công trong Giới thiệu; chưa tự thông báo khi mở app. Khi tải cần giữ màn này mở; lỗi mạng có thể thử lại từ đầu, chưa hỗ trợ resume.

Chưa push mã hoặc tạo GitHub Release. Trước khi phát hành: review file được Git theo dõi, loại keystore/local.properties, cấu hình CI upstream và thông tin riêng không dành công khai; build/kiểm tra release mới có updater; thử release→release trên thiết bị hoặc emulator riêng, giữ bản debug trên điện thoại chủ app.

Khi được chủ app cho phép công khai, push source tương ứng đúng tag cùng LICENSE GPL và thay đổi; upload APK đã xác minh. Không upload khóa ký, cấu hình chứa mật khẩu hoặc log. Giữ bản mã nguồn tương ứng mỗi APK để tuân thủ GPL. Tải APK đầu tiên một lần; các bản release cùng khóa sau đó cài đè qua updater, không yêu cầu gỡ app.

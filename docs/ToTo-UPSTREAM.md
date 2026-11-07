# Giữ bản cá nhân khi nhận cập nhật Mochy

## Nguyên tắc

Bản cá nhân làm việc trên nhánh `toto/personal`; mốc ổn định được lưu bằng tag `toto-stable-20261107`. Repo gốc vẫn là remote `origin` (https://github.com/realidkroo/Mochy-Music.git). Chưa có repo cá nhân nên không đổi origin và không push lên repo gốc.

Cập nhật bằng cách review/chọn commit phù hợp, không ghi đè thư mục hay chấp nhận toàn bộ file upstream khi xung đột. Git không thể tự bảo đảm mọi thay đổi vẫn đúng: guard phát hiện một số mất mát rõ ràng, còn hành vi phải kiểm tra mã và thử trên máy. Không tự tải/cài APK gốc để cập nhật bản cá nhân.

## Xem cập nhật (không áp dụng)

```sh
python3 scripts/review_upstream.py
```

Công cụ kiểm tra URL remote, fetch main rồi so từ mốc trong `config/toto-upstream.json`, ghi `docs/verification/upstream-review.md`: commit mới, file chạm vùng bảo vệ và file cả hai phía cùng sửa. Không merge, reset, cài APK, push hay cập nhật mốc tự động. Có thể dùng `--no-fetch` để kiểm tra từ remote ref đã lưu. Mốc review không phải phiên bản APK; không đổi chỉ để làm danh sách commit biến mất.

## Áp dụng một đợt đã review

1. Làm sạch working tree bằng commit các thay đổi đã review; kiểm tra không đưa local.properties, khóa ký, token, log riêng tư vào Git.
2. Từ `toto/personal`, tạo nhánh thử riêng: `git switch -c toto/update-YYYYMMDD`.
3. Đọc từng commit bằng `git show <sha>`. Chỉ chọn sửa lỗi/tính năng cần thiết. Dùng `git cherry-pick <sha>` đối với commit độc lập đã hiểu rõ; commit merge hoặc phụ thuộc nhiều cần port từng phần trong nhánh thử. Không tự cherry-pick tất cả commit trong báo cáo.
4. Xung đột phải kết hợp thủ công: giữ namespace Kotlin, applicationId vn.ToTo.music, tên/logo/QR/link cá nhân, Auth gate và cache Keystore, profiles/Storage/RLS, ngôn ngữ Việt/Anh, loại trừ phiên khỏi backup, chặn installer/updater APK gốc. Không dùng lựa chọn ours/theirs cho cả file để xử lý nhanh. Nếu không muốn tiếp tục một cherry-pick đang xung đột, dùng `git cherry-pick --abort`.
5. Kiểm tra:

```sh
python3 scripts/check_personal_contract.py
python3 scripts/check_personal_resources.py
bash scripts/test_personal_rls_local.sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:assembleUniversalFossDebug :app:testUniversalFossDebugUnitTest --console=plain
git diff --check
```

6. Cài đè đúng điện thoại bằng adb -s; nếu khác chữ ký thì dừng, không gỡ app. Thử đăng nhập/logout, tài khoản chưa có quyền/bị khóa, mở lạnh/foreground/offline, deep link/widget/controller; hồ sơ/ảnh theo UUID, tiếng Việt/Anh, online/local/background/pause và phần code cập nhật. Không tự khóa user thật hay chạy migration lên project.
7. Ghi kết quả thật/chưa thử vào ToTo-TEST-RESULTS.md. Khi đợt thử ổn mới đưa commit từ nhánh thử về nhánh cá nhân (ff-only nếu phù hợp), tạo tag ổn định mới. Nếu chỉ nhận vài commit, giữ mốc review và ghi danh sách commit đã nhận; chỉ nâng mốc khi đã rà toàn bộ khoảng thay đổi và ghi rõ những phần cố ý không nhận.

## Sao lưu và quay về

Mốc Git chỉ lưu mã/tài nguyên được theo dõi, không lưu local.properties, SDK, APK build hoặc dữ liệu điện thoại. Giữ local.properties và khóa ký ở nơi riêng an toàn; không in vào báo cáo.

Xem bản đã lưu bằng nhánh mới, không reset cưỡng bức working tree:

```sh
git switch -c toto/recovery-YYYYMMDD toto-stable-20261107
```

Muốn quay APK về bản cũ cần build từ mốc phù hợp và giữ cùng khóa ký; có thể bị chặn version downgrade/schema. Không dùng adb uninstall/clear để vượt qua. Chưa thử quy trình hạ phiên bản có dữ liệu trên máy thật.

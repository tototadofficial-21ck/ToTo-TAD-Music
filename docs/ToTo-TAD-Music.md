# Bàn giao bản Android cá nhân ToTo TAD Music

## Phạm vi và điểm tích hợp đã rà soát

- Gradle: `app/build.gradle.kts`, minSdk 24, target/compileSdk 36, Java 21; ABI universal/arm64/armeabi/x86/x86_64 và biến thể foss/fdroid/gms. Giữ namespace Kotlin `com.mochyy.mochymusic`; application ID cá nhân `vn.ToTo.music`, kể cả debug. Không thể cài đồng thời debug và release của bản cá nhân này. Android tạo vùng dữ liệu mới cho ID mới; dùng export/transfer của app gốc rồi import sau khi qua gate nếu muốn chuyển thư viện. Bản gốc có ID khác nên vẫn cài song song được.
- `MainActivity.kt`: Compose, NavHost, cold start, `onNewIntent`, deep link YouTube/nhạc, share intent và launcher aliases. Bọc navigation bằng `auth/AuthGate.kt`. Intent khi chưa có phiên được cấp quyền được giữ chờ; phiên đã xác minh được khôi phục để mở navigation ngay và kiểm tra lại ở nền.
- `MusicService.kt`, `MediaLibrarySessionCallback.kt`, `TransportTransitions.kt`, `audio/pass/PassRouterSink.kt`: service/binder, MediaSession, notification, media controller, đầu ra âm thanh, phát tiếp/hàng đợi và widget. Các lệnh mới bị từ chối khi chưa xác minh hoặc đã thu hồi quyền. Khi đăng xuất/khóa, hủy chuyển âm, dừng player và Cast, bỏ thông báo foreground. Không xóa thư viện hoặc các đăng nhập nguồn nhạc.
- `AndroidManifest.xml`: mọi launcher alias vẫn trỏ MainActivity; cùng tên/logo. Sửa cả overlay tên debug, shortcuts debug/main và manifest debug từng thêm lại quyền cài APK. Adaptive/round/legacy icon và splash dùng ảnh người dùng cung cấp. Dùng splash Android chuẩn; tắt hoạt cảnh mở đầu vẽ logo Mochy bằng đường nét. Logo Trang chủ dùng ảnh mới. Không thay logo/thông tin tác giả ở phần ghi công.
- Updater: có cả `GithubUpdates.kt` và updater cũ `mochyupdater.kt`, worker tải APK và `CustomDownloadManager`. Đã chặn các đường kiểm tra/tải/cài APK upstream, bỏ `REQUEST_INSTALL_PACKAGES`. Bản cá nhân chưa có feed riêng, không tự cập nhật. Các link mã nguồn, GPL và ghi công dự án gốc vẫn giữ nguyên.
- Support/Donate và nút Donate trên hồ sơ nhà phát triển dẫn tới QR cá nhân. QR là ảnh gốc bạn gửi, không dựng lại hay thay dữ liệu. Chủ app hiển thị ToTo TAD Music; thông tin tác giả gốc vẫn ghi riêng.
- Locale: tái sử dụng `i18n/AppLanguage.kt` (LocaleManager Android 13+, DataStore trên Android cũ). Tiếng Việt/English trên gate dùng cùng cơ chế, lưu trước khi recreate. Gom các mục chỉ có ở `values-vi-rVN` vào `values-vi`, ưu tiên bản `vi` và xóa override vùng cũ. Hoàn thiện các bản dịch còn để nguyên tiếng Anh; giữ tên dịch vụ và đơn vị kỹ thuật. `UiText.kt` dùng application resources đã được cơ chế locale hiện có cập nhật để dịch callback ngoài Compose.
- Không sửa thuật toán tìm kiếm/chọn nguồn, lời bài hát, download nhạc, import, playlist, Room/SQLite, backup/restore hoặc đăng nhập các dịch vụ nhạc. Thay đổi ở playback chỉ phục vụ ranh giới quyền truy cập. Gỡ hai log cũ in giá trị cookie YouTube.

## Cấu hình build

1. Mở Project Settings / API trong Supabase Dashboard, lấy **Project URL** và **Publishable key** (`sb_publishable_…`).
2. Chép hai dòng trong `supabase.properties.example` vào `local.properties` đang được git bỏ qua. Không ghi đè dòng `sdk.dir` nếu đã có. Hoặc đặt hai biến môi trường có cùng tên khi chạy Gradle.
3. Để trống sẽ build được nhưng gate không cho vào app. Không có tài khoản khách, đăng ký Supabase công khai, mật khẩu chung hoặc bypass debug/release.
4. Không dùng `sb_secret_…`, secret key hay service_role trong `local.properties`/APK. Gradle từ chối khóa không có tiền tố Publishable.
5. Máy cần Java 21, Android SDK 36, build tools, CMake 3.22.1 và NDK 27.0.12077973. Ví dụ trên máy này:

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleUniversalFossDebug
./gradlew :app:testUniversalFossDebugUnitTest
python3 scripts/check_personal_resources.py
```

APK ở `app/build/outputs/apk/universalFoss/debug/` (tên thực tế xem phần kết quả kiểm tra). Cài bằng `adb install -r <đường-dẫn-apk>` hoặc chép APK sang Android và mở bằng trình quản lý tệp. Cần Android 7.0 trở lên. Dùng cùng signing key khi build lại để cài đè mà giữ dữ liệu; không chia sẻ keystore release.

## Áp dụng Supabase và tạo tài khoản

Không có migration nào được tự chạy lên project của bạn.

1. Dashboard → Authentication → Sign In / Providers (hoặc phần cấu hình Auth tương ứng): giữ Email/password; tắt **Allow new users to sign up**, anonymous sign-ins và các provider không dùng. Không tắt hẳn Email provider. Việc không có nút đăng ký trong app không thay thế cấu hình server này.
2. SQL Editor: review rồi chạy `supabase/migrations/202611060001_personal_access.sql` đúng một lần. Migration dành cho schema mới; nếu đã có bảng trùng tên, cần review và điều chỉnh trước khi chạy.
3. Authentication → Users → Add user / Create user: tạo email và mật khẩu riêng, xác nhận email bằng thao tác admin phù hợp. Không dùng Invite qua email nếu bạn chưa có trang đặt mật khẩu/redirect riêng.
4. Trigger tạo `profiles` và `app_access` với `enabled=false`. Tài khoản mới vẫn bị chặn dù mật khẩu đúng. Email và thời điểm đăng nhập gần nhất nằm trong Auth (`last_sign_in_at`), không nhân bản email vào `profiles`.
5. Table Editor → `profiles`: nhập `display_name` nếu cần. App hiện không cho người dùng sửa hồ sơ.
6. Table Editor → `app_access`: tìm đúng UUID của Auth user rồi đặt `enabled=true` để cấp quyền.
7. Khóa: đặt `enabled=false`. Có thể ban user thêm trong Authentication để chặn cả Auth login/refresh. Mở lại: bỏ ban nếu có và đặt lại `enabled=true`. Chỉ mở lại một trong hai nơi có thể vẫn bị chặn.

Hoặc dùng SQL Editor với UUID thật lấy từ Dashboard:

```sql
update public.app_access set enabled = true where user_id = '<UUID-thật>'::uuid;
-- Khóa: thay true bằng false. Không sửa RLS/GRANT để cấp quyền từng người.
update public.profiles set display_name = '<tên hiển thị>' where user_id = '<UUID-thật>'::uuid;
```

Đặt lại mật khẩu: trong trang Auth user, dùng **Send password recovery** nếu bạn đã cấu hình một trang recovery/redirect đáng tin cậy của riêng mình. App này không nhận recovery link và không có trang recovery công khai. Nếu chưa có trang đó, dùng công cụ admin cục bộ `python3 scripts/reset_supabase_password.py`: nhập URL, UUID từ Dashboard, khóa admin và mật khẩu mới qua lời nhắc ẩn. Công cụ gọi Admin API, không lưu/in khóa hoặc mật khẩu; không được chép khóa admin vào cấu hình build. Không có dashboard admin trong app. Sau đổi mật khẩu, yêu cầu người dùng đăng xuất rồi đăng nhập lại; dùng `enabled=false` nếu cần chặn ngay ở lần xác minh kế tiếp.

Tài liệu gốc: [cấu hình đăng ký](https://supabase.com/docs/guides/auth/general-configuration), [Admin update user/password/ban](https://supabase.com/docs/reference/javascript/auth-admin-updateuserbyid), [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security), [Auth API](https://supabase.github.io/auth/).

## Quyền dữ liệu và bảo vệ phiên

- `profiles.user_id` và `app_access.user_id` là khóa chính/FK tới `auth.users.id`, xóa theo Auth user. `profiles` chỉ có tên hiển thị và thời điểm tạo.
- RLS bật trên cả hai bảng. `authenticated` chỉ có SELECT: chỉ đọc quyền của chính mình; chỉ đọc hồ sơ của mình khi đang được cấp quyền. Không có INSERT/UPDATE/DELETE từ client, kể cả đổi `display_name`. `anon` không có quyền đọc/ghi. Trigger chạy security definer với search_path rỗng và không được client gọi.
- Quyền admin nằm ở Dashboard/backend, không lấy từ user metadata tự sửa được. Chỉ secret/service_role ở máy admin có quyền cập nhật; APK chỉ có URL và Publishable key.
- `supabase/verify_rls.sql` kiểm tra RLS **và GRANT** theo hai UUID khác nhau, thử đọc chéo, sửa quyền và sửa hồ sơ người khác, rồi kiểm tra người bị khóa và role anon. Chỉ chạy trên project thử nghiệm, thay hai UUID bằng hai Auth user thật trước; toàn bộ thay đổi được rollback. Script đã qua PostgreSQL cục bộ tách biệt với schema Auth giả lập và hai UUID thử nghiệm; chưa chạy với Supabase Auth thật hoặc project của bạn. Có thể chạy lại bằng `bash scripts/test_personal_rls_local.sh` nếu máy có PostgreSQL 16.
- Phiên lưu refresh token cùng biên nhận cấp quyền (UUID và project) đã xác minh, mã hóa AES-256-GCM bằng khóa trong Android Keystore; access token chỉ ở RAM. Biên nhận chỉ được ghi sau khi server xác nhận quyền và tải đúng hồ sơ, không lấy quyền từ preference thuần văn bản. Mỗi lần xác minh lấy token mới, gọi Auth `/user` rồi kiểm tra bảng quyền của UUID đó. Không tin UUID/quyền tự khai báo từ client.
- Tệp phiên là `noBackupFilesDir/toto-auth.enc`, ghi atomic. Vùng này không thuộc Android cloud backup/device transfer; thêm exclude phòng vệ trong cả hai XML backup. Backup cũ chỉ lấy settings/database; transfer dùng danh sách file/prefs cho phép, không lấy `noBackupFilesDir`. Khóa Keystore cũng không được xuất.
- Không có logger mạng cho Supabase, không ghi mật khẩu/token/response vào log. Xóa token cục bộ và chặn lệnh phát ngay khi đăng xuất; gửi logout tới Supabase theo kiểu cố gắng tối đa khi có mạng. Không thể cam kết thu hồi server khi thiết bị đang mất mạng.
- Không gửi playlist, lịch sử, tệp nhạc, bản tải hay cookie nguồn nhạc tới Supabase. Các đồng bộ nguồn nhạc sẵn có của dự án vẫn giữ hành vi gốc.

## Chính sách mạng và thời điểm khóa có hiệu lực

- Đăng nhập lần đầu vẫn xác minh trước khi vào app. Mở lạnh khôi phục phiên và biên nhận hợp lệ bằng Keystore, đối chiếu chủ hồ sơ cache rồi vào ngay; cold start và MainActivity STARTED kiểm tra lại ở nền. Gộp các lần kiểm tra trong 60 giây của cùng tiến trình; nút Thử lại bỏ qua khoảng chờ. Phiên cũ trước thay đổi này được xác minh một lần để nâng định dạng; chưa có biên nhận, đã đăng xuất hoặc không giải mã được thì không vào app từ cache.
- Khi đã vào app và còn ở phiên được xác minh, cho phép nghe offline/phát nền kể cả access token tới hạn trong lúc nghe. Không có polling Supabase liên tục khi ở nền.
- Vì vậy khóa trên Dashboard **không dừng ngay lập tức** bài đang phát nền hoặc app đang mở liên tục. Khóa có hiệu lực khi lần xác minh kế tiếp hoàn tất. Nếu mở lại/foreground không thể xác minh do mất mạng hoặc lỗi server/cấu hình, phiên đã được cấp quyền vẫn hoạt động, hiện popup lỗi có Thử lại/Tiếp tục nghe nhạc. Nếu server xác nhận khóa, thu hồi quyền hoặc refresh session không hợp lệ, xóa biên nhận/phiên, dừng phát và quay về đăng nhập với thông báo. Nếu thiết bị tiếp tục mất mạng, quyền đã xác minh có thể tiếp tục dùng cho đến lần kết nối/xác minh kế tiếp; không có thời hạn cưỡng bức offline mới. Khi đang ở app đã xác minh, mất mạng đơn thuần không khóa nhạc trên máy/bản tải.
- Khi xác nhận bị khóa/chưa cấp quyền hoặc Auth từ chối phiên: xóa phiên, dừng player/chuyển âm/Cast, từ chối điều khiển qua widget/notification/media controller, hiện gate. Không xóa thư viện. Sau mở lại quyền, đăng nhập lại. Với Cast, lệnh dừng/ngắt là cố gắng tối đa; thiết bị nhận đã mất kết nối có thể tiếp tục phát độc lập.

## Thông tin cần bạn bổ sung

- `SUPABASE_URL` và `SUPABASE_PUBLISHABLE_KEY`: đã điền cấu hình cục bộ theo thông tin bạn gửi ngày 07/11/2026. Mẫu cấu hình vẫn để trống; giá trị thật chỉ nằm trong `local.properties` được Git bỏ qua. Auth health trả HTTP 200; API chưa tìm thấy bảng `app_access` (PGRST205), cần áp dụng migration bằng Dashboard trước khi sử dụng.
- Liên hệ chủ app: thay `toto_contact` trong `values/toto_strings.xml` và `values-vi/toto_strings.xml`. Hiện là thông báo chưa cấu hình, không tự tạo email/link/số tài khoản.
- Nguồn APK cá nhân: chưa có nên updater bị vô hiệu hóa. Không đổi sang một URL phát hành đoán trước.

## Kiểm thử nghiệm thu trên thiết bị

Đã tạo emulator tạm Android 36.1 để thử gate; chưa có cấu hình/tài khoản Supabase thật trong phiên làm việc này. Các ca sau cần chạy sau khi bạn cấu hình; không xem kiểm tra mã nguồn là kết quả runtime:

| Ca | Kết quả cần có |
| --- | --- |
| Đúng mật khẩu + enabled=true | Qua gate, thư viện và navigation hoạt động |
| Sai mật khẩu/refresh hết hạn | Báo lỗi, không vào navigation |
| Đúng mật khẩu nhưng chưa cấp/đã khóa | Chặn; không phát được từ widget/controller |
| Mở lạnh hoặc quay lại app khi mất mạng | Có biên nhận Keystore và đúng chủ cache: vào ngay, popup lỗi/Thử lại, vẫn nghe offline; không có phiên đã cấp quyền: chặn |
| Mất mạng sau khi đã xác minh | Phát được file trên máy/bản tải trong phiên đó |
| Khóa lúc đang phát nền | Tiếp tục theo chính sách tới lần kiểm tra foreground; sau đó dừng |
| Đăng xuất khi phát/chuyển âm/Cast | Dừng, không thể phát lại bằng thông báo/widget; dữ liệu thư viện còn |
| Deep link/share/launcher alias khi chưa đăng nhập | Chỉ gate; intent chờ sau xác minh |
| Hai tài khoản A/B | Không đọc/sửa hồ sơ/quyền của nhau; anon bị từ chối |
| Tìm bài và phát nguồn trực tuyến | Kiểm tra nguồn hiện hoạt động; không đổi thứ tự nguồn |
| Nhạc trên máy, tải, lời, playlist, import, backup/restore | Hành vi gốc, không mang phiên Supabase sang máy khác |
| vi, vi-VN và en, màn hình nhỏ, bàn phím mở | Nội dung đúng ngôn ngữ, gate/support cuộn được, không cắt chữ |
| App icon/round/adaptive/alias/splash | Đúng tên và logo ToTo TAD Music |

Kết quả build và kiểm tra tự động thực tế được ghi tại `docs/ToTo-TEST-RESULTS.md`.

Danh sách đầy đủ các tệp thay đổi: `docs/verification/changed-files.txt`.


## Hồ sơ gắn tài khoản (bổ sung 07/11/2026)

Thay đổi mới cho phép lưu tên và ảnh hồ sơ theo UUID Supabase. Áp dụng migration thứ hai trước khi cài APK mới; xem hướng dẫn từng bước tại [ToTo-SUPABASE-PROFILE.md](ToTo-SUPABASE-PROFILE.md). Các mô tả cũ về hồ sơ chỉ lưu trên máy/quyền profiles chỉ đọc được thay thế bởi bổ sung này; dữ liệu nghe nhạc vẫn cục bộ.


## Cấu hình trang chủ app và liên kết cá nhân

- Tên app/logo/QR đã dùng thông tin người dùng cung cấp. Trang Chủ app dùng logo ToTo TAD Music; ghi công tác giả Mochy được giữ trong mục Tác giả và người đóng góp dự án gốc. Các liên kết/tên/avatar tác giả ở đó mô tả dự án gốc, không phải chủ bản cá nhân.
- Các URL cá nhân đặt tập trung tại `app/src/main/res/values/toto_owner_links.xml`: website, trang cá nhân, cộng đồng hỗ trợ, liên hệ và repo bản cá nhân. Đã cấu hình Facebook, Instagram và Discord do chủ app cung cấp; website, email và repo cá nhân chưa có nên để trống, app không hiện nút link trống. Có thể dùng URL `mailto:` cho địa chỉ liên hệ sau khi chủ app cung cấp. XML URL có dấu & cần viết &amp;.
- Nhãn và mô tả: `values/toto_branding.xml` cùng `values-vi/toto_branding.xml`. Chưa có tiểu sử riêng nên mô tả hiện tại chỉ nêu đúng đây là bản cá nhân dựa trên Mochy Music.
- Màn Biểu tượng của bản cá nhân chỉ trình bày logo thực tế; không còn các mẫu Mochy/anime cũ vốn không khớp biểu tượng launcher đã thay. Các alias và dữ liệu lựa chọn cũ vẫn giữ tương thích.
- Không đổi tên dịch vụ Apple Music/Monochrome/JioSaavn hay nội dung ghi công/GPL thành tên chủ app.

# Kết quả kiểm tra — 06–07/11/2026

## Đã chạy

- Build `:app:assembleUniversalFossDebug`: thành công. APK universal debug, application ID **vn.ToTo.music**, tên **ToTo TAD Music** cho cả tiếng Anh và Việt, minSdk 24 / targetSdk 36.
- `:app:testUniversalFossDebugUnitTest`: **136 test, 0 lỗi, 0 thất bại, 0 bỏ qua**. Sửa lời gọi API cũ trong `VideoDashTest` để khớp chữ ký hiện tại; không đổi mã phát video. Đồng bộ danh sách chọn locale với các thư mục tài nguyên hiện có; giữ nguyên các ngôn ngữ đã hỗ trợ.
- `python3 scripts/check_personal_resources.py`: qua; 2.091 tài nguyên mặc định, 2.090 tài nguyên tiếng Việt (mảng mã locale không dịch), không trùng tên, không thiếu string có thể dịch, không sai placeholder/plurals. Đã gộp và loại override `vi-rVN` cũ.
- `bash scripts/test_personal_rls_local.sh`: qua trên PostgreSQL 16 cục bộ tách biệt, với `auth.users`/`auth.uid()` giả lập và hai UUID thử nghiệm. Kiểm tra backfill tài khoản cũ, trigger tài khoản mới mặc định chưa cấp quyền, đọc đúng hồ sơ/quyền của mình, chặn đọc chéo, chặn ghi qua GRANT, hồ sơ của tài khoản bị khóa không đọc được, anon không có quyền và FK cascade. Không chạy migration hay test lên Supabase của bạn.
- `apksigner verify`: APK có chữ ký debug v2 hợp lệ. Kiểm tra manifest APK: tên/ID đúng và không có `REQUEST_INSTALL_PACKAGES`, kể cả manifest debug.
- Kiểm tra mã manifest: **50 launcher alias** dùng cùng tên và launcher icon cá nhân, cùng đích MainActivity. Sửa shortcuts main/debug sang ID cá nhân. Adaptive icon có vùng đệm cho mặt nạ tròn; QR là ảnh gốc người dùng gửi.
- Emulator tạm **Android 36.1, arm64, 720 × 1280 px, 320 dpi (360 × 640 dp)**: cài APK, mở mặc định không crash; gate hiện trước navigation. Chuyển English → Tiếng Việt; tiếng Việt được giữ sau force-stop/mở lại. Thử nhập thông tin QA giả khi URL/key trống: báo chưa cấu hình và không qua gate. Không có tài khoản/token thật được sử dụng.
- Emulator: mở lạnh qua deep link YouTube; gửi share intent khi app đang mở; broadcast nút phát của cả hai widget; gửi media play. Gate vẫn hiện, MediaSession ở `NONE(0)`, metadata null, hàng đợi rỗng; không phát nhạc. Đây là kiểm tra **trước đăng nhập**, chưa phải ca đăng xuất lúc đang phát.
- Emulator: kiểm tra gate tiếng Việt ở cỡ chữ **130%**, không cắt nội dung. Kiểm tra mở lạnh không có mạng và không có phiên: vẫn bị giữ ở gate. Không suy diễn kết quả này thành kiểm tra offline với phiên đã xác minh.
- `git diff --check`: qua. Có cảnh báo build sẵn có về DSL/API cũ, metadata Room và tài nguyên bedside cũ; build vẫn thành công. Không sửa schema Room hoặc thuật toán nguồn nhạc.

Ảnh kiểm tra nằm trong `docs/verification/`: gate English, gate tiếng Việt và tiếng Việt cỡ chữ lớn. Không chứa thông tin đăng nhập thật.

## Chưa chạy / chỉ rà mã

- Đăng nhập Supabase thật; sai mật khẩu thật; chưa cấp quyền/bị khóa; refresh token hết hạn/thu hồi; mất mạng khi có phiên đã xác minh; khóa khi đang phát nền; đăng xuất khi đang phát/Crossmix/Cast. Hai cấu hình Supabase hiện để trống nên không vượt gate để thử các luồng này.
- RLS/GRANT với JWT từ hai tài khoản Supabase Auth thật. Test PostgreSQL cục bộ không thay thế bài kiểm tra tích hợp này; có script và hướng dẫn để chạy trên project thử nghiệm.
- Tìm kiếm/phát từ nguồn ngoài, phát file trên máy, tải nhạc, lời, playlist, import, backup/restore và phát nền sau đăng nhập. Không xác nhận nguồn ngoài đang hoạt động hay đang lỗi vì chưa thể qua gate hợp lệ.
- Home/Tìm kiếm/Thư viện/trình phát/Cài đặt/Support sau đăng nhập: đã sửa resources và rà mã, chưa kiểm tra hình ảnh hoặc âm thanh thực tế. Đặc biệt cần xem QR và bố cục các màn này trên thiết bị sau cấu hình.
- Alias thay thế: đã rà cả 50 alias trong mã; chỉ alias mặc định được khởi chạy trên emulator. Android 36.1 từ chối lệnh shell bật alias vốn bị tắt, nên chưa thử đổi icon qua màn tùy chỉnh (màn này ở sau gate).
- Ở đợt emulator ban đầu chưa chạy thiết bị thật. Phiên USB mới được ghi riêng bên dưới; Android 7/minSdk, build release, GMS/F-Droid và Cast thực vẫn chưa thử.

## APK và cấu hình còn thiếu

APK: `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`.

Cập nhật ngày 07/11/2026: đã thêm Project URL và Publishable key được cung cấp vào `local.properties` (Git bỏ qua) và build lại APK. Kiểm tra chỉ đọc: Auth health HTTP 200; REST `app_access` trả HTTP 404 / PGRST205 (chưa có bảng trong schema cache). Chưa chạy migration lên project; bạn cần áp dụng SQL rồi tạo/cấp quyền tài khoản theo `docs/ToTo-TAD-Music.md`. Các thử nghiệm emulator với cấu hình trống phía trên là kết quả của bản trước lần cấu hình này; chưa thử đăng nhập tài khoản thật. Liên hệ chủ app còn là placeholder `toto_contact` trong hai bản ngôn ngữ. Nguồn phát hành riêng chưa có; cả updater cũ/mới và tải/cài APK upstream đều bị vô hiệu hóa.

Khi đã xác minh, app cho nghe offline/nền. Khóa có hiệu lực ở lần xác minh mở lạnh/foreground kế tiếp; không có kiểm tra nền liên tục. Nếu không xác minh được khi trở lại foreground, app dừng phát và đóng gate. Dừng/ngắt Cast được gửi theo khả năng kết nối; thiết bị nhận ở mạng khác hoặc đã mất kết nối có thể không nhận được lệnh dừng. Không xóa thư viện khi đăng xuất.

SHA-256 APK cuối: `ff485bb201032a82196c2e3b5e1cef822a352d6606272d72073fb17620fd7ff9`. Dung lượng: 89306022 byte.


## Phiên kiểm thử USB trên điện thoại thật — 07/11/2026

- Thiết bị USB: Samsung SM-S948B, Android 17 (API 37), serial `R5GL13NRSBZ`; adb trạng thái `device`, đã được cấp quyền gỡ lỗi. Mọi lệnh thiết bị dùng serial cụ thể này.
- Xác nhận hai giá trị Supabase có trong `local.properties`, không in giá trị.
- `:app:assembleUniversalFossDebug`: thành công (6 giây); cài bằng `adb -s R5GL13NRSBZ install -r ...`: Success. Không gỡ app, không xóa dữ liệu, không có lỗi khác chữ ký.
- Mở alias mặc định: cold start thành công, Android hiện hộp thoại quyền thông báo. Chờ người dùng tự xử lý quyền và nhập thông tin đăng nhập.
- Kiểm tra kết nối chỉ đọc: Auth health HTTP 200; REST app_access với Publishable key và không có phiên người dùng trả HTTP 401 / 42501. Không còn phản hồi thiếu bảng PGRST205 ở lần kiểm tra này; chưa xác nhận quyền đọc của tài khoản đăng nhập.
- Chưa lấy logcat hay ghi hình trong lúc người dùng nhập mật khẩu. Chưa xác định lỗi runtime để sửa.

Thứ tự đang chờ người dùng thử và xác nhận:
1. Đăng nhập bằng tài khoản đã cấp quyền — đang chờ kết quả.
2. Tìm và phát một bài online — chưa thử.
3. Khóa màn hình, xác nhận nhạc vẫn phát — chưa thử.
4. Phát một tệp nhạc có sẵn trên điện thoại — chưa thử.
5. Xem giao diện tiếng Việt và mục Ủng hộ/QR — chưa thử.
6. Đăng xuất trong khi đang phát, kiểm tra nhạc dừng và không phát lại bằng thông báo/widget — chưa thử.

Nếu có lỗi: lấy log quanh lần tái hiện, chỉ tiến trình app hoặc crash, loại nội dung xác thực trước khi lưu; xác định nguyên nhân rồi mới sửa nhỏ, build và cài đè để thử lại đúng ca. Không bỏ gate, không đổi nguồn hoặc luồng phát.


### Tái hiện lỗi đăng nhập và cải thiện giao diện trên máy USB

- Người dùng báo thông báo "tài khoản bị khóa hoặc chưa được cấp quyền". Kiểm tra log tiến trình app bằng bộ lọc chỉ trích dấu crash/tên lớp ngoại lệ: không thấy crash trong cửa sổ kiểm tra; không lưu nội dung log thô hoặc thông tin xác thực.
- Người dùng tự chạy truy vấn chỉ đọc trong Dashboard: `da_cap_quyen=false`, `dang_bi_khoa=false`. Nguyên nhân đã xác định: tài khoản Auth tồn tại nhưng chưa được bật quyền app, đúng thiết kế mặc định từ chối; không phải lỗi mật khẩu và không bỏ gate.
- Hướng dẫn người dùng tự cấp quyền cho đúng email bằng upsert có RETURNING. Người dùng xác nhận kết quả true và đã vào được app. Không có thao tác ghi Supabase nào do agent tự thực hiện.
- Đổi riêng màn gate: logo ToTo, nền tối và màu nhấn ấm, form gọn trong thẻ bo góc, nút đăng nhập toàn chiều ngang, IME Next/Done, hiện/ẩn mật khẩu, ngôn ngữ phía trên. Không còn hiện nút đăng xuất và thử lại không cần thiết ở trạng thái đăng nhập ban đầu. Giao diện tự cuộn theo bàn phím/insets và cỡ chữ.
- Tách thông báo tài khoản chưa được bật quyền, Auth ban, email chưa xác nhận, cấu hình database/key và lỗi mạng. Thêm 3 unit test để bảo đảm lỗi quyền/database không bị báo thành sai mật khẩu. Không thay điều kiện cấp quyền, lưu phiên, nguồn hoặc luồng phát nhạc.
- Thêm log chẩn đoán debug chỉ gồm tên bước, kết quả thuộc danh sách cố định và HTTP status; không log URL có UUID, email, mật khẩu, response, token, cookie hoặc nội dung exception.
- Build debug và 139 unit test thành công (0 thất bại). Cài đè Success trên cùng SM-S948B và mở alias mặc định thành công; không gỡ hoặc xóa dữ liệu.
- Đăng nhập tài khoản được cấp quyền: **qua theo xác nhận người dùng**. Kiểm tra tìm/phát online: đang chờ người dùng. Phát nền khi khóa, tệp trên máy, UI tiếng Việt/Ủng hộ và đăng xuất lúc phát vẫn chưa hoàn tất.
- Chưa chụp màn hình máy thật khi người dùng nhập thông tin xác thực. Cần đánh giá bố cục gate mới trên máy thật khi người dùng đăng xuất ở cuối quy trình.

Chẩn đoán an toàn sau cài đè: `stage=access outcome=GRANTED`, `stage=access outcome=GRANTED`.


### Rà giao diện tiếng Việt trên máy thật theo yêu cầu người dùng

- Tạm hoãn các ca âm thanh để ưu tiên kiểm tra từng màn hình. Người dùng chưa xác nhận bài online đã phát được.
- Bằng chứng trên máy: màn chào mừng còn "Welcome to mochy!", mô tả tiếng Anh và nút "Next"; đây là chuỗi viết cứng trong `WelcomeStep.kt` và `SetupWizardScreen.kt`, không phải do thiếu tài nguyên `values-vi`. Máy dùng vi-VN; sau đó đặt locale riêng của app thành vi bằng LocaleManager qua adb.
- Đã chuyển nội dung luồng thiết lập (chào mừng, nhập/khôi phục, chọn dịch vụ, đăng nhập dịch vụ, hồ sơ, chọn nghệ sĩ, hiệu năng và hoàn tất) sang resources Anh/Việt. Đổi tiêu đề chào mừng sang thương hiệu ToTo TAD Music và giảm cỡ chữ để tên dài không bị cắt. Giữ thứ tự bước và các điều kiện thiết lập gốc.
- Rà thêm chuỗi UI viết cứng ở thành phần dùng chung, tìm kiếm, thư viện, menu trình phát, nhập/đồng bộ, bộ nhớ, âm thanh, thống kê và giới thiệu. Tái sử dụng resources sẵn có khi cùng nội dung; bổ sung resources có định dạng cho các câu chứa tên bài, nghệ sĩ, nguồn và số lượng. Không dịch tên bài/nghệ sĩ/dịch vụ do nguồn trả về; giữ nội dung giấy phép và ghi công gốc.
- Danh tính tìm kiếm cài đặt và nhãn model nội bộ bằng tiếng Anh được giữ ổn định khi cần; nội dung hiển thị được lấy từ resources để không đổi routing hoặc thuật toán.
- Kiểm tra thực tế của bản dịch mới và kết quả build cuối được cập nhật tiếp dưới đây. Các màn chưa mở trên máy thật không được tính là đã qua.


Kiểm tra trực tiếp bản dịch trên SM-S948B (locale app vi):

| Màn | Kết quả quan sát |
| --- | --- |
| Chào mừng | Tiếng Việt, tên ToTo TAD Music, tiêu đề và nút Tiếp tục không bị cắt |
| Hỏi nhập nhạc | Tiêu đề, mô tả, Nhập nhạc và Để sau đều tiếng Việt |
| Chọn cách nhập | Chuyển dữ liệu, Nhập, Khôi phục và mô tả đều tiếng Việt |
| Chọn dịch vụ | Tiêu đề và mô tả tiếng Việt; giữ tên Spotify/Apple Music/YouTube Music/Tidal. Còn cụm My Tracks, đã đưa vào đợt sửa tiếp |
| Hồ sơ ban đầu | Phần chính đã tiếng Việt nhưng còn {username}, joined at và chữ phụ quá tối; đã xác định dòng nguồn và sửa tiếp, chưa coi là qua trước khi cài bản sửa |

Không nhập tài khoản dịch vụ nhạc, không mở trang đăng nhập nguồn, không chạy import/restore và không thay đổi lựa chọn nguồn trong lúc xem các màn này. Các lựa chọn nhập chỉ được mở để kiểm tra rồi quay lại. Ảnh không chứa mật khẩu/token/cookie được lưu tại docs/verification/usb-*.png.


## Rà giao diện USB tiếp theo — 07/11/2026

- Đã mở Trang chủ, Tìm kiếm, Thư viện, trình phát, Hồ sơ, Cài đặt, Hành vi ứng dụng, bảng Ngôn ngữ và Ủng hộ trên SM-S948B. Phần nội dung hiện trên các màn này được đối chiếu trực tiếp; chưa coi toàn bộ màn con/tình huống lỗi là đã kiểm tra.
- Trang chủ và Thư viện: các tiêu đề, bộ lọc, hướng dẫn nhập, trạng thái trống hiện tiếng Việt. Trình phát mở được và có trạng thái đang phát; chưa có xác nhận âm thanh từ người dùng nên chưa đánh dấu ca phát online thành công.
- Tìm kiếm: phát hiện Gaming/Party từ dữ liệu nguồn; sửa riêng nhãn hiển thị thành Chơi game/Tiệc tùng, giữ nguyên browse ID, thứ tự và truy vấn.
- Hiệu ứng chuyển bài: phát hiện None; chuyển nhãn hiệu ứng sang resources, giữ nguyên enum và giá trị lưu.
- Hồ sơ: phát hiện About app trong nút giới thiệu; chuyển sang resource Giới thiệu ứng dụng. Tăng logo Trang chủ lên 44dp để tránh hiển thị quá nhỏ.
- Bảng chọn ngôn ngữ: phát hiện hai câu tiếng Anh từ bản vi cũ (theo ngôn ngữ điện thoại/cảnh báo bản dịch); đã dịch. Các mục đang hiển thị trong Hành vi ứng dụng đã tiếng Việt.
- Ủng hộ: xác nhận tên, lời mời ủng hộ và QR đúng ảnh người dùng cung cấp, ảnh hiện trọn chiều rộng và đầy đủ QR. Cột có cuộn; thông tin liên hệ phía dưới còn placeholder do chưa được cung cấp.
- Ảnh tạm của trình phát không lưu vào repo vì có lớp phủ chat của ứng dụng khác. Không thu thập thông tin đăng nhập.
- Còn thiếu: rà mọi màn con, đổi qua lại Anh/Việt bằng picker, hồ sơ sau sửa, màn lỗi tải xuống/thông báo; xác nhận âm thanh online, khóa màn hình/phát nền, file cục bộ và đăng xuất khi đang phát. Các ca gate/sai mật khẩu/khóa/mất mạng/hết hạn/deep link chưa được thử đầy đủ trên máy thật.

### Bản cài cuối của lượt rà UI

- Build và unit test: thành công; 139 test, 0 failure/error/skipped. Resource checker: 2.566 mặc định/2.565 tiếng Việt; diff check qua.
- Cài đè USB bằng install -r: Success, mở lại app thành công và phiên đăng nhập được giữ. Không gỡ app hoặc xóa dữ liệu.
- Đối chiếu lại trực tiếp sau cài: Tìm kiếm đã hiện Chơi game/Tiệc tùng; Hồ sơ đã hiện Giới thiệu ứng dụng. Các nhãn hiệu ứng và hai câu trong picker đã sửa/build, chưa đối chiếu lại sau cài.
- APK: app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk (89.594.750 byte).
- SHA-256: cbd8cf9298fb7ab6baa3c903f507a4a0ff6e494ec3a5cdfa18d941cd1d7e8d40.


## Hồ sơ gắn UUID Supabase — 07/11/2026

- Thêm migration 202611070001_account_profiles.sql: UPDATE theo cột, RLS hồ sơ của chính mình khi có quyền, bucket ảnh private 2 MiB JPEG và policy đúng UUID/avatar.jpg. Không chạy lên project thật.
- PostgreSQL cục bộ với hai UUID và mock Auth/Storage: qua các ca đọc/sửa của mình, chặn đọc/sửa người khác, chặn thay UUID/ngày tạo/quyền, chặn đường dẫn ảnh người khác và tài khoản bị khóa. Mock không thay cho HTTP Storage thật.
- App tải hồ sơ sau khi xác minh quyền, trước navigation; lưu hồ sơ từ onboarding/editor lên tài khoản, tải ảnh riêng tư bằng header auth; lỗi có thông báo/retry, không in dữ liệu nhạy cảm. Hồ sơ cũ chỉ được dùng khi người dùng chọn và lưu.
- Chưa cài đợt tính năng này lên USB: cần người dùng áp dụng migration trước để tránh làm bản đang dùng bị chặn vì thiếu cột/bucket. Chưa thử upload/download thật, chuyển tài khoản hay khôi phục hồ sơ trên thiết bị thứ hai.

- Người dùng xác nhận đã chạy migration hồ sơ thành công trên Supabase. Build/test mới nhất: 139 JVM tests qua, checker 2.570/2.569 tài nguyên qua, diff check qua. RLS local fixture qua. Việc upload/download trên project thật vẫn cần thử từ app.

- APK hồ sơ tài khoản mới: `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`, 89597530 byte; SHA-256 `79b226cb44c07a5a25141776dc3be2bcb1cba6bd68488b1d1696c44c0d30933f`.

- USB SM-S948B: cài đè tính năng hồ sơ thành công, không gỡ/xóa dữ liệu. Mở app giữ phiên, log cố định `stage=access outcome=GRANTED` sau bước đọc hồ sơ mới. Đã mở đến Tạo hồ sơ nghe nhạc: tên/ảnh trống theo row Supabase chưa tạo; có nút Dùng hồ sơ cũ trên máy, thông báo lưu theo tài khoản và Tiếp tục. Đang chờ người dùng chọn và lưu hồ sơ để kiểm tra ghi/upload thật.

### Kết quả thực tế sau migration và cài bản hồ sơ

- Người dùng đã tạo/lưu hồ sơ trên máy USB và xác nhận **tên và ảnh đều đúng**; app chỉ chuyển bước khi lưu và đọc lại từ Supabase thành công.
- Đóng hẳn app bằng am force-stop rồi mở lại: auth refresh và tải hồ sơ qua, log cố định GRANTED. App vào Trang chủ, không yêu cầu tạo lại hồ sơ. Mở Hồ sơ và người dùng xác nhận lần hai **tên và ảnh vẫn đúng**. Ca lưu/tải hồ sơ và ảnh trên tài khoản hiện tại qua.
- Chưa thử thực tế: sửa hồ sơ lần tiếp theo, thiết bị thứ hai, chuyển A/B trên điện thoại, mất mạng giữa upload và PATCH. Hai tài khoản chỉ được kiểm tra policy/GRANT bằng PostgreSQL mock cô lập. Không khẳng định các ca đó đã qua trên project thật.
- Tài liệu thao tác: docs/ToTo-SUPABASE-PROFILE.md. Không thu thập tên hồ sơ, ảnh cá nhân hay dữ liệu đăng nhập vào báo cáo/ảnh chụp repo.


## Sửa nháy màn đăng nhập và liên kết YouTube — 07/11/2026

- Người dùng báo mở lại app hiện form đăng nhập một lúc, và đăng nhập YouTube vẫn ở trang Google/YouTube.
- Nguyên nhân form nháy xác định trong AuthGate: nhánh checking dùng LoginScreen. Đổi sang màn xác minh riêng (logo/spinner, không có form email/mật khẩu); vẫn xác minh quyền mỗi lần foreground, không mở navigation trước khi thành công. Chặn xếp trùng tác vụ verify.
- AuthGate trước đó bỏ composition navigation trong lúc kiểm tra, có thể hủy WebView đăng nhập khi chuyển ra/vào Google/2FA. Giữ composition đã xác minh trong tiến trình dưới lớp che kín, chặn tương tác và ẩn accessibility khi kiểm tra; nếu xác minh lỗi/khóa thì bỏ nội dung bảo vệ. Không giữ nội dung sau logout hoặc bỏ gate.
- Luồng YouTube chỉ bắt onPageFinished của Music và im lặng nếu cookie chưa tới. Thêm Hoàn tất liên kết để thử lại sau chuyển trang động; từ trang YouTube chính có thể mở Music để nhận phiên. Kiểm tra host HTTPS chính xác.
- Trước đây accountInfo().getOrNull() vẫn ghi tài khoản rỗng và báo thành công khi API lỗi; đổi sang chỉ lưu/báo liên kết sau xác minh thành công. Lỗi hiển thị rõ và thử lại, không ghi stack/request/cookie. Log debug ToToYouTubeLogin chỉ nhãn cố định VERIFYING/COOKIE_PENDING/LINKED hoặc nhóm lỗi.
- Không thay đổi thứ tự nguồn, logic phát hay đồng bộ thư viện. Tài khoản YouTube được xem ở Cài đặt → Tích hợp → YouTube Music; tên/ảnh hồ sơ Supabase không bị tài khoản YouTube thay thế.
- Chờ kiểm tra APK mới trên USB và người dùng tự đăng nhập Google; chưa khẳng định lỗi liên kết đã hết chỉ dựa vào kiểm tra mã.

- Bản sửa build thành công (43s), 139 test qua, resource checker 2.577/2.576 qua, diff check qua. USB install -r Success, khởi động lạnh vào được app, phiên/hồ sơ được giữ. Đi từ Cài đặt → Tích hợp tiện ích → YouTube Music → Đăng nhập YouTube. Đang chờ người dùng tự đăng nhập và xác nhận kết quả.
- APK lượt sửa: 89602294 byte, SHA-256 `a20db91abfee8d31944ca836305141ad4d783bdefdecec1c221e052ffbfe10ce`.

### Xác nhận trên USB sau sửa

- Log debug được lọc chỉ nhãn cố định: `event=VERIFYING`, `event=LINKED`. UI đã tự quay về Đồng bộ và hiện nút Đăng xuất thay cho Đăng nhập YouTube. Không lưu ảnh chụp tên/email tài khoản.
- Người dùng xác nhận **Đã quay về và hiện tên tài khoản YouTube**.
- Người dùng tự ra Home của điện thoại rồi mở lại app và xác nhận **Tên YouTube hiện đúng, không còn nháy form**. Hai lỗi báo trong lượt này đã thử lại và qua trên SM-S948B.
- Chính sách xác minh quyền Supabase khi cold start/foreground vẫn giữ. Khi đang kiểm tra chỉ hiện logo/trạng thái xác minh; khi lỗi thật mới hiện màn phục hồi/đăng nhập.
- Chưa thử trong lượt này: đăng nhập Google bằng 2FA bên ngoài/đổi Google account, lỗi mạng Google/API và toàn bộ đồng bộ playlist. Không bật thêm tùy chọn upload hoặc bấm Đồng bộ ngay thay người dùng.


## Thương hiệu còn sót theo 7 ảnh người dùng — 07/11/2026

- Phát hiện: tác giả dự án gốc hiển thị như chủ app, link Discord gốc nằm ở vị trí cộng đồng bản cá nhân, logo Mochy trong About/header/navigation và các mẫu preview biểu tượng cũ, nhãn About và trạng thái mô-đun còn tiếng Anh.
- Thêm OwnerSheet dùng tên/logo/QR ToTo TAD Music. Các điểm vào từ Hồ sơ/About/Mô-đun mở trang chủ app; URL cá nhân cấu hình riêng một file, chưa có dữ liệu thì không tạo link và hiển thị chưa cấu hình.
- Tác giả gốc vẫn được ghi công riêng; bỏ nút dẫn tới QR chủ bản cá nhân khỏi trang tác giả gốc để tránh nhầm người nhận. Giữ giấy phép, copyright, repo Mochy và ghi công thư viện/dự án nền tảng.
- Biểu tượng thật trong picker, header, About và tab Hồ sơ dùng logo cá nhân. Trạng thái No account needed chuyển sang Không cần tài khoản ở lớp hiển thị; không đổi mã module/nguồn/thuật toán phát.
- Đang kiểm tra build và USB; các link cá nhân chờ chủ app cung cấp.

- Người dùng cung cấp Facebook tho.do.188, Instagram tho.anh.do01 và Discord invite hQatWSBBe; đã đưa đúng link vào cấu hình tập trung, OwnerSheet và mục liên hệ/ủng hộ. Cộng đồng từ Hồ sơ/Mô-đun mở Discord cá nhân. Không thêm email/website/repo vì người dùng xác nhận ngoài ra không có.

- Build cuối kèm link cá nhân thành công (49s), 139 test, 0 failure, 0 error, 0 skipped. Resource checker 2.606/2.599 qua (7 tài nguyên không dịch gồm các URL và danh sách locale), diff check qua. Kiểm tra chính xác 3 URL người dùng cung cấp qua.
- APK: `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`, 89619238 byte; SHA-256 `3616872a00fe513f28302cc4a6270dd125cedf2e71f6aeae03cc907442a9700d`.

### Đối chiếu USB bản thương hiệu mới

- USB install -r: Success, mở app giữ phiên và dữ liệu.
- Hồ sơ: đã hiện Chủ app · ToTo TAD Music và Cộng đồng Discord, logo header/tab cá nhân đúng ảnh cung cấp.
- OwnerSheet: mở thực tế, logo đúng, tên/mô tả vừa khung; đủ ba nút Facebook, Instagram, Cộng đồng Discord cùng Ủng hộ và Ghi công dự án gốc. Đã kiểm tra URL trong resources đúng chính xác link người dùng gửi; không mở mạng xã hội hoặc gửi tin nhắn để thử.
- Giới thiệu: mở thực tế, tên và logo cá nhân; tiêu đề Liên kết và ủng hộ, Tác giả và người đóng góp dự án gốc bằng tiếng Việt. Giữ nguồn Mochy rõ tên.
- Tùy chỉnh: mở thực tế, chỉ còn logo ToTo TAD Music và chú thích đúng, không còn preview anime/mark gốc.
- Mô-đun: mở và xem ảnh thực tế, banner logo cá nhân/Cộng đồng Discord; Monochrome và JioSaavn hiển thị Tắt · Không cần tài khoản. Không bật/tắt/đổi module trong lúc kiểm tra.
- Phần thông tin liên hệ mới của Support/Donate và nhãn ở trang tác giả gốc đã kiểm tra mã/build; chưa mở lại toàn bộ các phần dưới cuộn trong lượt này.


## Chính sách vào ngay, kiểm tra quyền nền — 07/11/2026

Theo yêu cầu mới đã được người dùng chấp thuận, thay thế chính sách chặn foreground/cold start trước đó:
- Biên nhận cấp quyền gắn UUID/project chỉ ghi sau xác minh server và hồ sơ, mã hóa cùng refresh token bằng Keystore trong noBackupFilesDir. Access token vẫn chỉ ở RAM. Đối chiếu owner của cache trước khi mở bằng biên nhận. Không có đăng nhập khách/nhánh bỏ qua kiểm tra cho phiên chưa từng được cấp quyền.
- Khôi phục cục bộ trước Activity/service; phiên được cấp quyền mở navigation ngay. Kiểm tra nền khi khởi động/foreground, gộp 60 giây cùng tiến trình, retry chủ động bỏ qua gộp. Deep link không bị chờ do busy của kiểm tra nền khi đã có quyền.
- Lỗi tạm thời giữ phiên, thông báo có Thử lại/Tiếp tục nghe nhạc. Xác nhận thu hồi/khóa/phiên không hợp lệ thì xóa cache quyền/refresh, dừng phát và về đăng nhập. Không xóa thư viện. Không kiểm tra định kỳ liên tục khi ở nền, nên khóa có hiệu lực khi lần xác minh kế tiếp hoàn tất; offline kéo dài vẫn dùng được quyền đã xác minh theo yêu cầu mới.
- Phiên refresh-only cũ được nâng định dạng sau một lần xác minh online. Không cần migration Supabase mới.
- Bổ sung SessionPolicyTest kiểm tra lỗi tạm không cấp quyền cho user chưa xác minh và thu hồi luôn thắng cache. Đang build và kiểm tra USB.

### Kết quả bản kiểm tra nền

- Build universalFossDebug thành công (47s), **142 JVM tests, 0 failure/error/skipped**; checker 2.610 mặc định/2.603 tiếng Việt qua; diff check qua.
- USB install -r Success. Lần đầu sau cập nhật xác minh online và nâng phiên refresh-only cũ; lần khởi động lạnh kế tiếp log `stage=restore outcome=CACHED_GRANT` trước `stage=access outcome=GRANTED`, app vào Trang chủ.
- Tạm tắt Wi-Fi và mobile data qua adb, cold start: khôi phục CACHED_GRANT rồi nhận CONNECTION_FAILURE. App hiện popup Chưa kiểm tra được quyền tài khoản, có Tiếp tục nghe nhạc/Thử xác minh lại; không về form đăng nhập. Đã khôi phục hai cài đặt mạng về trạng thái gốc trong finally.
- Đang đối chiếu tiếp thao tác bên trong app khi đóng popup, rồi mở lại có mạng. Thu hồi quyền/khóa/refresh không hợp lệ được kiểm tra logic/JVM, chưa thao tác khóa user thật trong lượt này. Không chạy SQL lên project.

- Đóng popup bằng Tiếp tục nghe nhạc trong lúc cả Wi-Fi/mobile data đang tắt rồi chuyển sang Thư viện: thao tác được, không quay về đăng nhập. Đã phục hồi hai cài đặt mạng. Đây là kiểm tra truy cập/thao tác offline; chưa xác nhận âm thanh file cục bộ trong lượt này.
- Sau phục hồi mạng, mở lạnh lại: log CACHED_GRANT và UI Trang chủ hiện trong khi kiểm tra nền chưa hoàn tất, chứng minh không cần chờ role mới mở navigation.

- APK kiểm tra nền: `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`, 89621854 byte; SHA-256 `dd8cd6746367bfb04fae021ec811e84f9ed3ba81860ffaa1cddc29e6f5d15518`.


## Splash bị cắt và crash sau cào thẻ — 07/11/2026

- Mở đúng màn cào thẻ trên USB, cào barcode để vào recap. Đọc báo cáo crash lưu trên máy, chỉ xuất tên exception/stack frame: UnknownFormatConversionException tại UiText.get → RecapStoryPagesKt.MonthPage (dòng 268). Không sao chép nội dung báo cáo raw/tên/ảnh hồ sơ vào repo.
- Root cause: UiText luôn gọi overload getString với vararg, kể cả khi rỗng, nên chuỗi literal “trên tổng 100%”/“out of 100%” bị xử lý như printf và lỗi. Helper mới chỉ định dạng khi có tham số; áp dụng cả plurals, giữ nguyên locale/placeholder. Thêm regression test phần trăm literal Anh/Việt và positional arguments + escaped percent.
- Splash: bitmap logo phủ kín vùng icon, bị mặt nạ Android cắt. Thêm inset 28% để toàn bộ ảnh nằm trong vòng tròn an toàn; style alias Android 12+ dùng cùng drawable có đệm thay vì fallback launcher. Không sửa ảnh QR/ảnh logo nguồn, không đổi launcher ID hay luồng nghe nhạc.
- Đang build, sau đó cài đè USB để kiểm tra lại recap và nhờ người dùng đối chiếu splash.

### Kết quả sửa splash/recap trên USB

- Build thành công (58s), 144 JVM tests qua gồm regression phần trăm literal và template có tham số. Checker tài nguyên và diff check qua. Install -r Success, không gỡ/xóa dữ liệu.
- Người dùng xem cold start trên bản mới và xác nhận **Logo đã hiện trọn vẹn**.
- Đã mở Hồ sơ, kéo mở mặt sau, cào barcode và vào story; thao tác chạm tiếp trên bản mới. UI tới trang hành trình có Hẹn gặp lại nhé!/CHẠM!, sau chạm tiếp đã qua trang tháng từng gây crash để tới trang thứ trong tuần; app vẫn chạy. Không có marker UnknownFormatConversionException/FATAL trong log lỗi của tiến trình hiện tại. Không đánh dấu đây là xác nhận âm thanh snippet/phát nền.
- APK `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk`, 89621854 byte; SHA-256 `c950678deafb842bca32aec31a01dfeb8fa806538ec8d0c8aba6af19a64a3c08`.

- Kiểm tra tiếp qua các trang sau: thấy Xem tổng kết của bạn, tới đúng CHẠM ĐỂ KẾT THÚC; bấm kết thúc đã quay về Hồ sơ bình thường. Không chỉ dừng ở trang hành trình.
- Bổ sung drawable-v26 giữ inset phần trăm cho Android mới; fallback Android 24/25 dùng inset 80dp, vì InsetDrawable của API 24 chỉ đọc dimension, không hỗ trợ fraction. Đối chiếu mã AOSP android-7.0.0_r1 (graphics/java/android/graphics/drawable/InsetDrawable.java); nhánh Android 24/25 kiểm tra mã/build, chưa có thiết bị API đó để thử. Android 17 dùng đúng drawable 28% đã được người dùng xác nhận.

- Đóng gói cuối có fallback API 24/25: build 8s qua, 144 tests qua, checker/diff check qua. Cài đè bản cuối Success. APK 89621995 byte, SHA-256 `34db594dfd9215cc9481afdb413c783d4556d21cabf56135dd04fff5e8352a39`.


## Nút Đăng xuất cuối trang Hồ sơ — 07/11/2026

- Thêm hàng Đăng xuất ở dưới Mở cài đặt và trước khoảng đệm đáy của Hồ sơ. Dùng icon logout và màu đỏ nhạt để phân biệt thao tác tài khoản; chuỗi Việt/Anh có sẵn.
- Gọi PersonalAuth.logout hiện có: thu hồi quyền cục bộ, xóa phiên/biên nhận, dừng phát và về gate; không xóa thư viện hay phiên nguồn nhạc. Không thêm API/luồng đăng xuất khác.
- Đang build/cài đè và kiểm tra vị trí nút trên USB.

- Build 45s thành công, 144 tests qua, diff check qua. APK 89621995 byte, SHA-256 `6a1eb750741dee07a15683ed9a6bdee8f5339683a44ab0a86bcc5c70a7a40410`.

- USB install -r Success. Đã mở Hồ sơ và cuộn cuối trang: Đăng xuất hiện ngay dưới Mở cài đặt; toàn bộ hàng nằm trên mini player, chữ không bị cắt/che. Chưa bấm đăng xuất trong lượt này để giữ phiên đang dùng; liên kết tới hàm logout hiện có đã kiểm tra mã.


## Rà bug upstream và bảo toàn bản cá nhân — 07/11/2026

- Người dùng ưu tiên ổn định, chỉ sửa bug thực sự của bản đang dùng. Đã đọc 8 issue đang mở gắn nhãn bug trên repo gốc và đối chiếu đường mã; chưa tái hiện đầy đủ các ca đó trên bản cá nhân, không sửa engine/Room/source order theo suy đoán. Chi tiết và mức xác minh ở ToTo-UPSTREAM-BUG-AUDIT.md.
- Chỉ fetch main từ origin chính thức: 21 commit mới, 223 file thay đổi, 60 file trùng vùng cá nhân. Không merge/cherry-pick/apply commit nào; không đổi APK trên điện thoại trong lượt này.
- Tạo nhánh cục bộ toto/personal, lưu toàn bộ bản cá nhân đã kiểm tra và tag toto-stable-20261107. Không push. Local.properties, phiên đăng nhập, khóa ký và APK build không đưa vào Git.
- Công cụ review_upstream.py chạy online và --no-fetch qua; check_personal_contract.py và check_personal_resources.py qua; diff check qua. Không chạy lại build/JVM vì lượt này chỉ thêm script/config/tài liệu, không sửa mã Android. APK nền hiện tại là bản 144 tests đã qua trước đó.
- Guard chỉ là kiểm tra hồi quy tích hợp/nhận diện, không chứng minh an toàn quyền truy cập hay tự giải quyết xung đột. Quy trình nhận commit phải review và thử trên nhánh riêng/USB trước khi cập nhật bản ổn định.


## Tạm ẩn nút mã nguồn ở Giới thiệu — 07/11/2026

- Theo yêu cầu chủ app, bỏ hàng Mã nguồn Mochy Music khỏi nhóm hành động của AboutSheet; không để nút vô hiệu hoặc vùng bấm trống. Giữ phần ghi công, copyright và giấy phép. Repo cá nhân chưa có nên không tạo URL thay thế.
- Đổi đúng một hàng UI; không đổi cơ chế cập nhật hoặc các URL ghi công trong màn tác giả gốc. Guard bản cá nhân và diff check qua; đang build/cài đè USB và kiểm tra màn thực tế.

- Build debug thành công (1m12s), guard và diff check qua. Không chạy lại JVM cho thay đổi chỉ bỏ một hàng UI; bộ 144 tests gần nhất vẫn là lượt trước, không ghi nhận là đã chạy lại. Thêm AboutSheet vào vùng review bảo vệ và guard cảnh báo nếu nút nguồn cũ xuất hiện lại.

- USB install -r Success, mở lại About thực tế: nhóm Liên kết và ủng hộ không còn Mã nguồn Mochy Music; vẫn thấy Tác giả và người đóng góp dự án gốc. APK 89621995 byte, SHA-256 `53b0121c46c761d316930e4ae42a15ec54cb4665f6721d8003b45d93f54231a1`.


## Một hồ sơ chủ app trong Giới thiệu — 07/11/2026

- Thay hai thẻ tác giả gốc bằng một PersonRow ToTo TAD Music, logo cá nhân, phụ đề Chủ app và mở trang link cá nhân. Đổi tiêu đề thành Chủ app, bỏ nút chủ app trùng ở phía trên.
- Ghi công Roo/realidkroo và key/realkeichi giữ dạng chữ, không có avatar/link hồ sơ ở section này. OwnerSheet cũng ghi công bằng chữ thay vì mở hai profile cũ. Giữ giấy phép/copyright/các dự án nền tảng và ghi công còn lại.
- Guard cảnh báo khi vòng lặp thẻ tác giả cũ được đưa lại. Checker tài nguyên 2.612/2.605 và diff check qua. Đang build/cài USB; không chạy JVM lại cho thay đổi trình bày UI.

- Build debug thành công (2m1s), guard/checker/diff check qua. APK 89623247 byte, SHA-256 `6e9d3de50759a09324f8d805d957ea59698898435ce8e5a874059179e0d54b26`.

- USB install -r Success. Đã mở About: mục Chủ app có đúng một thẻ ToTo TAD Music, không còn hai avatar/hàng bấm Roo/key; dòng ghi công gốc chỉ là Text. Bấm thẻ mới mở đúng Facebook, Instagram, Discord và Ủng hộ cá nhân. Nội dung không bị cắt trong vùng quan sát.

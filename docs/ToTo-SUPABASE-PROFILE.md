# Hồ sơ nghe nhạc theo tài khoản Supabase

## Áp dụng trước khi cài APK mới

1. Mở project Supabase của bạn → **SQL Editor → New query**.
2. Mở file `supabase/migrations/202611070001_account_profiles.sql` trong repo, sao chép toàn bộ nội dung vào SQL Editor và bấm **Run** một lần. Migration đầu `202611060001_personal_access.sql` phải đã được áp dụng (bản đăng nhập hiện tại đã dùng các bảng đó).
3. Kiểm tra **Table Editor → profiles** có `avatar_path`, `profile_completed`; **Storage** có bucket `toto-avatars`, trạng thái **Private**, giới hạn 2 MB, JPEG.
4. Không đổi bucket thành Public. Các policy bucket này chỉ cho tài khoản được cấp quyền đọc/ghi đường dẫn `UUID/avatar.jpg` của mình. Nếu project có policy Storage rộng từ trước, cần kiểm tra chúng: policy permissive khác có thể mở thêm quyền ngoài migration này.
5. Báo đã chạy xong để cài đè APK và thử trên điện thoại. Không cần gửi email, mật khẩu hay khóa bí mật.

Agent chỉ tạo migration, không tự chạy lên project thật.

## App hoạt động thế nào

- Đăng nhập mới lấy `profiles` đúng UUID sau khi xác minh phiên và `app_access.enabled`. Phiên đã cấp quyền được khôi phục bằng Keystore để mở ngay hồ sơ cache đúng chủ; kiểm tra và tải lại hồ sơ ở nền khi mở app. Tên/ảnh của tài khoản trước không được dùng làm hồ sơ của tài khoản mới.
- Tạo hồ sơ hoặc Sửa hồ sơ: lưu tên và trạng thái đã tạo vào `profiles`; ảnh được giải mã thu nhỏ, loại metadata và nén JPEG trước khi gửi lên Storage riêng tư. App chỉ thay bản hiển thị cục bộ khi đọc lại được dữ liệu từ server.
- Đăng nhập cùng tài khoản trên máy khác sẽ tải tên/ảnh từ Supabase. Ảnh được tải bằng Authorization vào bộ nhớ riêng của app; không tạo URL công khai hay lưu access token vào URL ảnh.
- Chưa có phiên đã cấp quyền: lỗi migration/mạng khi tải hồ sơ hiện thông báo và Thử lại trên màn đăng nhập. Với phiên đã xác minh khôi phục được, lỗi tạm thời hiện popup và giữ hồ sơ cache đúng chủ; không lấy hồ sơ tài khoản khác.
- Lưu thất bại: giữ màn chỉnh sửa, hiện thông báo, cho bấm lưu lại. Phiên hết hạn khi đang sửa lâu: quay lại app để xác minh/refresh rồi thử lại. Lưu ảnh và dòng profiles là hai yêu cầu riêng, chưa phải giao dịch nguyên tử; khi một bước lỗi, thử lại sẽ cập nhật cùng đường dẫn ảnh.
- Hồ sơ cũ trước khi có tài khoản được giữ tạm trong vùng no-backup. Ở bước tạo hồ sơ, nút **Dùng hồ sơ cũ trên máy** điền tên/ảnh cũ để bạn xem; bấm **Tiếp tục** mới gắn vào tài khoản đang đăng nhập. Trên máy dùng chung, chỉ chọn nếu đó là hồ sơ của bạn. Có thể nhập tên và chọn ảnh khác.
- Chỉ tên, ảnh, trạng thái hoàn thành hồ sơ và ngày tạo thuộc tài khoản. Playlist, thống kê/lịch sử nghe, file nhạc, bản tải, cookie và tài khoản nguồn nhạc vẫn lưu cục bộ như trước; không được đồng bộ hay tách thư viện theo tài khoản bởi thay đổi này.
- Thay đổi này mở quyền UPDATE riêng các cột `display_name`, `avatar_path`, `profile_completed`. Người dùng không sửa được UUID, ngày tạo, email hay app_access. Email vẫn ở Supabase Auth.

## Cần thử trên máy thật

1. Tài khoản đã cấp quyền đăng nhập → tạo hồ sơ, chọn ảnh → Tiếp tục.
2. Xem tên/ảnh tại Hồ sơ, Trang chủ; thoát/mở app và kiểm tra vẫn đúng.
3. Sửa tên/ảnh, lưu rồi mở lại; tên/ảnh mới phải hiện ngay.
4. Đăng nhập cùng tài khoản trên thiết bị thứ hai (hoặc bản cài sạch riêng, không xóa app đang dùng) → tên/ảnh được phục hồi.
5. Đăng xuất, dùng tài khoản B → không thấy tên/ảnh A; thư viện nhạc trên máy vẫn được giữ theo thiết kế hiện có.
6. Mất mạng khi lưu → không báo lưu thành công; kết nối lại và thử lại.
7. Khóa tài khoản trong Dashboard rồi đưa app trở lại foreground → khi kiểm tra nền hoàn tất (gộp 60 giây) thì gate chặn; ảnh riêng tư cũng bị RLS chặn.

## Kiểm tra đã chạy

Build universalFossDebug, JVM tests và RLS cục bộ được ghi ở `docs/ToTo-TEST-RESULTS.md`. Script `scripts/test_personal_rls_local.sh` dùng PostgreSQL cô lập với mock Auth/Storage: kiểm tra policy và GRANT hai UUID, không phải kiểm thử HTTP Storage thật. Chưa xác nhận upload/download trên project thật trước khi người dùng áp dụng migration.

Thiết kế bucket riêng tư và policy tham khảo tài liệu chính thức:
- https://supabase.com/docs/guides/storage/security/access-control
- https://supabase.com/docs/guides/storage/serving/downloads

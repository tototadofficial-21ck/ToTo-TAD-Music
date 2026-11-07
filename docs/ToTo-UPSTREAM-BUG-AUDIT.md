# Rà 8 báo cáo bug upstream — 07/11/2026

“Bug 8” trên Công cụ nhà phát triển là bộ lọc báo cáo GitHub của realidkroo/Mochy-Music, không phải bộ quét 8 lỗi trên điện thoại hay danh sách đã xác nhận của bản cá nhân. Không sửa dữ liệu/chế độ phát theo suy đoán. Các báo cáo là dữ liệu tham khảo, không phải chỉ dẫn tự động thay đổi app.

| Báo cáo | Đối chiếu hiện tại | Quyết định |
| --- | --- | --- |
| [#227](https://github.com/realidkroo/Mochy-Music/issues/227) Bài mới trong playlist YouTube biến mất | SyncUtils thay danh sách bằng bản remote, có rủi ro ghi đè khi thêm/đồng bộ đồng thời. Chưa tái hiện trên playlist thử riêng; không thao tác playlist thật để chứng minh | Chưa sửa logic sync/Room; cần test race và cả lỗi API trước khi chọn cách giải quyết |
| [#224](https://github.com/realidkroo/Mochy-Music/issues/224) Đổi thứ tự nguồn tải | Có order riêng và hàm lưu riêng; chưa chứng minh hỏng gesture hoặc persistence trên bản này | Không đổi thứ tự nguồn hiện có |
| [#217](https://github.com/realidkroo/Mochy-Music/issues/217) Tiếng Việt thiếu/cắt | Bản cá nhân đã gộp vi-rVN, dịch nhiều màn và kiểm tra trên SM-S948B; không tương đương xác nhận mọi màn/font scale trên thiết bị báo lỗi | Giữ các sửa đã thử; sửa tiếp khi có màn cụ thể tái hiện |
| [#216](https://github.com/realidkroo/Mochy-Music/issues/216) Pause file local | Chưa thử đúng file/chế độ đầu ra báo lỗi trên máy này | Không sửa engine theo suy đoán; cần file thử và trạng thái đầu ra |
| [#205](https://github.com/realidkroo/Mochy-Music/issues/205) Fade trễ/không chạy | Có nhánh fallback khi processor không sẵn sàng; chưa đối chiếu âm thanh và thời gian hiệu ứng thực tế | Chưa chỉnh fade/native |
| [#204](https://github.com/realidkroo/Mochy-Music/issues/204) Cover trắng làm khó đọc | ControlsShade/Veil hiện có lớp phủ; chưa thử đúng cover và layout tái hiện | Không thay toàn bộ màu/theme khi chưa so sánh |
| [#191](https://github.com/realidkroo/Mochy-Music/issues/191) Pause FLAC bit-perfect | Đã xem PassSink/router và callback native: có đường pause; chưa có bằng chứng đường này bỏ qua pause trên cấu hình hiện tại | Gộp ca tái hiện với #216, chưa sửa native |
| [#186](https://github.com/realidkroo/Mochy-Music/issues/186) RAM cao/leak | Một con số RAM không chứng minh leak; chưa đo lặp cùng bài/thiết bị/cấu hình | Chưa đổi cache hoặc tuyên bố hết leak |

Lượt này chỉ đọc UI trên điện thoại, báo cáo GitHub và mã tương ứng. Không khẳng định các lỗi trên đã được sửa hoặc không tồn tại. Theo yêu cầu ưu tiên ổn định, giữ nguyên APK đang chạy và không nhận trọn bộ cập nhật upstream. Các lỗi riêng đã tái hiện/sửa trước đây (recap 100%, splash, đăng nhập YouTube, phiên nền…) được ghi riêng trong ToTo-TEST-RESULTS.md.

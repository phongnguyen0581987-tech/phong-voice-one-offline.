# Phong Voice One — Android Offline (source build)

Ứng dụng Android riêng để nhập văn bản tiếng Việt, yêu cầu tạo giọng **Phong**, nghe và lưu WAV/MP3. Không kết nối cloud, không gọi ElevenLabs.

**Đây là ứng dụng khách (client), không phải mô hình nhân bản giọng độc lập.** Trên điện thoại cần cài [CloneTTS](https://github.com/sipeter/CloneTTS/releases), tạo giọng với tên/alias `Phong`, bật `Settings > Local HTTP API Service`. Sau khi cài mô hình, bộ máy có thể xử lý offline. Chất lượng đọc tiếng Việt và độ giống giọng chưa được kiểm chứng.

## Quy trình
1. Tải APK từ **Actions > Build Phong Voice One APK > build-debug-apk > Artifacts**. File tên `PhongVoice-One-Android-debug-APK.zip`; giải nén để lấy `app-debug.apk`.
2. Cài `app-debug.apk` vào điện thoại. Android có thể hỏi quyền cài ứng dụng ngoài Play Store.
3. Cài CloneTTS từ trang Releases chính chủ. Trong CloneTTS: Voices > Add Voice > Select External để chọn **file âm thanh cá nhân đang lưu trong điện thoại** (KHÔNG nằm trong repo), dán lời mẫu khớp chính xác, đặt alias `Phong`, Save & Enable. Trong Settings bật Local HTTP API Service (cổng 8080).
4. Mở Phong Voice One, nhấn Kiểm tra kết nối, dán câu ngắn và bấm TẠO GIỌNG ĐỌC; thử offline bằng cách bật chế độ máy bay.
5. Kết quả WAV được lưu dưới đuôi WAV, MP3 được lưu dưới đuôi MP3. Không đổi đuôi WAV thành MP3.

## Bảo vệ dữ liệu
Kho mã nguồn này là PUBLIC. **KHÔNG đăng giọng mẫu thật, bản ghi âm, khóa cá nhân, hoặc văn bản riêng tư lên GitHub.** File mẫu được nhập **trực tiếp trên Samsung** vào CloneTTS. Mã nguồn chỉ gửi văn bản tới `http://127.0.0.1:8080/api/tts` qua kết nối loopback trên chính điện thoại; không có mô hình TTS hoặc dữ liệu giọng trong APK này. `INTERNET` là permission Android bắt buộc để gọi HTTP loopback.

## Kỹ thuật
Android Java không dùng dependency ngoài; Java 17, AGP 8.7.3, Gradle 8.9, Android SDK 35. Push vào main sẽ chạy GitHub Actions và đính kèm debug APK trong Artifacts nếu biên dịch thành công. Đây là APK debug (dùng thử), không phải bản release đã ký phát hành.

Tham khảo: https://github.com/sipeter/CloneTTS (API POST /api/tts, form-urlencoded, voice alias).

**Giới hạn:** Không khẳng định đọc tiếng Việt giống giọng mẫu trước khi kiểm thử bằng Samsung. Nếu CloneTTS không hỗ trợ phát âm tiếng Việt phù hợp, cần thay mô hình TTS, không tự ý thay sang giọng khác.

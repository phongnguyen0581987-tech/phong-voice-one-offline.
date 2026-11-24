# Phong Voice One 1.4.0 — tiếng Việt offline

Đây là bản thay thế CloneTTS bằng bộ máy **Android TextToSpeech**, yêu cầu giọng đọc vi-VN **không cần mạng**. Bản này nhập văn bản tiếng Việt (tối đa 3.000 ký tự), tạo WAV, nghe lại và lưu file bằng điện thoại Samsung.

**Lưu ý bắt buộc:** Bản này sử dụng một giọng tiếng Việt có sẵn. **Chưa sử dụng file giọng mẫu Phong và không có tính năng voice cloning.** Để clone thật sự phải tích hợp mô hình Việt riêng và kiểm thử trên thiết bị; không tự gọi giọng mặc định là giọng Phong.

## Cài đặt
1. Cài APK Phong Voice One 1.4.0 từ GitHub Actions.
2. Cài bộ máy tiếng Việt Android sherpa-onnx từ trang chính chủ: https://k2-fsa.github.io/sherpa/onnx/tts/apk-engine.html
3. Trên Samsung ARM64, tìm file engine tên có `arm64-v8a-vie-tts-engine-vits-piper-vi_VN-vais1000-medium.apk`. Đây là bản Piper được huấn luyện cho tiếng Việt. Không phải giọng clone cá nhân.
4. Vào Cài đặt Samsung, tìm "Chuyển văn bản thành giọng nói" / "Text-to-speech output", chọn công cụ ưu tiên sherpa-onnx tiếng Việt.
5. Mở Phong Voice One và bấm KIỂM TRA GIỌNG TIẾNG VIỆT, sau đó nhập một câu ngắn và bấm TẠO ÂM THANH TIẾNG VIỆT.
6. Khi đã tạo WAV thì NGHE THỬ và LƯU FILE WAV sẽ được bật.

Phần mềm chỉ chọn giọng có mã ngôn ngữ `vi` và `isNetworkConnectionRequired() == false`. Không có giọng Việt offline thì báo lỗi, **không thay bằng giọng tiếng Anh/Trung hoặc gọi giọng trên Internet**.

Bản 1.4.0 KHÔNG sử dụng CloneTTS, HTTP local port 8080 hay permission INTERNET. Mã nguồn công khai không chứa dữ liệu giọng mẫu cá nhân.

APK biên dịch được trên GitHub không đồng nghĩa đã chạy thử thành công trên Samsung. Sau khi biên dịch cần cài và kiểm tra lại một câu tiếng Việt, phát lại WAV và lưu file.

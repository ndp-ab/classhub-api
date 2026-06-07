package com.classhub.classhubapi.service;

import com.classhub.classhubapi.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final Path uploadRoot;

    public FileStorageService(@Value("${classhub.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeEventCheckinImage(Long eventId, Long userId, MultipartFile file) {
        validateImage(file);

        LocalDate today = LocalDate.now();
        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;
        String relativePath = String.format(
                "event-checkins/%d/%02d/event-%d/user-%d/%s",
                today.getYear(),
                today.getMonthValue(),
                eventId,
                userId,
                filename);

        Path target = uploadRoot.resolve(relativePath).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BadRequestException("Duong dan upload khong hop le");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Khong the luu anh diem danh");
        }

        return "/uploads/" + relativePath.replace("\\", "/");
    }

    /**
     * Xóa file theo relative path dạng "/uploads/...". Không throw exception nếu xóa lỗi.
     * Chỉ dùng để cleanup file mồ côi khi lưu DB thất bại sau khi đã ghi file.
     */
    public void deleteByRelativePathQuietly(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            // Cắt tiền tố "/uploads/" để lấy relative path trong uploadRoot
            String subPath = relativeUrl.substring("/uploads/".length());
            Path target = uploadRoot.resolve(subPath).normalize();
            // Kiểm tra path traversal trước khi xóa
            if (!target.startsWith(uploadRoot)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (Exception ignored) {
            // Quiet: lỗi cleanup không được lan ra để che lỗi gốc
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Anh diem danh khong duoc rong");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Anh diem danh khong duoc vuot qua 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException("File phai la anh");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Chi ho tro anh jpg, jpeg hoac png");
        }
    }

    private String getExtension(String originalFilename) {
        String filename = StringUtils.cleanPath(originalFilename != null ? originalFilename : "");
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new BadRequestException("Ten file anh khong hop le");
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}

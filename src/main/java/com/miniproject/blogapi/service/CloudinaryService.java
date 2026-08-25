package com.miniproject.blogapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.miniproject.blogapi.exception.AttachmentUploadException;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    public String uploadFile(MultipartFile file) {
        validateFile(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload attachment '{}' to Cloudinary", file.getOriginalFilename(), e);
            throw new AttachmentUploadException(
                    "Failed to upload attachment: " + file.getOriginalFilename(), e
            );
        }
    }

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AttachmentUploadException(
                    "Unsupported file type"
                            + (contentType != null ? " (" + contentType + ")" : "")
                            + " for '" + file.getOriginalFilename()
                            + "'. Allowed: JPEG, PNG, GIF, WEBP, PDF",
                    null
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AttachmentUploadException(
                    "File '" + file.getOriginalFilename() + "' exceeds the 5MB size limit", null
            );
        }
    }
}

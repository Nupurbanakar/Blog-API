package com.miniproject.blogapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a single file to Cloudinary and returns its public URL.
     * Throws AttachmentUploadException on any failure -- the caller
     * decides what to do with that (see PostServiceImpl in Part E).
     */
    public String uploadFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );
            return (String) result.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload attachment '{}' to Cloudinary", file.getOriginalFilename(), e);
            throw new com.miniproject.blogapi.exception.AttachmentUploadException(
                    "Failed to upload attachment: " + file.getOriginalFilename(), e
            );
        }
    }
}

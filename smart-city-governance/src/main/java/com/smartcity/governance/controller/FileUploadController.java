package com.smartcity.governance.controller;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {

        // ✅ Create uploads folder if not exists
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
			dir.mkdirs();
		}

        // ✅ Generate unique filename to avoid overwrite
        String uniqueName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String filePath = UPLOAD_DIR + uniqueName;

        file.transferTo(new File(filePath));

        // ✅ Return path that frontend can use
        return ResponseEntity.ok(uniqueName);
    }
}
package com.emia.archives.controller;

import com.emia.archives.dto.BulletinData;
import com.emia.archives.service.BulletinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BulletinController {

    private final BulletinService bulletinService;

    @PostMapping("/schools/{schoolId}/bulletins/{stagiaireId}")
    public ResponseEntity<byte[]> generateBulletin(@PathVariable String schoolId,
                                                   @PathVariable Long stagiaireId,
                                                   @RequestBody(required = false) BulletinData data) throws Exception {
        if (data != null) {
            bulletinService.saveBulletin(schoolId, stagiaireId, data);
        }
        byte[] pdf = bulletinService.generateBulletin(schoolId, stagiaireId, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=bulletin-" + stagiaireId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/schools/{schoolId}/bulletins/{stagiaireId}")
    public ResponseEntity<byte[]> generateBlankBulletin(@PathVariable String schoolId,
                                                        @PathVariable Long stagiaireId) throws Exception {
        return generateBulletin(schoolId, stagiaireId, null);
    }

    @GetMapping("/schools/{schoolId}/bulletins/{stagiaireId}/data")
    public ResponseEntity<BulletinData> getSavedBulletin(@PathVariable String schoolId,
                                                         @PathVariable Long stagiaireId) {
        BulletinData data = bulletinService.getBulletin(schoolId, stagiaireId);
        return data != null ? ResponseEntity.ok(data) : ResponseEntity.noContent().build();
    }
}
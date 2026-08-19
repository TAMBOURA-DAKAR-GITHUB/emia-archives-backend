package com.emia.archives.controller;

import com.emia.archives.service.DiplomaService;
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

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DiplomaController {

    private final DiplomaService diplomaService;

    @GetMapping("/schools/{schoolId}/diplomas/{stagiaireId}")
    public ResponseEntity<byte[]> generateDiploma(@PathVariable String schoolId, @PathVariable Long stagiaireId)
            throws Exception {
        byte[] pdf = diplomaService.generateDiploma(schoolId, stagiaireId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=diplome-" + stagiaireId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/schools/{schoolId}/diplomas/{stagiaireId}/preview", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> generateDiplomaPreview(@PathVariable String schoolId, @PathVariable Long stagiaireId)
            throws Exception {
        byte[] jpeg = diplomaService.generateDiplomaPreview(schoolId, stagiaireId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=apercu-" + stagiaireId + ".jpg")
                .contentType(MediaType.IMAGE_JPEG)
                .body(jpeg);
    }

    @PostMapping("/schools/{schoolId}/diplomas/batch")
    public ResponseEntity<byte[]> generateBatch(@PathVariable String schoolId,
                                                @RequestBody List<Long> stagiaireIds) throws Exception {
        byte[] pdf = diplomaService.generateDiplomasPdf(schoolId, stagiaireIds);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=diplomes.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}

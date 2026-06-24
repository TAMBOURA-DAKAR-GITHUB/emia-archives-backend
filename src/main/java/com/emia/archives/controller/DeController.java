package com.emia.archives.controller;

import com.emia.archives.dto.PersonnelDto;
import com.emia.archives.dto.request.CreatePersonnelRequest;
import com.emia.archives.dto.response.ApiResponse;
import com.emia.archives.enums.PersonnelCategorie;
import com.emia.archives.service.PersonnelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/de")
@RequiredArgsConstructor
public class DeController {

    private final PersonnelService personnelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonnelDto>>> getAll(@PathVariable String schoolId) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getAll(schoolId, PersonnelCategorie.DE)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> getById(@PathVariable String schoolId,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getById(id, schoolId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonnelDto>> create(@PathVariable String schoolId,
                                                             @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.create(schoolId, request, PersonnelCategorie.DE),
                "Membre DE créé avec succès"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonnelDto>> update(@PathVariable String schoolId,
                                                             @PathVariable Long id,
                                                             @RequestBody CreatePersonnelRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                personnelService.update(id, schoolId, request),
                "Membre DE modifié avec succès"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String schoolId,
                                                     @PathVariable Long id) {
        personnelService.delete(id, schoolId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Membre DE supprimé avec succès"));
    }

    @GetMapping("/{id}/com-emia")
    public ResponseEntity<ApiResponse<List<PersonnelDto>>> getComEmia(@PathVariable String schoolId,
                                                                       @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personnelService.getDeComEmia(id, schoolId)));
    }
}

package com.VSong.controller;

import com.VSong.dto.VtuberRequest;
import com.VSong.entity.VtuberEntity;
import com.VSong.service.VtuberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/vtubers")
public class AdminVtuberController {

    private final VtuberService vtuberService;

    public AdminVtuberController(VtuberService vtuberService) {
        this.vtuberService = vtuberService;
    }

    @PostMapping
    public ResponseEntity<VtuberEntity> createVtuber(@RequestBody VtuberRequest vtuberRequest) {
        VtuberEntity vtuberEntity = vtuberService.createVtuber(vtuberRequest.getDescription(), vtuberRequest.getGender());
        return new ResponseEntity<>(vtuberEntity, HttpStatus.CREATED);
    }
}

package com.abel.sentinel.controller;

import com.abel.sentinel.dto.PositionDTO;
import com.abel.sentinel.service.PositionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'OPERATOR', 'ADMIN')")
    public List<PositionDTO> getCurrentPositions() {
        return positionService.getCurrentPositions();
    }
}
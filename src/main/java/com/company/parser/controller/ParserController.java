package com.company.parser.controller;

import com.company.parser.model.dto.ParsingResultDTO;
import com.company.parser.model.dto.PriceDTO;
import com.company.parser.service.parser.ParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/parser")
@RequiredArgsConstructor
public class ParserController {

    private final ParserService parserService;

    /**
     * Запуск парсинга всех сайтов
     */
    @PostMapping("/parse")
    public ResponseEntity<ParsingResultDTO> startParsing() {
        log.info("Received request to start parsing");

        if (parserService.isRunning()) {
            return ResponseEntity.badRequest()
                    .body(ParsingResultDTO.failure("Parsing already in progress"));
        }

        ParsingResultDTO result = parserService.executeParsing();

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Парсинг конкретного сайта
     */
    @PostMapping("/parse/{siteId}")
    public ResponseEntity<ParsingResultDTO> parseSite(@PathVariable String siteId) {
        log.info("Received request to parse site: {}", siteId);

        ParsingResultDTO result = parserService.parseSite(siteId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Получение статуса парсинга
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        boolean isRunning = parserService.isRunning();

        return ResponseEntity.ok(Map.of(
                "running", isRunning,
                "status", isRunning ? "RUNNING" : "IDLE"
        ));
    }

    /**
     * Получение последних результатов
     */
    @GetMapping("/results")
    public ResponseEntity<List<PriceDTO>> getLatestResults() {
        List<PriceDTO> results = parserService.getLatestResults();
        return ResponseEntity.ok(results);
    }

    /**
     * Остановка парсинга
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stopParsing() {
        log.info("Received request to stop parsing");

        if (!parserService.isRunning()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No parsing in progress"));
        }

        parserService.stopParsing();
        return ResponseEntity.ok(Map.of("message", "Parsing stop requested"));
    }
}

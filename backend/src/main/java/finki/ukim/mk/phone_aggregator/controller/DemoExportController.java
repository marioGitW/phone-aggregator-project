package finki.ukim.mk.phone_aggregator.controller;

import finki.ukim.mk.phone_aggregator.service.DemoExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local dev tool that (re)generates the static-demo-mode fixtures under
 * frontend/public/demo-data/ from the current database. No auth (matching the rest of this
 * app) - not meant to be exposed publicly. See root README's "Regenerating the demo
 * fixtures" section.
 */
@RestController
@CrossOrigin(origins = "*")
public class DemoExportController {

    private static final Path DEFAULT_OUTPUT_DIR = Path.of("..", "frontend", "public", "demo-data");

    private final DemoExportService demoExportService;

    public DemoExportController(DemoExportService demoExportService) {
        this.demoExportService = demoExportService;
    }

    @GetMapping("/api/export-demo")
    public ResponseEntity<Map<String, Object>> exportDemo(
            @RequestParam(required = false) String outputDir
    ) throws IOException {
        Path resolved = outputDir != null ? Path.of(outputDir) : DEFAULT_OUTPUT_DIR;
        DemoExportService.ExportResult result = demoExportService.export(resolved);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Demo data exported successfully");
        response.put("outputDir", result.outputDir());
        response.put("modelCount", result.modelCount());
        response.put("offerCount", result.offerCount());
        return ResponseEntity.ok(response);
    }
}

package com.ithra.library.controller;

import com.ithra.library.dto.*;
import com.ithra.library.entity.MediaFile;
import com.ithra.library.service.MediaAnalysisService;
import com.ithra.library.service.QueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaAnalysisService mediaAnalysisService;
    private final QueryService queryService;

    @GetMapping("/")
    public String index(Model model) {
        List<MediaFile> mediaFiles = mediaAnalysisService.getAllMediaFiles();
        model.addAttribute("mediaFiles", mediaFiles);
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/";
            }

            MediaFile mediaFile = mediaAnalysisService.uploadFile(file);

            redirectAttributes.addFlashAttribute("success",
                    "File uploaded successfully! Processing started.");
            redirectAttributes.addFlashAttribute("mediaFileId", mediaFile.getId());

            return "redirect:/result/" + mediaFile.getId();

        } catch (Exception e) {
            log.error("Error uploading file", e);
            redirectAttributes.addFlashAttribute("error",
                    "Failed to upload file: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Long id, Model model) {
        try {
            MediaAnalysisResult result = mediaAnalysisService.getAnalysisResult(id);
            model.addAttribute("result", result);
            return "result";
        } catch (Exception e) {
            log.error("Error loading result", e);
            model.addAttribute("error", "Failed to load analysis result");
            return "error";
        }
    }

    @GetMapping("/api/result/{id}")
    @ResponseBody
    public ResponseEntity<Object> getResult(@PathVariable Long id) {
        try {
            MediaAnalysisResult result = mediaAnalysisService.getAnalysisResult(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting result", e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/api/query")
    @ResponseBody
    public ResponseEntity<Object> processQuery(@RequestBody QueryRequest request) {
        try {
            QueryResponse response = queryService.processQuery(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing query", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/files")
    public String listFiles(Model model) {
        List<MediaFile> files = mediaAnalysisService.getAllMediaFiles();
        model.addAttribute("files", files);
        return "files";
    }
}
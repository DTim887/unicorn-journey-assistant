package com.unicorn.journey.assistant.fileparse.controller;

import com.unicorn.journey.assistant.fileparse.dto.FileParseResult;
import com.unicorn.journey.assistant.fileparse.dto.ParseResponse;
import com.unicorn.journey.assistant.fileparse.service.ParserService;
import org.apache.tika.metadata.Metadata;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ParserController {

    private final ParserService parserService;

    public ParserController(ParserService parserService) {
        this.parserService = parserService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok");
    }

    @PostMapping(path = "/files/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParseResponse parseFiles(@RequestPart("files") MultipartFile[] files) {
        List<FileParseResult> results = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .map(this::parseOne)
                .collect(Collectors.toList());
        return new ParseResponse(results);
    }

    private FileParseResult parseOne(MultipartFile file) {
        FileParseResult r = new FileParseResult();
        r.setId(UUID.randomUUID().toString());
        r.setFilename(file.getOriginalFilename());
        r.setSize(file.getSize());
        List<String> warnings = new ArrayList<>();

        try (InputStream isForDetect = file.getInputStream(); InputStream isForParse = file.getInputStream()) {
            String detected = parserService.detectMime(isForDetect);
            String clientType = file.getContentType();
            r.setMimeType(StringUtils.hasText(detected) ? detected : clientType);

            Metadata md = new Metadata();
            String text = parserService.parseToText(isForParse, md);
            r.setText(text);

            Map<String, Object> meta = new LinkedHashMap<>();
            String pages = md.get("xmpTPg:NPages");
            if (pages != null) meta.put("pages", pages);
            String language = md.get("language");
            if (language != null) meta.put("language", language);
            meta.put("parsed", Boolean.TRUE);
            r.setMetadata(meta);

            if (text.isEmpty()) warnings.add("未抽取到文本，可能为扫描件或图片，已启用OCR需安装 tesseract-ocr 与语言包");
        } catch (Exception e) {
            r.setText("");
            warnings.add("解析失败: " + e.getClass().getSimpleName());
        }

        r.setWarnings(warnings);
        return r;
    }
}

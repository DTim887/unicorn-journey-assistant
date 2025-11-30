package com.unicorn.journey.assistant.fileparse.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class ParserService {

  private final Tika tika = new Tika();

  @Value("${app.parse.maxTextLength}")
  private int maxTextLength;
  @Value("${app.parse.ocrEnabled}")
  private boolean ocrEnabled;
  @Value("${app.parse.ocrLanguages}")
  private String ocrLanguages;

  public String detectMime(InputStream is) throws Exception {
    return tika.detect(is);
  }

  public String parseToText(InputStream is, Metadata md) throws Exception {
    AutoDetectParser parser = new AutoDetectParser();
    BodyContentHandler handler = new BodyContentHandler(-1);
    ParseContext ctx = new ParseContext();

    PDFParserConfig pdfCfg = new PDFParserConfig();
    pdfCfg.setExtractInlineImages(true);
    pdfCfg.setOcrDPI(300);
    try {
      PDFParserConfig.OCR_STRATEGY strategy = PDFParserConfig.OCR_STRATEGY.AUTO;
      pdfCfg.setOcrStrategy(strategy);
    } catch (Throwable ignored) {}
    ctx.set(PDFParserConfig.class, pdfCfg);

    if (ocrEnabled) {
      TesseractOCRConfig tess = new TesseractOCRConfig();
      tess.setLanguage(ocrLanguages);
      tess.setTimeoutSeconds(120);
      try { tess.setEnableImagePreprocessing(true); } catch (Throwable ignored) {}
      ctx.set(TesseractOCRConfig.class, tess);
    }

    parser.parse(is, handler, md, ctx);
    String text = handler.toString();
    if (text == null) return "";
    text = text.replaceAll("\\u0000", "").trim();
    if (text.length() > maxTextLength) {
      text = text.substring(0, maxTextLength);
    }
    return text;
  }
}
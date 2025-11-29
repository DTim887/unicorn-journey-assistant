package com.unicorn.journey.assistant.fileparse.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FileParseResult {
  private String id;
  private String filename;
  private String mimeType;
  private long size;
  private String text;
  private Map<String, Object> metadata;
  private List<String> warnings;

}
package com.unicorn.journey.assistant.fileparse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ParseResponse {
  private List<FileParseResult> files;

}
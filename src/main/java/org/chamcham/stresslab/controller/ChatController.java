package org.chamcham.stresslab.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.chamcham.stresslab.dto.ChatMessageDto;
import org.chamcham.stresslab.handler.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

  private final ChatWebSocketHandler handler;

  @GetMapping("/health")
  public String health() {
    return "ok";
  }

  @GetMapping("/messages/recent")
  public List<ChatMessageDto> recent() {
    return handler.getRecentMessages();
  }
}

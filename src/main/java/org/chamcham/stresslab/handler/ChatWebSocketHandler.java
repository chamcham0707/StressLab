package org.chamcham.stresslab.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.chamcham.stresslab.dto.ChatMessageDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

  private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final LinkedList<ChatMessageDto> recentMessages = new LinkedList<>();
  private final int MAX_HISTORY = 500;

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.add(session);
    sendHistory(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    sessions.remove(session);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    String payload = message.getPayload();

    ChatMessageDto chat;
    try {
      chat = objectMapper.readValue(payload, ChatMessageDto.class);
    } catch (Exception e) {
      session.sendMessage(new TextMessage("{\"error\":\"invalid message format\"}"));
      return;
    }

    chat.setTimestamp(Instant.now().toEpochMilli());

    synchronized (recentMessages) {
      recentMessages.addLast(chat);
      if (recentMessages.size() > MAX_HISTORY) {
        recentMessages.removeFirst();
      }
    }

    String json = objectMapper.writeValueAsString(chat);
    TextMessage out = new TextMessage(json);
    for (WebSocketSession s : sessions) {
      if (s.isOpen()) {
        try {
          s.sendMessage(out);
        } catch (Exception ignored) {
          // TODO: 개별 세션 전송 실패 시, 무시하고 계속
        }
      }
    }
  }

  private void sendHistory(WebSocketSession session) {
    try {
      synchronized (recentMessages) {
        for (ChatMessageDto m : recentMessages) {
          session.sendMessage(new TextMessage(objectMapper.writeValueAsString(m)));
        }
      }
    } catch (Exception ignored) {}
  }

  public List<ChatMessageDto> getRecentMessages() {
    synchronized (recentMessages) {
      return List.copyOf(recentMessages);
    }
  }
}

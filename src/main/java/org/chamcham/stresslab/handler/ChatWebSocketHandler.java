package org.chamcham.stresslab.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.chamcham.stresslab.dto.ChatMessageDto;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession; import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.w3c.dom.Text;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

  private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  private final List<ChatMessageDto> recentMessages = new CopyOnWriteArrayList<>();

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule());

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    sessions.add(session);
    sendHistory(session);
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessions.remove(session);

    log.info("Session removed: {}, total sessions = {}", session.getId(), sessions.size());
    log.info("Session closed: {}, code={}, reason={}",
        session.getId(),
        status.getCode(),
        status.getReason());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

    ChatMessageDto chatMessageDto = objectMapper.readValue(message.getPayload(), ChatMessageDto.class);

    chatMessageDto = new ChatMessageDto(
        chatMessageDto.getSender(),
        chatMessageDto.getContent(),
        LocalDateTime.now()
    );

    recentMessages.add(chatMessageDto);

    TextMessage broadcast = new TextMessage(objectMapper.writeValueAsString(chatMessageDto));
    for (WebSocketSession s : sessions) {
      safeSend(s, broadcast);
    }
  }

  private void sendHistory(WebSocketSession session) throws IOException {
    for (ChatMessageDto m : recentMessages) {
      safeSend(session, new TextMessage(objectMapper.writeValueAsString(m)));
    }
  }

  private void safeSend(WebSocketSession session, TextMessage message) {
    synchronized (session) {
      try {
        if (session.isOpen()) {
          session.sendMessage(message);
        }
      } catch (IllegalStateException e) {
        log.warn("Attempted to send message to closed session: {}", session.getId());
      } catch (IOException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("Broken pipe") || msg.contains("Connection reset")) {
          log.warn("Client disconnected during send (Broken pipe) - ignored ({})", msg);
        } else {
          log.error("Unexpected error while sending message.", e);
        }
      }
    }
  }
}

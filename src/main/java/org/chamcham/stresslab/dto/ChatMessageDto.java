package org.chamcham.stresslab.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
  private String sender;
  private String content;
  private LocalDateTime timestamp;
}

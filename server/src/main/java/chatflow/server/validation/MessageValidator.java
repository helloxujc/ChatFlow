package chatflow.server.validation;

import chatflow.server.model.ChatMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class MessageValidator {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,20}$");

  private MessageValidator() {}

  public static List<String> validate(ChatMessage chatMessage) {
    List<String> errors = new ArrayList<>();

    if (chatMessage == null) {
      errors.add("ChatMessage is null");
      return errors;
    }

    validateUserId(chatMessage.getUserId(), errors);
    validateUsername(chatMessage.getUsername(), errors);
    validateMessage(chatMessage.getMessage(), errors);
    validateTimeStamp(chatMessage.getTimestamp(), errors);
    validateMessageType(chatMessage.getMessageType(), errors);

    return errors;
  }

  private static void validateUserId(String userId, List<String> errors) {
    if (userId == null || userId.isBlank()) {
      errors.add("UserID is null or empty");
      return;
    }

    try {
      int id = Integer.parseInt(userId);
      if (id < 1 || id > 100000) {
        errors.add("UserID must be between 1 and 100000");
      }
    } catch (NumberFormatException e) {
      errors.add("UserID must be an integer");
    }
  }

  private static void validateUsername(String username, List<String> errors) {
    if (username == null || username.isBlank()) {
      errors.add("Username is null or empty");
      return;
    }
    if (!USERNAME_PATTERN.matcher(username).matches()) {
      errors.add("Username must be 3-20 alphanumeric characters");
    }
  }

  private static void validateMessage(String messageContent, List<String> errors) {
    if (messageContent == null || messageContent.isBlank()) {
      errors.add("ChatMessage content is null or empty");
      return;
    }

    int length = messageContent.length();
    if (length < 1 || length > 500) {
      errors.add("ChatMessage content must 1-500 characters");
    }
  }

  private static void validateTimeStamp(String timestamp, List<String> errors) {
    if (timestamp == null || timestamp.isBlank()) {
      errors.add("Timestamp is null or empty");
      return;
    }

    try {
      Instant.parse(timestamp);
    } catch (Exception e) {
      errors.add("Invalid timestamp");
    }
  }

  private static void validateMessageType(String messageType, List<String> errors) {
    if (messageType == null || messageType.isBlank()) {
      errors.add("ChatMessage type is null or empty");
      return;
    }

    List<String> validMessageTypes = new ArrayList<>();
    validMessageTypes.add("TEXT");
    validMessageTypes.add("JOIN");
    validMessageTypes.add("LEAVE");
    if (!validMessageTypes.contains(messageType)) {
      errors.add("MessageType must be one of " + validMessageTypes);
    }
  }
}

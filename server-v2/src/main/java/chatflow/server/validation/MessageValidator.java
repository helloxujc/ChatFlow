package chatflow.server.validation;

import chatflow.server.model.ChatMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates incoming {@link ChatMessage} instances against required formatting and constraints.
 *
 * <p>This validator checks user ID range, username format, message length, ISO-8601 timestamp parsing,
 * and supported message types (TEXT, JOIN, LEAVE). It returns a list of human-readable error messages.
 */
public final class MessageValidator {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9]{3,20}$");

  private MessageValidator() {}

  /**
   * Validates the given chat message and returns a list of validation errors.
   *
   * @param chatMessage the message to validate
   * @return a list of error messages; empty if valid
   */
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

  /**
   * Validates the userId field.
   *
   * @param userId the user ID as a string
   * @param errors the error list to append to
   */
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

  /**
   * Validates the username field.
   *
   * @param username the username to validate
   * @param errors the error list to append to
   */
  private static void validateUsername(String username, List<String> errors) {
    if (username == null || username.isBlank()) {
      errors.add("Username is null or empty");
      return;
    }
    if (!USERNAME_PATTERN.matcher(username).matches()) {
      errors.add("Username must be 3-20 alphanumeric characters");
    }
  }

  /**
   * Validates the message content field.
   *
   * @param messageContent the message content
   * @param errors the error list to append to
   */
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

  /**
   * Validates the timestamp field as an ISO-8601 instant.
   *
   * @param timestamp the timestamp string
   * @param errors the error list to append to
   */
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

  /**
   * Validates the message type field.
   *
   * @param messageType the message type string
   * @param errors the error list to append to
   */
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

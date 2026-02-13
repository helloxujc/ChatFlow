package chatflow.server.model;

/**
 * Represents a response sent by the WebSocket server to the client.
 *
 * <p>This response includes a status field, server-generated timestamp,
 * optional response data, and optional error details.
 */
public class ChatResponse {
  private String status;
  private String serverTimestamp;
  private Object data;
  private Object errors;

  /**
   * Default constructor for serialization and deserialization.
   */
  public ChatResponse() {
  }

  /**
   * Returns the response status (e.g., OK or ERROR).
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the response status.
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Returns the server-generated timestamp.
   */
  public String getServerTimestamp() {
    return serverTimestamp;
  }

  /**
   * Sets the server-generated timestamp.
   */
  public void setServerTimestamp(String serverTimestamp) {
    this.serverTimestamp = serverTimestamp;
  }

  /**
   * Returns the response data.
   */
  public Object getData() {
    return data;
  }

  /**
   * Sets the response data.
   */
  public void setData(Object data) {
    this.data = data;
  }

  /**
   * Returns the error details if the request failed.
   */
  public Object getErrors() {
    return errors;
  }

  /**
   * Sets the error details.
   */
  public void setErrors(Object errors) {
    this.errors = errors;
  }
}

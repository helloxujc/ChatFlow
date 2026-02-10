public interface SendChannel {

  void send(String text) throws Exception;

  boolean isOpen();

  void reconnect() throws Exception;
}

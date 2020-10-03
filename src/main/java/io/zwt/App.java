package io.zwt;

import io.zwt.controller.HomeController;
import io.zwt.domain.DataRecord;
import io.zwt.service.LANTask;
import io.zwt.util.SymmetricEncryption;
import javafx.application.Application;
import javafx.beans.binding.When;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

import static io.zwt.config.Config.*;

public class App extends Application {

  public static volatile String encryptedKey;
  private static boolean isOn = true;
  public static DatagramChannel channel = null;
  private static Button button;
  private ResourceBundle resourceBundle;
  public static String HOME = System.getProperty("user.home");
  LANTask task;

  @Override
  public void init() throws Exception {
    File file = new File(HOME + "/preference.properties");
    if (file.createNewFile()) {
      Properties properties = new Properties();
      properties.setProperty("lamp.status", "false");
      properties.setProperty("lamp.color", "0x008000ff");
      FileWriter fileWriter = new FileWriter(file);
      properties.store(fileWriter, "lamp");
    } else {
      resourceBundle = new PropertyResourceBundle(new FileReader(file));
    }
    resourceBundle = new PropertyResourceBundle(new FileReader(file));

    Selector selector = getSelector();
    App app = new App();
    task = new LANTask(selector, app);
    task.setDaemon(true);
    task.start();
  }

  @Override
  public void start(Stage stage) throws Exception {
    Parent parent = FXMLLoader.load(getClass().getResource("/fxml/home.fxml"), resourceBundle);

    /*Label label = (Label) parent.lookup("#label");

    label.textProperty().bind(new When(task.ipProperty().isNull())
      .then("Receiving data...")
      .otherwise(task.ipProperty()));*/
    Scene scene = new Scene(parent);
    stage.setScene(scene);
    stage.setTitle(APP_TITLE);
    stage.show();
  }

  @Override
  public void stop() throws Exception {
    System.out.println("Saving preference...");
    try {
      Properties properties = new Properties();
      properties.setProperty("lamp.status", String.valueOf(HomeController.status));
      properties.setProperty("lamp.color", HomeController.color);
      FileWriter fileWriter = new FileWriter(new File(HOME + "/preference.properties"));
      properties.store(fileWriter, "lamp");
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }
  }

  private static Selector getSelector() throws IOException {
    NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    Iterator<NetworkInterface> networkInterfaceIterator = networkInterfaces.asIterator();
    while (networkInterfaceIterator.hasNext()) {
      NetworkInterface next = networkInterfaceIterator.next();
      Optional<InetAddress> first = next.inetAddresses()
        .filter(inetAddress -> inetAddress.getHostAddress().contains("192.168.1."))
        .findAny();
      if (first.isPresent()) {
        ni = next;
        break;
      }
    }
    InetAddress multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
    channel = DatagramChannel.open(StandardProtocolFamily.INET)
      .setOption(StandardSocketOptions.SO_REUSEADDR, true)
      .bind(new InetSocketAddress(PORT))
      .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);

    channel.join(multicastAddress, ni);
    channel.configureBlocking(false);
    Selector selector = Selector.open();
    channel.register(selector, SelectionKey.OP_READ, new DataRecord());
    return selector;
  }

  public static void updateRGB(final int cmd, final int colorValue, final int light) throws IOException {
    String writeRGBData;
    if (cmd == -1) {
      writeRGBData = NEW_RGB_CMD_HEAD + ((light << 24) | colorValue) + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
    } else {
      writeRGBData = NEW_RGB_CMD_HEAD + ((light << 24) | colorValue) + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
    }
    ByteBuffer to = ByteBuffer.wrap(writeRGBData.getBytes());
    if (encryptedKey != null) {
      channel.send(to, GATEWAY);
      System.out.println(writeRGBData);
    }
  }

  public static void sendWhois(String cmd) throws IOException {
    ByteBuffer to = ByteBuffer.wrap(cmd.getBytes());
    channel.send(to, GATEWAY_UDP);
    System.out.println(cmd);
  }

  public static void sendWhatever(String cmd) throws IOException {
    ByteBuffer to = ByteBuffer.wrap(cmd.getBytes());
    channel.send(to, GATEWAY);
    System.out.println(cmd);
  }

  public static void togglePlug() throws IOException {
    isOn = !isOn;
    String writePlugData = WRITE_PLUG + (isOn ? OFF : ON) + KEY_JSON_ATTR + encryptedKey + CMD_TRAILER;
    ByteBuffer to = ByteBuffer.wrap(writePlugData.getBytes());
    System.out.println(writePlugData);
    if (encryptedKey != null) {
      channel.send(to, GATEWAY);
    }
  }

  public String onReceiveData(ByteBuffer receivedBytes) throws Exception {
    receivedBytes.flip();
    byte[] bytes = new byte[receivedBytes.limit()];
    while (receivedBytes.hasRemaining()) {
      bytes[receivedBytes.position()] = receivedBytes.get();
    }
    String receivedString = new String(bytes);
    // find token and encrypt it
    if (receivedString.contains("gateway") && receivedString.contains("heartbeat")) {
      int tokenIndex = receivedString.indexOf("token");
      String tokenString = receivedString.substring(tokenIndex + 8, tokenIndex + 24);
      byte[] cipher = SymmetricEncryption.performAESEncryption(tokenString, SECRET_KEY, INITIALIZATION_VECTOR);
      encryptedKey = DatatypeConverter.printHexBinary(cipher);
      //System.out.println(KEY_UPDATED);
      //System.out.println(encryptedKey);
    }
    if (receivedString.contains("Invalid key")) {
      encryptedKey = null;
    }
    System.out.println(receivedString);
    return receivedString;
  }
}

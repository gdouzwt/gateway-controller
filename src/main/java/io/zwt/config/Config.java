package io.zwt.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;

/**
 * 保存一些常量，应该外部化配置，存到 properties 文件
 */

public class Config {
  public static final String APP_TITLE = "米家多功能网关控制面板";

  public static final String SECRET = "YOUR_SECRET_HERE";
  public static final SecretKey SECRET_KEY = new SecretKeySpec(SECRET.getBytes(), 0, 16, "AES");
  public static final byte[] INITIALIZATION_VECTOR = {0x17, (byte) 0x99, 0x6d, 0x09, 0x3d, 0x28, (byte) 0xdd, (byte) 0xb3, (byte) 0xba, 0x69, 0x5a, 0x2e, 0x6f, 0x58, 0x56, 0x2e};
  public static final String GATEWAY_SID = "7811dcf981c4";
  public static final String PLUG_SID = "158d000234727c";
  public static final int UNICAST_PORT = 9898;
  public static final int MULTICAST_PORT = 4321;
  public static final String GATEWAY_ADDRESS = "192.168.1.145";
  public static final String MULTICAST_ADDRESS = "224.0.0.50";
  public static final InetSocketAddress GATEWAY_UNICAST_ADDRESS = new InetSocketAddress(GATEWAY_ADDRESS, UNICAST_PORT);
  public static final InetSocketAddress LOCAL_MULTICAST_ADDRESS = new InetSocketAddress(MULTICAST_ADDRESS, MULTICAST_PORT);
}

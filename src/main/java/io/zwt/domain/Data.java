package io.zwt.domain;

import java.io.Serializable;

public class Data implements Serializable {
  private String ip;

  public Data(String ipString) {
    this.ip = ipString;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  @Override
  public String toString() {
    return "Data{" +
      "ip='" + ip + '\'' +
      '}';
  }
}

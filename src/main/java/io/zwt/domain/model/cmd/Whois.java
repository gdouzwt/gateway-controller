package io.zwt.domain.model.cmd;

import java.io.Serializable;

public class Whois implements Serializable {
  private final String cmd;

  public String getCmd() {
    return cmd;
  }

  public Whois() {
    this.cmd = "whois";
  }
}
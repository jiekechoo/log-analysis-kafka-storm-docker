package com.log.kafka.other;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class TestXMPP {

  public static void main(String args[]) throws Exception {

    ConnectionConfiguration config = new ConnectionConfiguration("192.168.1.231");
    XMPPConnection xmppConnection = new XMPPConnection(config);
    try {
      xmppConnection.connect();
      xmppConnection.login("storm", "storm");
      Message msg = new Message("alarm@sectong.com", Message.Type.normal);
      msg.setBody("Test Message");
      xmppConnection.sendPacket(msg);
      xmppConnection.disconnect();
    } catch (XMPPException e) {
      e.printStackTrace();
    }
  }
}


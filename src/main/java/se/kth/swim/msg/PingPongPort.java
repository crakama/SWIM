package se.kth.swim.msg;

import se.sics.kompics.PortType;

public class PingPongPort extends PortType { {
    request(Ping.class);
    indication(Pong.class);
}}

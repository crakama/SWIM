package se.kth.swim.msg;

import se.sics.kompics.KompicsEvent;

import java.util.*;

public class Pong implements KompicsEvent {
    UUID pongTimeoutId;
    private PingPongType pingPongType;
    private Map<Integer, Status> peers = new TreeMap<>();

    public Pong(PingPongType pingpong, Map<Integer, Status> localState, UUID pongTimeoutId){
        this.peers.putAll(localState);
        this.pongTimeoutId = pongTimeoutId;
        this.pingPongType = pingpong;
    }
    public Map<Integer, Status> getViewUpdate() {
        return peers;
    }
    public UUID getPongTimeoutId(){
        return pongTimeoutId;
    }

    public PingPongType getPingPongType() {
        return pingPongType;
    }
}

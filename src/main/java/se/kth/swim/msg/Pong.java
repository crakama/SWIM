package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

import java.util.*;

public class Pong implements KompicsEvent {
    UUID pongTimeoutId;
    //private List<NatedAddress> peers = new ArrayList<>();
    private Map<Integer, Status> peers = new TreeMap<>();

    public Pong(Map<Integer,Status> localState, UUID pingTimeoutId){
        this.peers.putAll(localState);
        pongTimeoutId = pingTimeoutId;
    }
    public Map<Integer, Status> getViewUpdate() {
        return peers;
    }
    public UUID getPongTimeoutId(){
        return pongTimeoutId;
    }
}

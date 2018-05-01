package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pong implements KompicsEvent {
    UUID pongTimeoutId;
    private List<NatedAddress> peers = new ArrayList<>();
    public Pong(List<NatedAddress> localState, UUID pingTimeoutId){
        this.peers.addAll(localState);
        pongTimeoutId = pingTimeoutId;
    }
    public List<NatedAddress> getPeers() {
        return peers;
    }
    public UUID getPongTimeoutId(){
        return pongTimeoutId;
    }
}

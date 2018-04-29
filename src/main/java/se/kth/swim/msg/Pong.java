package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;
import java.util.List;

public class Pong implements KompicsEvent {
    private List<NatedAddress> peers = new ArrayList<>();
    public Pong(List<NatedAddress> localState){
        this.peers.addAll(localState);

    }
    public List<NatedAddress> getPeers() {
        return peers;
    }
}

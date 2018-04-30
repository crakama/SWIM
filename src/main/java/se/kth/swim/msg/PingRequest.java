package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class PingRequest implements KompicsEvent {
    private NatedAddress peerToPing;
    public PingRequest(NatedAddress pingReq){
        this.peerToPing=pingReq;
    }
    public NatedAddress getPeer() {
        return peerToPing;
    }
}
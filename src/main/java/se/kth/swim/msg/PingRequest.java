package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

import java.util.UUID;

public class PingRequest implements KompicsEvent {
    private NatedAddress peerToPing;
    private UUID pingSuspectRequesttId;
    public PingRequest(NatedAddress pingReq, UUID pingSuspectRequesttId){
        this.peerToPing = pingReq;
        this.pingSuspectRequesttId = pingSuspectRequesttId;
    }
    public NatedAddress getPeerToPing() {
        return peerToPing;
    }

    public UUID getPingSuspectRequesttId() {
        return pingSuspectRequesttId;
    }
}
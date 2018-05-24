package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class Suspect implements KompicsEvent {
    private NatedAddress peerDeclaredDead;
    public Suspect(NatedAddress peer){
        this.peerDeclaredDead=peer;
    }
    public NatedAddress getSuspectedPeer() {
        return peerDeclaredDead;
    }
}
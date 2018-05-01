package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class DeclareDead implements KompicsEvent {
    private NatedAddress peerDeclaredDead;
    public DeclareDead(NatedAddress peer){
        this.peerDeclaredDead=peer;
    }
    public NatedAddress getPeerDeclaredDead() {
        return peerDeclaredDead;
    }
}

package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class Dead implements KompicsEvent {
    private NatedAddress peerDeclaredDead;
    public Dead(NatedAddress peer){
        this.peerDeclaredDead=peer;
    }
    public NatedAddress getDeadPeer() {
        return peerDeclaredDead;
    }
}

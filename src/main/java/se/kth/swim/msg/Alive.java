package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class Alive implements KompicsEvent {
    private NatedAddress peerDeclaredDead;
    public Alive(NatedAddress peer){
        this.peerDeclaredDead=peer;
    }
    public NatedAddress getAlivePeer() {
        return peerDeclaredDead;
    }
}
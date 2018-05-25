package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.KompicsEvent;

public class Suspect implements KompicsEvent {
    private NatedAddress suspect;
    public Suspect(NatedAddress peer){
        this.suspect=peer;
    }
    public NatedAddress getSuspectedPeer() {
        return suspect;
    }
}
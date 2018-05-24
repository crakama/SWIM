package se.kth.swim.msg.net;

import se.kth.swim.msg.Suspect;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

public class NetSuspect extends BasicContentMsg<Suspect> {
    public NetSuspect(NatedAddress src, NatedAddress dst, NatedAddress peer) {
        super(src, dst,new Suspect(peer));
    }

    private NetSuspect(Header<NatedAddress> header, Suspect content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetSuspect(newHeader, getContent());
    }
}

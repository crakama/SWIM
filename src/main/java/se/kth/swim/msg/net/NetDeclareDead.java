package se.kth.swim.msg.net;

import se.kth.swim.msg.DeclareDead;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

public class NetDeclareDead extends BasicContentMsg<DeclareDead>{
    public NetDeclareDead(NatedAddress src, NatedAddress dst, NatedAddress peer) {
        super(src, dst,new DeclareDead(peer));
    }

    private NetDeclareDead(Header<NatedAddress> header, DeclareDead content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetDeclareDead(newHeader, getContent());
    }
}

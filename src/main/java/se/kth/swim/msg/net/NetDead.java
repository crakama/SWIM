package se.kth.swim.msg.net;

import se.kth.swim.msg.Dead;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

public class NetDead extends BasicContentMsg<Dead>{
    public NetDead(NatedAddress src, NatedAddress dst, NatedAddress peer) {
        super(src, dst,new Dead(peer));
    }

    private NetDead(Header<NatedAddress> header, Dead deadNodeContent) {
        super(header, deadNodeContent);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetDead(newHeader, getContent());
    }
}

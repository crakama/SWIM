package se.kth.swim.msg.net;

import se.kth.swim.msg.Gossip;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

public class NetGossip extends BasicContentMsg<Gossip> {
    public NetGossip(NatedAddress src, NatedAddress dst) {
        super(src, dst, new Gossip());
    }

    private NetGossip(Header<NatedAddress> header, Gossip content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetGossip(newHeader, getContent());
    }
}

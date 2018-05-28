package se.kth.swim.msg.net;

import se.kth.swim.msg.Alive;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

public class NetAlive extends BasicContentMsg<Alive> {
    public NetAlive(NatedAddress src, NatedAddress dst, NatedAddress peer) {
        super(src, dst,new Alive(peer));
    }

    private NetAlive(Header<NatedAddress> header, Alive aliveNodeContent) {
        super(header, aliveNodeContent);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetAlive(newHeader, getContent());
    }
}
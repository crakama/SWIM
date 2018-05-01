package se.kth.swim.msg.net;

import se.kth.swim.msg.Pong;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

import java.util.List;
import java.util.UUID;

public class NetPong extends BasicContentMsg<Pong>{
    public NetPong(NatedAddress src, NatedAddress dst, UUID pingTimeoutId,  List<NatedAddress> localState) {
        super(src, dst, new Pong(localState,pingTimeoutId));
    }

    private NetPong(Header<NatedAddress> header, Pong content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetPong(newHeader, getContent());
    }
}

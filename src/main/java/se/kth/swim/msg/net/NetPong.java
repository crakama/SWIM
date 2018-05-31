package se.kth.swim.msg.net;

import se.kth.swim.msg.Pong;
import se.kth.swim.msg.PingPongType;
import se.kth.swim.msg.Status;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

import java.util.Map;
import java.util.UUID;

public class NetPong extends BasicContentMsg<Pong>{
    public NetPong(NatedAddress src, NatedAddress dst, PingPongType pingpong, UUID pongTimeoutId, Map<Integer, Status> localState) {
        super(src, dst, new Pong(pingpong,localState,pongTimeoutId));
    }

    private NetPong(Header<NatedAddress> header, Pong content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetPong(newHeader, getContent());
    }
}

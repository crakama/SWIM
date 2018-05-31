package se.kth.swim.msg.net;

import se.kth.swim.msg.PingRequest;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

import java.util.UUID;

//
public class NetPingRequest extends BasicContentMsg<PingRequest> {
    public NetPingRequest(NatedAddress src, NatedAddress dst, NatedAddress pingReq, UUID pingRequesttId) {
        super(src, dst,new PingRequest(pingReq,pingRequesttId));
    }

    private NetPingRequest(Header<NatedAddress> header, PingRequest content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader) {
        return new NetPingRequest(newHeader, getContent());
    }
}

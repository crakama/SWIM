/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.swim.msg.net;

import se.kth.swim.msg.Ping;
import se.kth.swim.msg.Status;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicContentMsg;
import se.sics.kompics.network.Header;

import java.util.Map;
import java.util.UUID;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 *
 *  new Ping(pingTimeoutId), PING Bbject wrapped with UUID parameter, which can be accessd by other components that want to
 *  use it and have registered for PING events.
 *
 */
public class NetPing extends BasicContentMsg<Ping> {

    public NetPing(NatedAddress src, NatedAddress dst, UUID pingTimeoutId, Map<Integer, Status> localStateNodes) {
        super(src, dst, new Ping(pingTimeoutId,localStateNodes));
    }

    private NetPing(Header<NatedAddress> header, Ping content) {
        super(header, content);
    }

    @Override
    public BasicContentMsg newHeader(Header<NatedAddress> newHeader)
    {
        return new NetPing(newHeader, getContent());
    }

}

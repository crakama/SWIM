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
package se.kth.swim;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPong;
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SwimComp extends ComponentDefinition {

    private static final Logger log = LoggerFactory.getLogger(SwimComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
    private final List<NatedAddress> bootstrapNodes;
    private TreeMap<Integer, Address> localState = new TreeMap<>();

    private UUID pingTimeoutId;

    private int receivedPings = 0;

    public SwimComp(SwimInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(handlePingTimeout, timer);
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress.getId()});

            if (!bootstrapNodes.isEmpty()) {
                schedulePeriodicPing();
            }
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress.getId()});
            if (pingTimeoutId != null) {
                cancelPeriodicPing();
            }
        }

    };

    private Handler<NetPing> handlePing = new Handler<NetPing>() {

        @Override
        public void handle(NetPing event) {
            NatedAddress newlyJoinedPeer = event.getHeader().getSource();
            log.info("{} received ping from:{}", new Object[]{selfAddress.getId(),newlyJoinedPeer});
            receivedPings++;
            //TODO: select subset of nodes to be provided by bootstrap server and send pong with info about new peer that has sent a ping & my local state.

            updateLocalState(newlyJoinedPeer.getId(),newlyJoinedPeer);

            List<NatedAddress> randomPeers = selectPeers(bootstrapNodes,2);
            for(NatedAddress peer : randomPeers){
                trigger(new NetPong(selfAddress,peer), network);
                log.info("Peer {} has sent a PONG to peer :{}", new Object[]{selfAddress, peer});
            }
        }

    };

    private void updateLocalState(Integer peerID, NatedAddress newlyJoinedPeer) {
        log.info("Peer {} received PING from newly joined Peer :{} of ID:{}", new Object[]{selfAddress, newlyJoinedPeer,peerID});
        localState.put(peerID,newlyJoinedPeer);
    }

    public static <NateAddress> List<NateAddress> selectRandomPeers(List<NateAddress> list, int n, Random r) {
        int length = list.size();

        if (length < n) return null;

        //No shuffle
        for (int i = length - 1; i >= length - n; --i)
        {
            Collections.swap(list, i , r.nextInt(i + 1));
        }
        return list.subList(length - n, length);
    }

    public static <NateAddress> List<NateAddress> selectPeers(List<NateAddress> list, int n) {
        List<NateAddress> peerAddresses = selectRandomPeers(list, n, ThreadLocalRandom.current());
        log.info("{} number of peers Randomly Selected :{}", new Object[]{peerAddresses.stream().count(), peerAddresses});
        return peerAddresses;

    }


    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {

        @Override
        public void handle(PingTimeout event) {
            for (NatedAddress peerNodeAddress : bootstrapNodes) {
                log.info("{} sending ping to PeerNode:{}", new Object[]{selfAddress.getId(), peerNodeAddress});
                trigger(new NetPing(selfAddress, peerNodeAddress), network);
            }
        }

    };


    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(1000, 1000);
        PingTimeout sc = new PingTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }

    private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(pingTimeoutId);
        trigger(cpt, timer);
        pingTimeoutId = null;
    }

    public static class SwimInit extends Init<SwimComp> {

        public final NatedAddress selfAddress;
        public final List<NatedAddress> bootstrapNodes;

        public SwimInit(NatedAddress selfAddress, List<NatedAddress> bootstrapNodes) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
        }
    }

    private static class PingTimeout extends Timeout {

        public PingTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }
}

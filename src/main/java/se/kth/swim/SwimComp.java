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
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.msg.net.NetPingRequest;
import se.kth.swim.msg.net.NetPing;
import se.kth.swim.msg.net.NetPong;
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.*;
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
    private Map<Integer, NatedAddress> localState = new TreeMap<>();
    //private List<NatedAddress> nonPingablePeers = new ArrayList<>();
    private Map<Integer, NatedAddress> acknowledgements = new TreeMap<Integer, NatedAddress>();
    private UUID pingTimeoutId;
    private UUID pongTimeoutId;

    private int receivedPings = 0;

    public SwimComp(SwimInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(pongHandler, network);
        subscribe(pingRequestHandler, network);
        subscribe(handlePingTimeout, timer);
        subscribe(pongTimeoutHandler, timer);
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
            NatedAddress pingerPeer = event.getHeader().getSource();
            receivedPings++;
            trigger(new NetPong(selfAddress,pingerPeer,event.getContent().getPingTimeoutId(),getLocalState(localState)),network);
        }
    };

    //TODO: Schedule PONG and GOSSIP events
    private Handler<NetPong> pongHandler = new Handler<NetPong>() {
        @Override
        public void handle(NetPong netPongEvent) {
            if( netPongEvent.getContent().getPongTimeoutId() != null )
                cancelMsgRTTTimeout(netPongEvent.getContent().getPongTimeoutId());
            updateLocalState(netPongEvent.getSource().getId(),netPongEvent.getSource());
            log.info("Peer {} Received PONG from Peer :{}", new Object[]{selfAddress.getId(), netPongEvent.getSource()});
        }
    };
    private Handler<NetPingRequest> pingRequestHandler = new Handler<NetPingRequest>() {
        @Override
        public void handle(NetPingRequest netPingRequest) {
            log.info("Peer {} received Request from Peer :{} to PINGREQUEST Peer{}",
                    new Object[]{selfAddress.getId(), netPingRequest.getSource(),netPingRequest.getContent().getPeer()});
            trigger(new NetPing(selfAddress,netPingRequest.getContent().getPeer(), pingTimeoutId),network);
        }
    };
    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {
        @Override
        public void handle(PingTimeout event) {
            List<NatedAddress> randomPeers = selectPeers(bootstrapNodes, 1);
            for (NatedAddress randomPeer : randomPeers) {
                log.info("Peer {} sending PING to Random Peer :{}", new Object[]{selfAddress.getId(), randomPeer});
                pingTimeoutId = event.getTimeoutId();
                trigger(new NetPing(selfAddress, randomPeer,pingTimeoutId), network);
                scheduleMsgRTTTimeout(randomPeer);
            }
        }
    };
    private Handler<PongTimeout> pongTimeoutHandler = new Handler<PongTimeout>() {
        @Override
        public void handle(PongTimeout event) {
            List<NatedAddress> localview = getLocalState(localState);
            log.info("Peer {} received PongTimeout for Peer {} localview: {} localstate:{}",
                    new Object[]{selfAddress.getId(),event.getPeerSuspect(),localview, localState});
            List<NatedAddress> randomPeers = selectPeers(localview,3);
            if(randomPeers != null){
                UUID timeoutId = event.getTimeoutId();
                for(NatedAddress randompeer: randomPeers){
                    trigger(new NetPingRequest(selfAddress,randompeer,event.getPeerSuspect()),network);
                }

            }
        }
    };

    //TODO: If the pinger is the ramdom list, then send it to all otherwise add pinger then send message
    private void updateLocalState(Integer peerID, NatedAddress newlyJoinedPeer) {
        this.localState.put(peerID,newlyJoinedPeer);
    }
    //********************************** Select Random Peers **************************************************//

    public static List<NatedAddress> selectRandomPeers(List<NatedAddress> peerlist, int nrofRequiredNodes, Random r) {
        int peerlistLen= peerlist.size();
        if (peerlistLen < nrofRequiredNodes) return null;//TODO: Handle Null Pointer exception where this method is triggered
        for (int i = peerlistLen - 1; i >= peerlistLen - nrofRequiredNodes; --i)
        { Collections.swap(peerlist, i , r.nextInt(i + 1)); }
        return peerlist.subList(peerlistLen - nrofRequiredNodes, peerlistLen);
    }
    public static List<NatedAddress> selectPeers(List<NatedAddress> listofpeers, int nrofRequiredNodes) {
        List<NatedAddress> peerAddresses = selectRandomPeers(listofpeers, nrofRequiredNodes, ThreadLocalRandom.current());
        return peerAddresses;
    }


    private List<NatedAddress> getLocalState(Map<Integer, NatedAddress> state){
        List<NatedAddress> localpeerlist = state.values().stream().collect(Collectors.toList());
        return localpeerlist;
    }

    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(3000, 3000);
        PingTimeout sc = new PingTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }
    private void scheduleMsgRTTTimeout(NatedAddress randomPeer) {
        SchedulePeriodicTimeout schedulePeriodicTimeout = new SchedulePeriodicTimeout(15000, 15000);//15 seconds
        PongTimeout sc = new PongTimeout(schedulePeriodicTimeout,randomPeer);
        schedulePeriodicTimeout.setTimeoutEvent(sc);
        //pongTimeoutId = sc.getTimeoutId();
        trigger(schedulePeriodicTimeout, timer);
    }

    private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(pingTimeoutId);
        trigger(cpt, timer);
        pingTimeoutId = null;
    }

    private void cancelMsgRTTTimeout(UUID pongTimeout) {
        CancelTimeout cpt = new CancelTimeout(pongTimeout);
        trigger(cpt, timer);
        //pongTimeoutId = null;
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

    private class PongTimeout extends Timeout{
        private NatedAddress peerSuspect;
        public PongTimeout(SchedulePeriodicTimeout schedulePeriodicTimeout, NatedAddress randomPeer) {
            super(schedulePeriodicTimeout);
            peerSuspect = randomPeer;

        }
        public NatedAddress getPeerSuspect(){
            return peerSuspect;
        }
    }
}

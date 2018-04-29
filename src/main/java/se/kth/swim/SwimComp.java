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
import se.kth.swim.msg.net.NetGossip;
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
    private TreeMap<Integer, NatedAddress> localState = new TreeMap<>();
    //private List<NatedAddress> localState = new ArrayList<>() ;

    private UUID pingTimeoutId;

    private int receivedPings = 0;

    public SwimComp(SwimInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;

        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(pongHandler, network);
        subscribe(gossipHandler, network);
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
            receivedPings++;
            //List<NatedAddress> localview = getLocalState();
            //if(!localview.isEmpty())
            //trigger(new NetPong(selfAddress,newlyJoinedPeer,localview), network);
            updateLocalState(newlyJoinedPeer.getId(),newlyJoinedPeer);
            startGossip(newlyJoinedPeer);
        }
    };

    //TODO: Schedule PONG and GOSSIP events
    private Handler<NetPong> pongHandler = new Handler<NetPong>() {
        @Override
        public void handle(NetPong netPong) {
            log.info("Peer {} received PONG from:{} with message: {}",
                    new Object[]{selfAddress.getId(),netPong.getSource(),netPong.getContent().getPeers()});
            /*            log.info("Peer {} received PONG from:{} with message: {}",
                                new Object[]{selfAddress.getId(),netPong.getSource(),netPong.getContent().getPeers()});*/
        }
    };

    private Handler<NetGossip> gossipHandler = new Handler<NetGossip>() {
        @Override
        public void handle(NetGossip netGossip) {
            log.info("Peer {} received Gossip from:{}", new Object[]{selfAddress.getId(),netGossip.getSource()});
            startGossip(netGossip.getSource());
        }
    };

    private void startGossip(NatedAddress newlyJoinedPeer) {
        List<NatedAddress> randomPeers = null;
        List<NatedAddress> state = getLocalState();
        int nodes = Math.toIntExact(state.stream().count());
        if(nodes > 3){
            randomPeers = selectPeers(state, 3);
            //randomPeers  = selectPeers(getLocalState(), nodes);
            log.info("Peer {} Has Randomly Selected Peers :{} for Gossip", new Object[]{selfAddress.getId(), randomPeers});
        for (NatedAddress randomPeer : randomPeers) {
            //if(newlyJoinedPeer.getId() !=  randomPeer.getId()){
                trigger(new NetPong(selfAddress,randomPeer,state), network);
                //trigger(new NetGossip(selfAddress, randomPeer), network);
            //}
        }
        }
    }

    private void updateLocalState(Integer peerID, NatedAddress newlyJoinedPeer) {
        log.info("Peer {} received PING from newly joined Peer :{} of ID:{}",
                new Object[]{selfAddress, newlyJoinedPeer,peerID});
        this.localState.put(peerID,newlyJoinedPeer);
    }

    public static <NateAddress> List<NateAddress> selectRandomPeers(List<NateAddress> peerlist, int nrofRequiredNodes, Random r) {
        int peerlistLen= peerlist.size();

        if (peerlistLen < nrofRequiredNodes) return null;

        //No shuffle
        for (int i = peerlistLen - 1; i >= peerlistLen - nrofRequiredNodes; --i)
        {
            Collections.swap(peerlist, i , r.nextInt(i + 1));
        }
        return peerlist.subList(peerlistLen - nrofRequiredNodes, peerlistLen);
    }

    public static <NatedAddress> List<se.kth.swim.nat.NatedAddress> selectPeers(List<se.kth.swim.nat.NatedAddress> listofpeers, int nrofRequiredNodes) {
        List<se.kth.swim.nat.NatedAddress> peerAddresses = selectRandomPeers(listofpeers, nrofRequiredNodes, ThreadLocalRandom.current());
        log.info("{} number of peers Randomly Selected :{}", new Object[]{peerAddresses.stream().count(), peerAddresses});
        return peerAddresses;

    }


    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {

        @Override
        public void handle(PingTimeout event) {
            List<NatedAddress> randomPeers = selectPeers(bootstrapNodes, 1);
            for (NatedAddress randomPeer : randomPeers) {
                log.info("Peer {} sending ping to Random Peer Node:{}", new Object[]{selfAddress.getId(), randomPeer});
                trigger(new NetPing(selfAddress, randomPeer), network);
            }
        }

    };

    private List<NatedAddress> getLocalState(){
        List<NatedAddress> localpeerlist = localState.values().stream().collect(Collectors.toList());
        return localpeerlist;
    }


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

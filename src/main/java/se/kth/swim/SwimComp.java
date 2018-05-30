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
import se.kth.swim.msg.Status;
import se.kth.swim.msg.StatusType;
import se.kth.swim.msg.net.*;
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
    private Map<Integer,Status> localStateNodes = new TreeMap<>();
    private Map<Integer,UUID> suspectedNodes = new TreeMap<>();
    private Map<Integer, Status> updateLocalview = new TreeMap<>();
    private UUID pingTimeoutId;
    private UUID pongTimeoutId;
    private int receivedPings = 0;
    private int incarnationNumber;

    public SwimComp(SwimInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", selfAddress);
        this.bootstrapNodes = init.bootstrapNodes;
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handlePing, network);
        subscribe(pongHandler, network);
        subscribe(suspectTimeoutHandler,timer);
        subscribe(handlePingTimeout, timer);
        subscribe(pongTimeoutHandler, timer);
    }
    public static class SwimInit extends Init<SwimComp> {
        public final NatedAddress selfAddress;
        public final List<NatedAddress> bootstrapNodes;

        public SwimInit(NatedAddress selfAddress, List<NatedAddress> bootstrapNodes) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
        }
    }

    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} At SwimComp Start Handler starting...", new Object[]{selfAddress.getId()});
            incarnationNumber = 0;
            localStateNodes.put(selfAddress.getId(),new Status(StatusType.ALIVE,incarnationNumber,selfAddress,selfAddress));
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
    //-------------------------------------- Event Handlers ---------------------------------------------------------//
    //                                                                                                               //
    //---------------------------------------------------------------------------------------------------------------//

    private Handler<NetPing> handlePing = new Handler<NetPing>() {
        @Override
        public void handle(NetPing netPingEvent) {
            NatedAddress sourcePeer = netPingEvent.getHeader().getSource();
            receivedPings++;
            log.info("Peer {} Received PING to from Peer :{}", new Object[]{selfAddress.getId(), sourcePeer    });
            updateLocalState(netPingEvent.getContent().getViewUpdate());
            trigger(new NetPong(selfAddress,sourcePeer,netPingEvent.getContent().getPingTimeoutId(),localStateNodes),network);
        }
    };
    private Handler<NetPong> pongHandler = new Handler<NetPong>() {
        @Override
        public void handle(NetPong netPongEvent) {
            if( netPongEvent.getContent().getPongTimeoutId() != null )
                cancelPongTimeout(netPongEvent.getContent().getPongTimeoutId(),netPongEvent.getSource());
            updateLocalview.clear();
            updateLocalview.putAll(netPongEvent.getContent().getViewUpdate());
            Status status = updateLocalview.get(netPongEvent.getSource().getId());
            updateLocalState(updateLocalview);
            log.info("Peer {} Received PONG from Peer :{} of Status {} currentlocalview {} originalView {}",
                    new Object[]{selfAddress.getId(),netPongEvent.getSource(), status,  updateLocalview.keySet(),netPongEvent.getContent().getViewUpdate().keySet()  });
//            for (Integer key : localStateNodes.keySet()) {
//                Status st = localStateNodes.get(key);
//                log.info("Peer {} has Status {}",
//                        new Object[]{key, st.getStatusType() });
//            }

        }
    };

    private Handler<PingTimeout> handlePingTimeout = new Handler<PingTimeout>() {
        @Override
        public void handle(PingTimeout event) {
            NatedAddress randomPeer = selectRandomPeer();
            log.info("Peer {} sending PING to Random Peer :{}", new Object[]{selfAddress.getId(), randomPeer    });
            pingTimeoutId = event.getTimeoutId();
            schedulePongTimeout(randomPeer);
            trigger(new NetPing(selfAddress, randomPeer,pingTimeoutId,localStateNodes), network);
        }
    };
    private Handler<PongTimeout> pongTimeoutHandler = new Handler<PongTimeout>() {
        @Override
        public void handle(PongTimeout pongTimeoutEvent) {
            updateLocalview.clear();
            updateLocalview.put(pongTimeoutEvent.getSuspectedPeer().getId(),
                        new Status(StatusType.SUSPECTED,0,pongTimeoutEvent.getSuspectedPeer(),selfAddress));
            Status foreignStatus = updateLocalview.get(pongTimeoutEvent.getSuspectedPeer().getId());
            if( foreignStatus != null )
                log.info("Peer {} received PongTimeout for Peer {} and has Marked it as {}",
                        new Object[]{selfAddress.getId(),pongTimeoutEvent.getSuspectedPeer(),foreignStatus.getStatusType()});
            updateLocalState(updateLocalview);
        }
    };
    //TODO Replace check of (status != null) with
    private Handler<SuspectTimeout> suspectTimeoutHandler = new Handler<SuspectTimeout>() {
        @Override
        public void handle(SuspectTimeout suspectTimeoutEvent) {
            updateLocalview.clear();
            Status localStatus = localStateNodes.get(suspectTimeoutEvent.getDeadPeer().getId());
            if(localStatus != null){

                updateLocalview.put(suspectTimeoutEvent.getDeadPeer().getId(),
                        new Status(StatusType.DEAD,localStatus.getIncarnationNo(),suspectTimeoutEvent.getDeadPeer(),selfAddress));
            }
            Status status = updateLocalview.get(suspectTimeoutEvent.getDeadPeer().getId());
            if( status != null )
                log.info("Peer {} received SuspectTimeout for Peer {} and Marked it as {}",
                        new Object[]{selfAddress.getId(),suspectTimeoutEvent.getDeadPeer(),status.getStatusType()});
            updateLocalState(updateLocalview);
        }
    };
    //Twisted logic -> old value refers to newly received and newValue is one currently stored in localview
    //merging by viewing new list  as old and old/local list is new
    private void updateLocalState(Map<Integer,Status> peers) {
        peers.forEach((key_natAddress, value_status) ->{
            Status st = localStateNodes.get(key_natAddress);
            if(st == null) {
                log.info("Peer {} Received  Peer {} at forEach with FOREIGN status {} LOCAL status {}",
                        new Object[]{selfAddress,key_natAddress, value_status.getStatusType(), "NULL PEER"});
            }else log.info("Peer {} Received  Peer {} at forEach with FOREIGN status {} LOCAL status {}",
                    new Object[]{selfAddress,key_natAddress,value_status.getStatusType(), st.getStatusType()});

                localStateNodes.merge(key_natAddress, value_status, (local, incoming) ->
                        mergeViews(key_natAddress,incoming,local) );});//keep new value
    }
    private Status mergeViews(Integer key_natAddress, Status incoming, Status local) {
        Status newStatusValue;
        log.info("Peer {} received Gossip for Peer {} of incoming {} value_localstatus {}",
                new Object[]{selfAddress.getId(),incoming.getPeer(),incoming.getStatusType(),local.getStatusType()});
       if((key_natAddress.equals(selfAddress.getId()) && (incoming.isSuspected())) ){
            incarnationNumber++;
            newStatusValue = new Status(StatusType.ALIVE,incarnationNumber,selfAddress,selfAddress);
           log.info("incarnationNumber++ ");
           //Local Alive
        }else if( (incoming.isSuspected() && local.isAlive())){
           if(local.getIncarnationNo()  > incoming.getIncarnationNo() ){
               log.info("incoming.isSuspected() && local.isAlive() CHOOSE local  {}",new Object[]{local.getStatusType()} );
               newStatusValue = new Status(local.getStatusType(),local.getIncarnationNo(),local.getPeer(),selfAddress);
           }else{
               log.info("incoming.isSuspected() && local.isAlive() CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
               newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);
           }

        }else if((incoming.isAlive() && local.isAlive()) ){
           if(local.getIncarnationNo()  > incoming.getIncarnationNo() ){
               log.info("incoming.isAlive() && local.isAlive()  CHOOSE local  {}",new Object[]{local.getStatusType()} );
               newStatusValue = new Status(local.getStatusType(),local.getIncarnationNo(),local.getPeer(),selfAddress);
           }else{
               log.info("incoming.isAlive() && local.isAlive()  CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
               newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);
           }
       }
       else if( (incoming.isAlive() && local.isSuspected()) ){
           if(local.getIncarnationNo()  >= incoming.getIncarnationNo() ){
               log.info("incoming.isSuspected() && local.isSuspected() CHOOSE local  {}",new Object[]{local.getStatusType()} );
               newStatusValue = new Status(local.getStatusType(),local.getIncarnationNo(),local.getPeer(),selfAddress);
           }else{
               log.info("incoming.isSuspected() && local.isSuspected() CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
               newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);
           }

        }else if( (incoming.isSuspected() && local.isSuspected())){
           if(local.getIncarnationNo()  > incoming.getIncarnationNo() ){
               log.info("incoming.isSuspected() && local.isSuspected() CHOOSE local  {}",new Object[]{local.getStatusType()} );
               newStatusValue = new Status(local.getStatusType(),local.getIncarnationNo(),local.getPeer(),selfAddress);
           }else{
               log.info("incoming.isSuspected() && local.isSuspected() CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
               newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);
           }

        }else if(incoming.isDead()){
           log.info("incoming.isDead() CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
               newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);

        }else{
           log.info("Last Else CHOOSE Incoming  {}",new Object[]{incoming.getStatusType()} );
           newStatusValue = new Status(incoming.getStatusType(),incoming.getIncarnationNo(),incoming.getPeer(),selfAddress);
       }

        return newStatusValue;
    }

    //-------------------------------------- Random Peer Sampling----------------------------------------------------//
    //                                                                                                               //
    //---------------------------------------------------------------------------------------------------------------//
    public NatedAddress selectRandomPeer() {
        NatedAddress randomPeer;
        List<NatedAddress> shuffledList;
        Random random =new Random();
        while (true){
            shuffledList = select(bootstrapNodes, bootstrapNodes.size());
            randomPeer = shuffledList.get(random.nextInt(shuffledList.size()));
            if(!randomPeer.equals(selfAddress) || randomPeer != null);
            break;
        }
        return randomPeer;
    }

    private static List<NatedAddress> shuffleandSelect(List<NatedAddress> peerlist, int nrofRequiredNodes, Random r) {
        int peerlistLen= peerlist.size();
        if (peerlistLen < nrofRequiredNodes) return null;//TODO: Handle Null Pointer exception where this method is triggered
        for (int i = peerlistLen - 1; i >= peerlistLen - nrofRequiredNodes; --i)
        { Collections.swap(peerlist, i , r.nextInt(i + 1)); }
        return peerlist.subList(peerlistLen - nrofRequiredNodes, peerlistLen);
    }
    public static List<NatedAddress> select(List<NatedAddress> listofpeers, int nrofRequiredNodes) {
        List<NatedAddress> peerAddresses = shuffleandSelect(listofpeers, nrofRequiredNodes, ThreadLocalRandom.current());
        return peerAddresses;
    }


    private List<Status> getLocalState(Map<Integer, Status> state){
        List<Status> localpeerlist = state.values().stream().collect(Collectors.toList());
        return localpeerlist;
    }
    //-------------------------------------- Timeout Schedulers------------------------------------------------------//
    //                                                                                                               //
    //---------------------------------------------------------------------------------------------------------------//
    private void schedulePeriodicPing() {
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(3000, 3000);
        PingTimeout sc = new PingTimeout(spt);
        spt.setTimeoutEvent(sc);
        pingTimeoutId = sc.getTimeoutId();
        trigger(spt, timer);
    }
    private void schedulePongTimeout(NatedAddress randomPeer) {
        ScheduleTimeout scheduleTimeout = new ScheduleTimeout(2000);//2 seconds
        PongTimeout sc = new PongTimeout(scheduleTimeout,randomPeer);
        scheduleTimeout.setTimeoutEvent(sc);
        pongTimeoutId = sc.getTimeoutId();
        trigger(scheduleTimeout, timer);
    }
    private void scheduleSuspectTimeout(NatedAddress suspectedPeer) {
        ScheduleTimeout scheduleTimeout = new ScheduleTimeout(10000);
        SuspectTimeout sc = new SuspectTimeout(scheduleTimeout,suspectedPeer);
        scheduleTimeout.setTimeoutEvent(sc);
        trigger(scheduleTimeout, timer);
    }

    private void cancelPeriodicPing() {
        CancelTimeout cpt = new CancelTimeout(pingTimeoutId);
        trigger(cpt, timer);
        pingTimeoutId = null;
    }
    private void cancelPongTimeout(UUID timeoutId, NatedAddress source) {
        if (pongTimeoutId != null) {
            trigger(new CancelTimeout(pongTimeoutId), timer);
            pongTimeoutId = null;
        }

    }


    //-------------------------------------- Timeout Events ---------------------------------------------------------//
    //                                                                                                               //
    //---------------------------------------------------------------------------------------------------------------//

    private static class PingTimeout extends Timeout {
        public PingTimeout(SchedulePeriodicTimeout request) {
            super(request);
        }
    }

    private class PongTimeout extends Timeout{
        private NatedAddress peer;
        public PongTimeout(ScheduleTimeout schedulePeriodicTimeout, NatedAddress peer) {
            super(schedulePeriodicTimeout);
            this.peer = peer;

        }
        public NatedAddress getSuspectedPeer(){
            return peer;
        }
    }
    private class SuspectTimeout extends Timeout{
        private NatedAddress deadPeer;
        //private UUID suspectTimeoutID;
        public SuspectTimeout(ScheduleTimeout schedulePeriodicTimeout, NatedAddress peer) {
            super(schedulePeriodicTimeout);
            deadPeer = peer;
            // suspectTimeoutID = getTimeoutId();
        }
    /*
            public UUID getSuspectTimeoutID() {
                return suspectTimeoutID;
            }*/

        public NatedAddress getDeadPeer(){
            return deadPeer;
        }
    }
}
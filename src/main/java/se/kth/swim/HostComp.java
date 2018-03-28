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

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.croupier.CroupierComp;
import se.kth.swim.croupier.CroupierConfig;
import se.kth.swim.croupier.CroupierPort;
import se.kth.swim.croupier.util.OverlayFilter;
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class HostComp extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(HostComp.class);
    private Positive<Network> network = requires(Network.class);
    private Positive<Timer> timer = requires(Timer.class);

    private final NatedAddress selfAddress;
    
    private Component swim;
    private Component nat;
    private Component croupier;

    public HostComp(HostInit init) {
        this.selfAddress = init.selfAddress;
        log.info("{} initiating...", new Object[]{selfAddress});
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        
        int overlayId = 1; //so far we don't start multiple croupier overlay
        croupier = create(CroupierComp.class, new CroupierComp.CroupierInit(selfAddress, new ArrayList<NatedAddress>(init.bootstrapNodes), init.seed, init.croupierConfig, overlayId));
        connect(croupier.getNegative(Timer.class), timer, Channel.TWO_WAY);
        connect(croupier.getNegative(Network.class), network, new OverlayFilter(overlayId), Channel.TWO_WAY);
        
        nat = create(NatTraversalComp.class, new NatTraversalComp.NatTraversalInit(selfAddress, init.seed));
        connect(nat.getNegative(Network.class), network, Channel.TWO_WAY);
        connect(nat.getNegative(CroupierPort.class), croupier.getPositive(CroupierPort.class), Channel.TWO_WAY);
        
        swim = create(SwimComp.class, new SwimComp.SwimInit(selfAddress, init.bootstrapNodes));
        connect(swim.getNegative(Timer.class), timer, Channel.TWO_WAY);
        connect(swim.getNegative(Network.class), nat.getPositive(Network.class), Channel.TWO_WAY);
    }
    
    private Handler<Start> handleStart = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            log.info("{} starting...", new Object[]{selfAddress});
        }

    };
    private Handler<Stop> handleStop = new Handler<Stop>() {

        @Override
        public void handle(Stop event) {
            log.info("{} stopping...", new Object[]{selfAddress});
        }

    };

    public static class HostInit extends Init<HostComp> {

        public final NatedAddress selfAddress;
        public final List<NatedAddress> bootstrapNodes;
        public final long seed;
        public final CroupierConfig croupierConfig;

        public HostInit(NatedAddress selfAddress, List<NatedAddress> bootstrapNodes, long seed, 
          CroupierConfig croupierConfig) {
            this.selfAddress = selfAddress;
            this.bootstrapNodes = bootstrapNodes;
            this.seed = seed;
            this.croupierConfig = croupierConfig;
        }
    }
}

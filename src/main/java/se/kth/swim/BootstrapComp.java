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

import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.croupier.msg.CroupierJoin;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.PingPongPort;
import se.kth.swim.msg.Pong;
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.*;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BootstrapComp extends ComponentDefinition {

  private static final Logger log = LoggerFactory.getLogger(BootstrapComp.class);
  private Positive<Network> network = requires(Network.class);
  private Positive<Timer> timer = requires(Timer.class);

  private final NatedAddress selfAddress;
  private TreeMap<Integer, Address> peers = new TreeMap<>();

  public BootstrapComp(BootStrapInit init) {
    this.selfAddress = init.selfAddress;
    log.info("{} initiating...", new Object[]{selfAddress.getId()});
      Component pingercomp = create(PingerComp.class, Init.NONE);
      Component pongercomp = create(PongerComp.class, Init.NONE);

    subscribe(handleStart, control);
    subscribe(handleStop, control);

      {
          connect(pingercomp.getNegative(PingPongPort.class),pongercomp.getPositive(PingPongPort.class));
      }
  }

  Handler<Start> handleStart = new Handler<Start>() {

    @Override
    public void handle(Start event) {
      log.info("{} starting...", new Object[]{selfAddress});
    }

  };
  Handler<Stop> handleStop = new Handler<Stop>() {

    @Override
    public void handle(Stop event) {
      log.info("{} stopping...", new Object[]{selfAddress});
    }
  };
  Handler<CroupierJoin> croupierJoinHandler = new Handler<CroupierJoin>() {
    @Override
    public void handle(CroupierJoin croupierJoin) {
      for(NatedAddress peer : croupierJoin.getPeers()){
          peers.put(peer.getId(),peer.getBaseAdr());
      }
    }
};

  public static class BootStrapInit extends Init<BootstrapComp> {

    public final NatedAddress selfAddress;

    public BootStrapInit(NatedAddress selfAddress) {
      this.selfAddress = selfAddress;
    }
  }
}

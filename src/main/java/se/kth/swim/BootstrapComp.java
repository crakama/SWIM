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
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
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

  public BootstrapComp(Init init) {
    this.selfAddress = init.selfAddress;
    log.info("{} initiating...", new Object[]{selfAddress.getId()});

    subscribe(handleStart, control);
    subscribe(handleStop, control);
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

  public static class Init extends se.sics.kompics.Init<BootstrapComp> {

    public final NatedAddress selfAddress;

    public Init(NatedAddress selfAddress) {
      this.selfAddress = selfAddress;
    }
  }
}

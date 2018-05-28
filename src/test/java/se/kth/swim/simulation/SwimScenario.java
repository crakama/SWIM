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
package se.kth.swim.simulation;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.javatuples.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.HostComp;
import se.kth.swim.croupier.CroupierConfig;
import se.kth.swim.nat.NatType;
import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.impl.BasicAddress;
import se.kth.swim.network.impl.BasicNatedAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation3;
import se.sics.kompics.simulator.adaptor.distributions.ConstantDistribution;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.ChangeNetworkModelEvent;
import se.sics.kompics.simulator.events.system.KillNodeEvent;
import se.sics.kompics.simulator.events.system.SetupEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import se.sics.kompics.simulator.network.NetworkModel;
import se.sics.kompics.simulator.network.identifier.Identifier;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;
import se.sics.kompics.simulator.network.impl.DeadLinkNetworkModel;
import se.sics.kompics.simulator.network.impl.DisconnectedNodesNetworkModel;
import se.sics.kompics.simulator.network.impl.UniformRandomModel;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SwimScenario {

  private static final Logger log = LoggerFactory.getLogger(SwimScenario.class);

  private static long seed;
  private static InetAddress localHost;

  private static CroupierConfig croupierConfig = new CroupierConfig(10, 5, 2000, 1000);

  static {
    try {
      localHost = InetAddress.getByName("127.0.0.1");
    } catch (UnknownHostException ex) {
      throw new RuntimeException(ex);
    }
  }

  static Operation<SetupEvent> setupSystemOp = new Operation<SetupEvent>() {

    @Override
    public SetupEvent generate() {
      return new SetupEvent() {
        @Override
        public IdentifierExtractor getIdentifierExtractor() {
          return idExtractor();
        }
      };
    }
  };
    
  static Operation3<StartNodeEvent, Integer, Integer, Integer> startNodeOp
    = new Operation3<StartNodeEvent, Integer, Integer, Integer>() {

      @Override
      public StartNodeEvent generate(final Integer nodeId, final Integer firstId, final Integer lastId) {
        return new StartNodeEvent() {
            HostComp.HostInit host;
            private NatedAddress nodeAddress;

          @Override
          public Address getNodeAddress() {
            return nodeAddress;
          }

          @Override
          public Class<? extends ComponentDefinition> getComponentDefinition() {
            return HostComp.class;
          }

          @Override
          public Init getComponentInit() {
            nodeAddress = getAddress(nodeId, firstId, lastId);
            long nodeSeed = seed + nodeId;

            log.info("StartNodeEvent of peer {} at getComponentInit method bootstrapNodes{} nodeseed:{} croupierConfig{}",
                    new Object[]{nodeAddress.getId(),bootstrapNodes(nodeId, firstId, lastId),nodeSeed,croupierConfig});
              return new HostComp.HostInit(nodeAddress, bootstrapNodes(nodeId, firstId, lastId), nodeSeed, croupierConfig);
          }
        };
      }
    };

  static Operation3<KillNodeEvent, Integer, Integer, Integer> killNodeOp
    = new Operation3<KillNodeEvent, Integer, Integer, Integer>() {

      @Override
      public KillNodeEvent generate(final Integer nodeId, final Integer firstId, final Integer lastId) {
        return new KillNodeEvent() {

          @Override
          public Address getNodeAddress() {
              log.info("KillNodeEvent of peer {} at getComponentInit method bootstrapNodes{} firstId:{} lastId{}",
                      new Object[]{nodeId,bootstrapNodes(nodeId, firstId, lastId),firstId,lastId});
            return getAddress(nodeId, firstId, lastId);
          }


        };

      }

    };

  //Usable NetworkModels:
  //1. UniformRandomModel
  //parameters: minimum link latency, maximum link latency
  //by default Simulator starts with UniformRandomModel(50, 500), so minimum link delay:50ms, maximum link delay:500ms
  //2. DeadLinkNetworkModel
  //composite network model that can be built on any other network model
  //parameters: network model, set of dead links (directed links)
  //Pair<1,2> means if node 1 will try to send a message to node 2, the simulator will drop this message, since this is a dead link
  //3. DisconnectedNodesNetworkModel
  //composite network model that can be built on any other network model
  //parameters: network model, set of disconnected nodes
  //a disconnected node will not be able to send or receive messages
  static Operation<ChangeNetworkModelEvent> disconnectedNodesNMOp(final Set<Identifier> disconnectedNodes) {
    return new Operation<ChangeNetworkModelEvent>() {

      @Override
      public ChangeNetworkModelEvent generate() {
        NetworkModel baseNetworkModel = new UniformRandomModel(50, 500);
        NetworkModel compositeNetworkModel
          = new DisconnectedNodesNetworkModel(idExtractor(), baseNetworkModel, disconnectedNodes);
        return new ChangeNetworkModelEvent(compositeNetworkModel);
      }
    };
  }

  static Operation<ChangeNetworkModelEvent> deadLinksNMOp(final Set<Pair<Identifier, Identifier>> deadLinks) {
    return new Operation<ChangeNetworkModelEvent>() {

      @Override
      public ChangeNetworkModelEvent generate() {
        NetworkModel baseNetworkModel = new UniformRandomModel(50, 500);
        NetworkModel compositeNetworkModel
          = new DeadLinkNetworkModel(idExtractor(), baseNetworkModel, deadLinks);
        return new ChangeNetworkModelEvent(compositeNetworkModel);
      }
    };
  }

  //Operations require Distributions as parameters
  //1.ConstantDistribution - this will provide same parameter no matter how many times it is called
  //2.BasicIntSequentialDistribution - on each call it gives the next int. Works more or less like a counter
  //3.BasixIntSequentialDistribution - give it a start int id. It will draw elements and inc the id by 1 on each call. 
  //you can implement your own - by extending Distribution
  public static SimulationScenario simpleBoot(final long seed) {
    SwimScenario.seed = seed;
    SimulationScenario scen = new SimulationScenario() {
      private final Set<Pair<Identifier, Identifier>> deadLinks = new HashSet<>();
      private final Set<Identifier> disconnectedNodes = new HashSet<>();

      {
        //Make sure that your dead link set reflect the nodes in your system
        deadLinks.add(Pair.with(id(1), id(7)));
        deadLinks.add(Pair.with(id(7), id(1)));

        //Make sure disconnected nodes reflect your nodes in the system
        disconnectedNodes.add(id(5));

        final int nrNodes = 10;
        final int firstId = 1;
        final int lastId = firstId + (nrNodes - 1);

        StochasticProcess setupSystem = new StochasticProcess() {
          {
            eventInterArrivalTime(constant(10));
            raise(1, setupSystemOp);
          }
        };
        
        StochasticProcess startPeers = new StochasticProcess() {
          {
            eventInterArrivalTime(constant(10));
            raise(nrNodes, startNodeOp, new BasicIntSequentialDistribution(1), nodeIdConst(firstId), nodeIdConst(lastId));
          }
        };
          StochasticProcess startDeadPeers = new StochasticProcess() {
              {
                  eventInterArrivalTime(constant(1000));
                  raise(1, startNodeOp, nodeIdConst(5), nodeIdConst(firstId), nodeIdConst(lastId));
              }
          };

        StochasticProcess killPeers = new StochasticProcess() {
          {
            eventInterArrivalTime(constant(1000));
            raise(1, killNodeOp, nodeIdConst(5), nodeIdConst(firstId), nodeIdConst(lastId));
          }
        };

        StochasticProcess deadLinks1 = new StochasticProcess() {
          {
            eventInterArrivalTime(constant(1000));
            raise(1, deadLinksNMOp(deadLinks));
          }
        };

        StochasticProcess disconnectedNodes1 = new StochasticProcess() {
          {
            eventInterArrivalTime(constant(1000));
            raise(1, disconnectedNodesNMOp(disconnectedNodes));
          }
        };

        setupSystem.start();
        startPeers.startAfterTerminationOf(10, setupSystem);
        killPeers.startAfterTerminationOf(10000, startPeers);
        startDeadPeers.startAfterTerminationOf(5000, killPeers);
//                deadLinks1.startAfterTerminationOf(10000,startPeers);
//                disconnectedNodes1.startAfterTerminationOf(10000, startPeers);
        terminateAt(60 * 1000 * 1000);
      }

      ConstantDistribution nodeIdConst(int id) {
        return new ConstantDistribution(Integer.class, id);
      }

      BasicIntSequentialDistribution nodeIdDist(int startId) {
        return new BasicIntSequentialDistribution(startId);
      }

    };

    scen.setSeed(seed);

    return scen;
  }

  static List<NatedAddress> bootstrapNodes(int selfId, int firstId, int lastId) {
    List<NatedAddress> bootstrap = new LinkedList<>();

    int nrBootstrapNodes = 4;
    if (firstId + nrBootstrapNodes >= lastId) {
      throw new RuntimeException("start simulation with at least " + nrBootstrapNodes + 1 + " nodes");
    }
    if (firstId + nrBootstrapNodes >= selfId) {
      //first nrBootstrapNodes excluding itself
      for (int nodeId = firstId; nodeId <= firstId + nrBootstrapNodes; nodeId++) {
        if (nodeId != selfId) {
          bootstrap.add(getAddress(nodeId, firstId, lastId));
        }
      }
    } else {
      //previous nrBootstrap nodes
      for (int i = 1; i <= nrBootstrapNodes; i++) {
        bootstrap.add(getAddress(selfId - i, firstId, lastId));
      }
    }
    return bootstrap;
  }

  static List<NatedAddress> parentNodes(int selfId, int firstId, int lastId) {
    Assert.assertTrue(selfId % 2 == 1); //only odd nodes are nated and thus need parents
    List<NatedAddress> parents = new LinkedList<>();
    int nrParentNodes = 2;
    //only even node are open
    if (firstId + nrParentNodes * 2 >= lastId) {
      throw new RuntimeException("start simulation with at least " + (nrParentNodes * 2 + 1) + " nodes");
    }
    if (firstId + nrParentNodes * 2 >= selfId) {
      //first nrParents even nodes
      int firstEven = firstId + firstId % 2;
      for (int i = 1; i <= nrParentNodes; i++) {
        parents.add(getAddress(firstEven + 2 * i, firstId, lastId));
      }
    } else {
      //previous nrParents even nodes
      for (int i = 0; i < nrParentNodes; i++) {
        parents.add(getAddress(selfId - 1 - 2 * i, firstId, lastId));
      }
    }
    return parents;
  }

  static NatedAddress getAddress(int nodeId, int firstId, int lastId) {
      log.info("{},{},{}", new Object[]{nodeId, firstId, lastId});
    BasicAddress baseAdr = new BasicAddress(localHost, 12345, nodeId);
    if (nodeId % 2 == 0) {
      //open address
      return new BasicNatedAddress(baseAdr);
    } else {
      //nated address
      return new BasicNatedAddress(baseAdr, NatType.NAT, parentNodes(nodeId, firstId, lastId));
    }
  }

  static IdentifierExtractor idExtractor() {
    return new IdentifierExtractor() {

      @Override
      public Identifier extract(Address adr) {
        NatedAddress na = (NatedAddress) adr;
        return new SimId(na.getId());
      }
    };
  }

  static Identifier id(int nodeId) {
    return new SimId(nodeId);
  }

  public static class SimId implements Identifier {

    private final int id;

    public SimId(int id) {
      this.id = id;
    }

    @Override
    public int partition(int nrPartitions) {
      return id % nrPartitions;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 59 * hash + this.id;
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final SimId other = (SimId) obj;
      if (this.id != other.id) {
        return false;
      }
      return true;
    }
  }
}

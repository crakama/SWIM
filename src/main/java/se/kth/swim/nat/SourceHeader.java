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
package se.kth.swim.nat;

import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SourceHeader<A extends Address> implements Header<A> {
  public final Header<A> srcHeader;
  public final A relay;
  
  public SourceHeader(Header<A> srcHeader, A relay) {
    this.srcHeader = srcHeader;
    this.relay = relay;
  }
  
  public RelayHeader getRelayHeader() {
    return new RelayHeader(srcHeader, relay);
  }

  public A getSource() {
    return srcHeader.getSource();
  }

  public A getDestination() {
    return relay;
  }

  public Transport getProtocol() {
    return srcHeader.getProtocol();
  }

  @Override
  public String toString() {
    return "SourceHeader{" + srcHeader.getSource() + "->" + relay + "->" + srcHeader.getDestination() + '}';
  }
}

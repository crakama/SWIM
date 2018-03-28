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
package se.kth.swim.network.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import se.kth.swim.nat.NatType;
import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.network.Address;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class BasicNatedAddress implements NatedAddress {
  public final BasicAddress baseAddress;
  public final NatType natType;
  public final List<NatedAddress> parents;
  
  public BasicNatedAddress(BasicAddress baseAddress, NatType natType, List<NatedAddress> parents) {
    this.baseAddress = baseAddress;
    this.natType = natType;
    this.parents = parents;
  }
  
  public BasicNatedAddress(BasicAddress baseAddress) {
    this(baseAddress, NatType.OPEN, new LinkedList<NatedAddress>());
  }
  
  @Override
  public boolean isOpen() {
    return NatType.OPEN.equals(natType);
  }

  @Override
  public NatType getNatType() {
    return natType;
  }

  @Override
  public List<NatedAddress> getParents() {
    return parents;
  }

  @Override
  public Address getBaseAdr() {
    return baseAddress;
  }

  @Override
  public Integer getId() {
    return baseAddress.getId();
  }

  @Override
  public InetAddress getIp() {
    return baseAddress.getIp();
  }

  @Override
  public int getPort() {
    return baseAddress.getPort();
  }

  @Override
  public InetSocketAddress asSocket() {
    return baseAddress.asSocket();
  }

  @Override
  public boolean sameHostAs(Address other) {
    return baseAddress.sameHostAs(other);
  }

  @Override
  public int hashCode() {
    int hash = 5;
    hash = 17 * hash + Objects.hashCode(this.baseAddress);
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
    final BasicNatedAddress other = (BasicNatedAddress) obj;
    if (!Objects.equals(this.baseAddress, other.baseAddress)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return baseAddress.toString();
  }
}

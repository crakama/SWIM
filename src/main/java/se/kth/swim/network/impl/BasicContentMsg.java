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

import se.kth.swim.nat.NatedAddress;
import se.kth.swim.network.ContentMsg;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class BasicContentMsg<C extends Object> extends ContentMsg<NatedAddress, Header<NatedAddress>, C> {


  public BasicContentMsg(NatedAddress src, NatedAddress dst, C content) {
    this(new BasicHeader(src, dst, Transport.UDP), content);
  }

  public BasicContentMsg(Header<NatedAddress> header, C content) {
    super(header, content);
  }

  public BasicContentMsg newHeader(Header<NatedAddress> header) {
    return new BasicContentMsg(header, content);
  }


  @Override
  public NatedAddress getSource() {
    return header.getSource();
  }

  @Override
  public C getContent() {
    return super.getContent();
  }

  @Override
  public NatedAddress getDestination() {
    return header.getDestination();
  }

  @Override
  public Transport getProtocol() {
    return header.getProtocol();
  }
}

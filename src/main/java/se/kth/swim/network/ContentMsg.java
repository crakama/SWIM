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
package se.kth.swim.network;

import se.kth.swim.nat.NatedAddress;
import se.sics.kompics.PatternExtractor;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Msg;

import java.util.List;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public abstract class ContentMsg<A extends Address, H extends Header<A>, C extends Object>
  implements Msg<A, H>, PatternExtractor<Class<Object>, C> {

  public final H header;
  public final C content;
  private List<NatedAddress> statusContent;

  public ContentMsg(H header, C content) {
    this.header = header;
    this.content = content;
  }

  public C getContent() {
    return content;
  }


  public H getHeader() {
    return header;
  }

  @Override
  public Class<Object> extractPattern() {
    return (Class) content.getClass();
  }

  @Override
  public C extractValue() {
    return content;
  }
}

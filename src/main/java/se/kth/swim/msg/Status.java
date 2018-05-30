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

package se.kth.swim.msg;

import se.kth.swim.nat.NatedAddress;

import static se.kth.swim.msg.StatusType.ALIVE;
import static se.kth.swim.msg.StatusType.DEAD;
import static se.kth.swim.msg.StatusType.SUSPECTED;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class Status {
    public int receivedPings;

    private StatusType statusType = null;
    private NatedAddress statusReporter;
    public int incarnationNo;
    private NatedAddress peer;

    public Status(StatusType s, int incarnationNo, NatedAddress statusOwner, NatedAddress statusReporter)
    {
        statusType =s;
        this.incarnationNo=incarnationNo;
        this.peer = statusOwner;
        this.statusReporter = statusReporter;
    }
    public Status(int receivedPings) {
        this.receivedPings = receivedPings;
    }

    public int getIncarnationNo() {
        return incarnationNo;
    }

    public boolean isAlive()
    {
        if(statusType.equals(ALIVE))
            return true;
        return false;
    }
    public NatedAddress getPeer(){
        return peer;
    }

    public NatedAddress getStatusReporter() {
        return statusReporter;
    }

    public boolean isSuspected() {
        if(statusType.equals(SUSPECTED))
            return true;
        return false;
    }

    public boolean isDead() {
        if(statusType.equals(DEAD))
            return true;
        return false;
    }

    public StatusType getStatusType() {
        return statusType;
    }
}

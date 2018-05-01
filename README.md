
# SWIM

#### Scalable Weakly Consistent Infection Style Process Group Membership Protocol

##### Membership Discovery:
- A new peer randomly selects another peer and sends it a _`PING`_ event.
- Upon receipt of a ping event the receiving peer should respond back 
with a PONG message which consists of local view of the sending peer, the view is then merged with existing local state.

##### Basic SWIM Failure Detector:
- Introduce failures in the system
- If peer does not respond to _`PING`_ events(If _`PONG`_ not receive after a predefined timeout)
declare peer as a dead node and disseminate the death information through gossip - select random peers and send them death event.

##### Message Ordering :


##### K-InDirect Pings :


##### Limiting the information exchanged :
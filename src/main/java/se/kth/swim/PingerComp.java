package se.kth.swim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.PingPongPort;
import se.kth.swim.msg.Pong;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;

public class PingerComp extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(PingerComp.class);
    private Positive<PingPongPort> pingPongPortPositive = positive(PingPongPort.class);

    Handler<Pong> pongHandler = new Handler<Pong>() {
        @Override
        public void handle(Pong pong) {
            log.debug("Got a pong from :");
            trigger(new Ping(),pingPongPortPositive);
        }
    };
    //Subscribe Handlers to ports
    {
        subscribe(pongHandler,pingPongPortPositive);
    }
}

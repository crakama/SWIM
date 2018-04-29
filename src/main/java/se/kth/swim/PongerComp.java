package se.kth.swim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.swim.msg.Ping;
import se.kth.swim.msg.PingPongPort;
import se.kth.swim.msg.Pong;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;

public class PongerComp extends ComponentDefinition {
    private static final Logger log = LoggerFactory.getLogger(PongerComp.class);
    Negative<PingPongPort> pingPongPortNegative = negative(PingPongPort.class);

    Handler<Ping> pingHandler = new Handler<Ping>() {
        @Override
        public void handle(Ping ping) {
            log.debug("Got a PING from : ...and now sending a PONG");
            //trigger(new Pong(),pingPongPortNegative);
        }
    };
    //Subscribe Handlers to ports
    {
        subscribe(pingHandler,pingPongPortNegative);
    }
}

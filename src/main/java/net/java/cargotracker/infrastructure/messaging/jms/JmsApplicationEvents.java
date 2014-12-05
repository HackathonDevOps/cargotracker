package net.java.cargotracker.infrastructure.messaging.jms;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.JMSContext;
import net.java.cargotracker.application.ApplicationEvents;
import net.java.cargotracker.domain.model.cargo.Cargo;
import net.java.cargotracker.domain.model.handling.HandlingEvent;
import net.java.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;

@ApplicationScoped
public class JmsApplicationEvents implements ApplicationEvents, Serializable {

    private static final int LOW_PRIORITY = 0;
    @Inject
    JMSContext jmsContext;
    @Resource(lookup = "java:global/jms/CargoHandledQueue")
    private Destination cargoHandledQueue;
    @Resource(lookup = "java:global/jms/MisdirectedCargoQueue")
    private Destination misdirectedCargoQueue;
    @Resource(lookup = "java:global/jms/DeliveredCargoQueue")
    private Destination deliveredCargoQueue;
    @Resource(lookup = "java:global/jms/HandlingEventRegistrationAttemptQueue")
    private Destination handlingEventQueue;
    private static final Logger logger = Logger.getLogger(
            JmsApplicationEvents.class.getName());

    @Override
    public void cargoWasHandled(HandlingEvent event) {
        Cargo cargo = event.getCargo();
        logger.log(Level.INFO, "Cargo was handled {0}", cargo);
        jmsContext.createProducer()
                .setPriority(LOW_PRIORITY)
                .setDisableMessageID(true)
                .setDisableMessageTimestamp(true)
                .send(cargoHandledQueue,
                        cargo.getTrackingId().getIdString());
    }

    @Override
    public void cargoWasMisdirected(Cargo cargo) {
        logger.log(Level.INFO, "Cargo was misdirected {0}", cargo);
        jmsContext.createProducer()
                .setPriority(LOW_PRIORITY)
                .setDisableMessageID(true)
                .setDisableMessageTimestamp(true)
                .send(misdirectedCargoQueue,
                        cargo.getTrackingId().getIdString());
    }

    @Override
    public void cargoHasArrived(Cargo cargo) {
        logger.log(Level.INFO, "Cargo has arrived {0}", cargo);
        jmsContext.createProducer()
                .setPriority(LOW_PRIORITY)
                .setDisableMessageID(true)
                .setDisableMessageTimestamp(true)
                .send(deliveredCargoQueue,
                        cargo.getTrackingId().getIdString());
    }

    @Override
    public void receivedHandlingEventRegistrationAttempt(
            HandlingEventRegistrationAttempt attempt) {
        logger.log(Level.INFO, "Received handling event registration attempt {0}",
                attempt);
        jmsContext.createProducer()
                .setPriority(LOW_PRIORITY)
                .setDisableMessageID(true)
                .setDisableMessageTimestamp(true)
                .send(handlingEventQueue, attempt);
    }
}
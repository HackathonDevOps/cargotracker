package net.java.cargotracker.application.util;

import net.java.cargotracker.interfaces.booking.rest.CargoMonitoringService;
import net.java.cargotracker.interfaces.handling.rest.HandlingReportService;
import net.java.pathfinder.api.GraphTraversalService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS configuration.
 */
@ApplicationPath("rest")
public class RestConfiguration extends Application {

	@Override
	public Set<Class<?>> getClasses() {
		return new HashSet<>(Arrays.asList(HandlingReportService.class, GraphTraversalService.class, CargoMonitoringService.class));
	}

}

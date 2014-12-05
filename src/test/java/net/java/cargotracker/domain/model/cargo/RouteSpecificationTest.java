package net.java.cargotracker.domain.model.cargo;

import static org.junit.Assert.*;
import net.java.cargotracker.domain.model.cargo.RouteSpecification;
import net.java.cargotracker.domain.model.cargo.Itinerary;
import net.java.cargotracker.domain.model.cargo.Leg;

import java.util.Arrays;

import net.java.cargotracker.application.util.DateUtil;
import net.java.cargotracker.domain.model.location.SampleLocations;
import net.java.cargotracker.domain.model.voyage.Voyage;
import net.java.cargotracker.domain.model.voyage.VoyageNumber;

import org.junit.Test;

public class RouteSpecificationTest {

	Voyage hongKongTokyoNewYork = new Voyage.Builder(new VoyageNumber("V001"),
			SampleLocations.HONGKONG)
			.addMovement(SampleLocations.TOKYO, DateUtil.toDate("2009-02-01"),
					DateUtil.toDate("2009-02-05"))
			.addMovement(SampleLocations.NEWYORK,
					DateUtil.toDate("2009-02-06"),
					DateUtil.toDate("2009-02-10"))
			.addMovement(SampleLocations.HONGKONG,
					DateUtil.toDate("2009-02-11"),
					DateUtil.toDate("2009-02-14")).build();
	Voyage dallasNewYorkChicago = new Voyage.Builder(new VoyageNumber("V002"),
			SampleLocations.DALLAS)
			.addMovement(SampleLocations.NEWYORK,
					DateUtil.toDate("2009-02-06"),
					DateUtil.toDate("2009-02-07"))
			.addMovement(SampleLocations.CHICAGO,
					DateUtil.toDate("2009-02-12"),
					DateUtil.toDate("2009-02-20")).build();
	// TODO:
	// It shouldn't be possible to create Legs that have load/unload locations
	// and/or dates that don't match the voyage's carrier movements.
	Itinerary itinerary = new Itinerary(Arrays.asList(
			new Leg(hongKongTokyoNewYork, SampleLocations.HONGKONG,
					SampleLocations.NEWYORK, DateUtil.toDate("2009-02-01"),
					DateUtil.toDate("2009-02-10")), new Leg(
					dallasNewYorkChicago, SampleLocations.NEWYORK,
					SampleLocations.CHICAGO, DateUtil.toDate("2009-02-12"),
					DateUtil.toDate("2009-02-20"))));

	@Test
	public void testIsSatisfiedBySuccess() {
		RouteSpecification routeSpecification = new RouteSpecification(
				SampleLocations.HONGKONG, SampleLocations.CHICAGO,
				DateUtil.toDate("2009-03-01"));

		assertTrue(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	public void testIsNotSatisfiedByWrongOrigin() {
		RouteSpecification routeSpecification = new RouteSpecification(
				SampleLocations.HANGZOU, SampleLocations.CHICAGO,
				DateUtil.toDate("2009-03-01"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	public void testIsNotSatisfiedByWrongDestination() {
		RouteSpecification routeSpecification = new RouteSpecification(
				SampleLocations.HONGKONG, SampleLocations.DALLAS,
				DateUtil.toDate("2009-03-01"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}

	@Test
	public void testIsNotSatisfiedByMissedDeadline() {
		RouteSpecification routeSpecification = new RouteSpecification(
				SampleLocations.HONGKONG, SampleLocations.CHICAGO,
				DateUtil.toDate("2009-02-15"));

		assertFalse(routeSpecification.isSatisfiedBy(itinerary));
	}
}
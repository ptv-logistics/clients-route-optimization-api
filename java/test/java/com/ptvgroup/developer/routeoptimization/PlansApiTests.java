package com.ptvgroup.developer.routeoptimization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptvgroup.developer.client.routeoptimization.ApiException;
import com.ptvgroup.developer.client.routeoptimization.api.PlansApi;
import com.ptvgroup.developer.client.routeoptimization.model.ErrorResponse;
import com.ptvgroup.developer.client.routeoptimization.model.LocationType;
import com.ptvgroup.developer.client.routeoptimization.model.MixedLoadingProhibition;
import com.ptvgroup.developer.client.routeoptimization.model.Plan;
import com.ptvgroup.developer.client.routeoptimization.model.PlanSummaries;
import com.ptvgroup.developer.client.routeoptimization.model.PlanningRestrictions;
import com.ptvgroup.developer.client.routeoptimization.model.Route;
import com.ptvgroup.developer.client.routeoptimization.model.Stop;
import com.ptvgroup.developer.client.routeoptimization.model.TimeInterval;
import com.ptvgroup.developer.client.routeoptimization.model.CustomerLocationAttributes;
import com.ptvgroup.developer.client.routeoptimization.model.PositionInTrip;

public class PlansApiTests extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(PlansApiTests.class);

    @Test
    void validInstance() {
        assertThat(plansApiInstance instanceof PlansApi, is(true));
    }

    @Test
    void createPlanTest() throws Exception {
        final Plan plan = getSimpleValidPlan();
        final Plan response = createPlanViaApi(plan);
    }

    @Test
    void whenEmptyPlanIsUsedThenErrorIsReturned() throws Exception {
        Plan plan = null;
        ApiException thrown = assertThrows(ApiException.class,
                () -> plansApiInstance.createPlan(plan),
                "Expected createPlan to throw, but it did not.");
        assertThat(thrown.getMessage(), is(equalTo("Missing the required parameter 'plan' when calling createPlan")));
    }

    @Test
    void whenCreatingPlanWithValidPlanningHorizonThenSuccessIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        final OffsetDateTime now = OffsetDateTime.now();
        plan.setPlanningHorizon(new TimeInterval()
                .start(now)
                .end(now.plus(Duration.ofHours(5))));

        final Plan response = createPlanViaApi(plan);
        assertThat(response.getPlanningHorizon(), is(notNullValue()));
        assertThat(response.getPlanningHorizon().getStart().toInstant(), is(equalTo(plan.getPlanningHorizon().getStart().toInstant())));
        assertThat(response.getPlanningHorizon().getEnd().toInstant(), is(equalTo(plan.getPlanningHorizon().getEnd().toInstant())));
    }
    
    @Test
    public void WhenCreatingPlanWithInvalidPlanningHorizonTimeZoneOffsetThenErrorIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.planningHorizon(new TimeInterval()
            .start(OffsetDateTime.of(2021, 5, 5, 0, 0, 1, 0, ZoneOffset.ofTotalSeconds(120)))
            .end(OffsetDateTime.of(2021, 5, 5, 23, 59, 59, 0, ZoneOffset.ofTotalSeconds(120))));
        
        ApiException thrown = assertThrows(ApiException.class,
                () -> plansApiInstance.createPlan(plan),
                "Expected createPlan to throw, but it did not.");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getCauses().size(), is(equalTo(2)));
        assertThat(errorResponse.getCauses().get(0).getErrorCode(), is(equalTo("GENERAL_INVALID_VALUE")));
        assertThat(errorResponse.getCauses().get(0).getParameter(), is(equalTo("planningHorizon.start")));
        assertThat(errorResponse.getCauses().get(1).getErrorCode(), is(equalTo("GENERAL_INVALID_VALUE")));
        assertThat(errorResponse.getCauses().get(1).getParameter(), is(equalTo("planningHorizon.end")));
    }
    
    @Test
    public void WhenCreatingPlanWithValidTripSectionAndPositionInTripThenSuccessIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.getLocations().get(1).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(Integer.MAX_VALUE));
        plan.getLocations().get(2).setCustomerLocationAttributes(new CustomerLocationAttributes().positionInTrip(PositionInTrip.LAST_CUSTOMER_STOP));
        
        createPlanViaApi(plan);
    }

    @Test
    public void WhenCreatingPlanWithMixedLoadingProhibitionsThenSuccessIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.restrictions(new PlanningRestrictions().addMixedLoadingProhibitionsItem(
            new MixedLoadingProhibition().conflictingLoadCategory1("acid").conflictingLoadCategory2("base")
        ));
        createPlanViaApi(plan);
    }

    @Test
    void whenCreatingPlanWithoutDriversThenSuccessIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.setDrivers(null);

        createPlanViaApi(plan);
    }

    @Test
    void whenCreatingPlanWithTwoDepotLocationsIsSuccess() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.getLocations().get(1).type(LocationType.DEPOT);

        createPlanViaApi(plan);
    }

    @Test
    void whenCreatingPlanWithDuplicatedPickupsThenErrorIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.getVehicles().get(0).startLocationId("Depot").endLocationId("Depot");

        Route routeOfVehicle0 = getRouteOfVehicle(plan, plan.getVehicles().get(0).getId());
        assertThat(routeOfVehicle0, notNullValue());
        routeOfVehicle0.addStopsItem(new Stop().locationId("Depot").tripId("trip0"));

        routeOfVehicle0.addStopsItem(new Stop().locationId("Depot").pickupIds(Arrays.asList("76131")));
        routeOfVehicle0.addStopsItem(new Stop().locationId("Customer1").deliveryIds(Arrays.asList("76131")));
        routeOfVehicle0.addStopsItem(new Stop().locationId("Depot"));

        ApiException thrown = assertThrows(ApiException.class,
                () -> createPlanViaApi(plan),
                "Expected createPlanViaApi to throw, but it did not");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getCauses().get(0).getErrorCode(), is(equalTo("ROUTEOPTIMIZATION_DUPLICATE_TRANSPORT")));
    }

    @Test
    void whenCreatingPlanWithValidVehicleStartAndEndLocationIdThenSuccessIsReturned() throws Exception {
        Plan plan = getSimpleValidPlan();
        plan.getVehicles().get(0).startLocationId("Depot").endLocationId("Depot");

        Route routeOfVehicle0 = getRouteOfVehicle(plan, plan.getVehicles().get(0).getId());
        assertThat(routeOfVehicle0, notNullValue());
        routeOfVehicle0.addStopsItem(new Stop().locationId("Depot").tripId("trip0"));

        createPlanViaApi(plan);
    }

    @Test
    void deletePlanTest() throws Exception {
        Plan plan = getSimpleValidPlan();
        final Plan response = plansApiInstance.createPlan(plan);
        plansApiInstance.deletePlan(response.getId());

        ApiException thrown = assertThrows(ApiException.class,
                () -> plansApiInstance.getPlan(response.getId()),
                "Expected createPlan to throw, but it did not");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getCauses().get(0).getErrorCode(), is(equalTo("GENERAL_INVALID_ID")));
    }

    @Test
    void getPlanSummariesTest() throws Exception {
        PlanSummaries summaries = plansApiInstance.getPlanSummaries();
        assertThat(summaries, is(notNullValue()));
    }

    @Test
    void retrievePlanTest() throws Exception {
        Plan plan = getSimpleValidPlan();
        Plan response = createPlanViaApi(plan);

        plan = plansApiInstance.getPlan(response.getId());

        assertThat(plan.getId(), notNullValue());
        assertThat(plan.getId().equals(new UUID(0, 0)), is(false));
    }
}

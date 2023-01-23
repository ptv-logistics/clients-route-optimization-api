package com.ptvgroup.developer.routeoptimization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ptvgroup.developer.client.routeoptimization.ApiException;
import com.ptvgroup.developer.client.routeoptimization.api.OperationsApi;
import com.ptvgroup.developer.client.routeoptimization.model.ErrorResponse;
import com.ptvgroup.developer.client.routeoptimization.model.MixedLoadingProhibition;
import com.ptvgroup.developer.client.routeoptimization.model.Operation;
import com.ptvgroup.developer.client.routeoptimization.model.OperationStatus;
import com.ptvgroup.developer.client.routeoptimization.model.Plan;
import com.ptvgroup.developer.client.routeoptimization.model.PlanningRestrictions;
import com.ptvgroup.developer.client.routeoptimization.model.Route;
import com.ptvgroup.developer.client.routeoptimization.model.Stop;
import com.ptvgroup.developer.client.routeoptimization.model.TimeInterval;
import com.ptvgroup.developer.client.routeoptimization.model.Driver;
import com.ptvgroup.developer.client.routeoptimization.model.BreakRule;
import com.ptvgroup.developer.client.routeoptimization.model.DailyRestRule;
import com.ptvgroup.developer.client.routeoptimization.model.WorkingHoursPreset;
import com.ptvgroup.developer.client.routeoptimization.model.WorkLogbook;
import com.ptvgroup.developer.client.routeoptimization.model.Vehicle;
import com.ptvgroup.developer.client.routeoptimization.model.LocationType;
import com.ptvgroup.developer.client.routeoptimization.model.CustomerLocationAttributes;
import com.ptvgroup.developer.client.routeoptimization.model.PositionInTrip;
import com.ptvgroup.developer.client.routeoptimization.model.ViolationType;
import com.ptvgroup.developer.client.routeoptimization.model.OptimizationQuality;
import com.ptvgroup.developer.client.routeoptimization.model.TweakToObjective;

public class OperationsApiTests extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(OperationsApiTests.class);
    

    @Test
    void validInstance() {
        assertThat(opsApiInstance instanceof OperationsApi, is(true));
    }

    @Test
    void cancelOperation() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());
        Thread.sleep(1000);
        opsApiInstance.cancelOperation(createdPlan.getId());
    }

    @Test
    void getOperationStatus() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(initialPlan);
        final OffsetDateTime startTime = OffsetDateTime.now();

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());
        assertThat(op.getName(), is("optimization"));
        assertThat(op.getStatus(), is(OperationStatus.RUNNING));
        assertThat(op.getError(), nullValue());
        assertThat(op.getStartTime(), is(greaterThan(startTime.minusSeconds(5))));
        assertThat(op.getStartTime(), is(lessThan(startTime.plusSeconds(5))));
    }

    @Test
    void correctUTCTimeIntervalIsReturnedAfterEvaluation() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final OffsetDateTime odt = OffsetDateTime.of(2021, 01, 26, 0, 0, 0, 0, ZoneOffset.ofHours(-5));
        initialPlan.getLocations().get(0).openingIntervals(Arrays.asList(new TimeInterval()
                .start(odt)
                .end(odt.plusDays(5))));

        final Plan createdPlan = createPlanViaApi(initialPlan);
        assertThat(createdPlan.getLocations().get(0).getOpeningIntervals().get(0).getStart(), is(odt));  // Not UTC
        assertThat(createdPlan.getLocations().get(0).getOpeningIntervals().get(0).getEnd(), is(odt.plusDays(5)));

        opsApiInstance.startEvaluation(createdPlan.getId());
        final Plan evaluatedPlan = getPlanAfterOperation(createdPlan.getId());

        final OffsetDateTime expectedOdt = OffsetDateTime.of(2021, 01, 26, 0, 0, 0, 0, ZoneOffset.ofHours(-5));  // Not UTC
        assertThat(evaluatedPlan.getLocations().get(0).getOpeningIntervals().get(0).getStart(), is(expectedOdt));
        assertThat(evaluatedPlan.getLocations().get(0).getOpeningIntervals().get(0).getEnd(), is(expectedOdt.plusDays(5)));
    }

    @Test
    void correctUTCTimeIntervalIsReturnedAfterOptimization() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final OffsetDateTime odt = OffsetDateTime.of(2021, 01, 26, 0, 0, 0, 0, ZoneOffset.ofHours(-5));
        initialPlan.getLocations().get(0).openingIntervals(Arrays.asList(new TimeInterval()
                .start(odt)
                .end(odt.plusDays(5))));
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());

        final Plan optimizedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(optimizedPlan);
        final OffsetDateTime expectedOdt = OffsetDateTime.of(2021, 01, 26, 0, 0, 0, 0, ZoneOffset.ofHours(-5));  // Not UTC
        assertThat(optimizedPlan.getLocations().get(0).getOpeningIntervals().get(0).getStart(), is(expectedOdt));
        assertThat(optimizedPlan.getLocations().get(0).getOpeningIntervals().get(0).getEnd(), is(expectedOdt.plusDays(5)));
    }

    @Test
    void whenValidPlanIsOptimizedThenOperationIsCompletedWithSuccess() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());

        checkPlan(getPlanAfterOperation(createdPlan.getId()));

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        checkPlan(getPlanAfterOperation(createdPlan.getId()));
    }

    @Test
    void whenValidPlanWithValidQueryParametersIsOptimizedThenOperationIsCompletedWithSuccess() throws Exception {
        Plan initialPlan = getSimpleValidPlan();
        initialPlan.getTransports().get(0).setPriority(2);
        initialPlan.getTransports().get(1).setPriority(5);
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), OptimizationQuality.HIGH, new ArrayList<TweakToObjective>(Arrays.asList(TweakToObjective.IGNORE_MINIMIZATION_OF_NUMBER_OF_ROUTES)), true /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());

        checkPlan(getPlanAfterOperation(createdPlan.getId()));
    }

    @Test
    void whenValidPlanWithInvalidQueryParametersIsOptimizedThenErrorIsReturned() throws Exception {
        final Plan plan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(plan);

        ApiException thrown = assertThrows(ApiException.class,
                () -> opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), true /*considerTransportPriorities*/),
                "Expected createPlanViaApi to throw, but it did not");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getCauses().get(0).getErrorCode(), is(equalTo("ROUTEOPTIMIZATION_PARAMETER_CONFLICT")));
    }

    @Test
    void whenDeletingExistingOperationThenNoContentIsReturnedAndOperationIsUnavailable() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());
        Thread.sleep(1000);
        opsApiInstance.cancelOperation(createdPlan.getId());

        ApiException thrown = assertThrows(ApiException.class,
                                () -> opsApiInstance.getOperationStatus(createdPlan.getId()),
                                "Expected getOperationStatus to throw, but it did not");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getCauses().get(0).getErrorCode(), is(equalTo("GENERAL_INVALID_ID")));
    }

    @Test
    void whenValidPlanIsEvaluatedThenRoutesOfVehicleContainsReport() throws Exception {
        final Plan initialPlan = getSimpleValidPlan();
        final Plan createdPlan = createPlanViaApi(initialPlan);

        opsApiInstance.startEvaluation(createdPlan.getId());

        Plan evaluatedPlan = getPlanAfterOperation(createdPlan.getId());
        Route routeOfVehicle0 = getRouteOfVehicle(evaluatedPlan, evaluatedPlan.getVehicles().get(0).getId());
        assertThat(routeOfVehicle0, notNullValue());
        assertThat(routeOfVehicle0.getReport(), notNullValue());
    }

    @Test
    void whenValidPlanWithRoutesIsEvaluatedThenRouteVehicleIdsDoNotChange() throws Exception {
        Plan initialPlan = getSimpleValidPlan();
        Plan createdPlan = createPlanViaApi(initialPlan);
        opsApiInstance.startEvaluation(createdPlan.getId());
        final Plan evaluatedPlan = getPlanAfterOperation(createdPlan.getId());

        checkPlan(evaluatedPlan);

        Set<String> routeVehicleIdsInInitialPlan = new HashSet<String>();
        for (final Vehicle veh: initialPlan.getVehicles()) {
            Route route = getRouteOfVehicle(initialPlan, veh.getId());
            assertThat(route, notNullValue());
            routeVehicleIdsInInitialPlan.add(route.getVehicleId());
        }

        Set<String> routeVehicleIdsInEvaluatedPlan = new HashSet<String>();
        for (final Vehicle veh: evaluatedPlan.getVehicles()) {
            Route route = getRouteOfVehicle(evaluatedPlan, veh.getId());
            assertThat(route, notNullValue());
            routeVehicleIdsInEvaluatedPlan.add(route.getVehicleId());
        }

        assertThat(routeVehicleIdsInEvaluatedPlan.equals(routeVehicleIdsInInitialPlan), is(true));
    }

    @Test
    void WhenValidPlanWithTripSectionsIsOptimizedThenOperationIsCompletedWithSuccess() throws Exception {
        Plan initialPlan = createInputPlan(false);
        initialPlan.getLocations().get(0).setCustomerLocationAttributes(new CustomerLocationAttributes());
        assertThat(initialPlan.getLocations().get(0).getCustomerLocationAttributes().getTripSectionNumber(), is(equalTo(null)));
        assertThat(initialPlan.getLocations().get(0).getCustomerLocationAttributes().getPositionInTrip(), is(equalTo(null)));
        initialPlan.getLocations().get(1).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(5)); // Location0
        assertThat(initialPlan.getLocations().get(1).getCustomerLocationAttributes().getTripSectionNumber(), is(equalTo(5)));
        assertThat(initialPlan.getLocations().get(1).getCustomerLocationAttributes().getPositionInTrip(), is(equalTo(null)));
        initialPlan.getLocations().get(2).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(4)); // Location1
        initialPlan.getLocations().get(3).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(3)); // Location2
        initialPlan.getLocations().get(4).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(2)); // Location3
        initialPlan.getLocations().get(5).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(1)); // Location4
        initialPlan.getLocations().get(6).setCustomerLocationAttributes(new CustomerLocationAttributes().positionInTrip(PositionInTrip.FIRST_CUSTOMER_STOP)); // Location5
        initialPlan.getLocations().get(7).setCustomerLocationAttributes(new CustomerLocationAttributes().positionInTrip(PositionInTrip.LAST_CUSTOMER_STOP)); // Location6
        initialPlan.getLocations().get(8).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(Integer.MAX_VALUE)); // Location7
        initialPlan.getLocations().get(9).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(100)); // Location8
        initialPlan.getLocations().get(10).setCustomerLocationAttributes(new CustomerLocationAttributes().tripSectionNumber(1000)); // Location9

        Plan createdPlan = createPlanViaApi(initialPlan);
        assertThat(createdPlan.getLocations().get(0).getCustomerLocationAttributes().getTripSectionNumber(), is(equalTo(null)));
        assertThat(createdPlan.getLocations().get(0).getCustomerLocationAttributes().getPositionInTrip(), is(equalTo(null)));
        assertThat(createdPlan.getLocations().get(1).getCustomerLocationAttributes().getTripSectionNumber(), is(equalTo(5)));
        assertThat(createdPlan.getLocations().get(1).getCustomerLocationAttributes().getPositionInTrip(), is(equalTo(null)));
        opsApiInstance.startOptimization(createdPlan.getId(), getStandardOptimizationQuality(), getStandardTweaksToObjective(), false /*considerTransportPriorities*/);
        Operation op = opsApiInstance.getOperationStatus(createdPlan.getId());
        assertThat(op, notNullValue());

        final Plan optimizedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(optimizedPlan);

        assertThat(optimizedPlan.getUnplannedTransportIds().isEmpty(), is(true));
        assertThat(optimizedPlan.getRoutes().size(), is(equalTo(1)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().size(), is(equalTo(11)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(0).getLocationId(), is(equalTo("Depot")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(1).getLocationId(), is(equalTo("Location5")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(2).getLocationId(), is(equalTo("Location4")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(3).getLocationId(), is(equalTo("Location3")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(4).getLocationId(), is(equalTo("Location2")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(5).getLocationId(), is(equalTo("Location1")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(6).getLocationId(), is(equalTo("Location0")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(7).getLocationId(), is(equalTo("Location8")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(8).getLocationId(), is(equalTo("Location9")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(9).getLocationId(), is(equalTo("Location7")));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(10).getLocationId(), is(equalTo("Location6")));
    }

    @Test
    void whenPlanWithViolatedMixedLoadingProhibitionIsEvaluatedThenViolationsAreReported() throws Exception {
        Plan initialPlan = TestBase.getPlanWithRoute();
        initialPlan.restrictions(new PlanningRestrictions().addMixedLoadingProhibitionsItem(
                new MixedLoadingProhibition().conflictingLoadCategory1("0").conflictingLoadCategory2("1")
        ));
        initialPlan.getTransports().get(0).loadCategory("0");
        initialPlan.getTransports().get(1).loadCategory("1");

        Plan createdPlan = createPlanViaApi(initialPlan);
        opsApiInstance.startEvaluation(createdPlan.getId());
        final Plan evaluatedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(evaluatedPlan);

        Route route = evaluatedPlan.getRoutes().get(0);
        assertThat(route.getStops().size(), is(equalTo(5)));

        assertThat(route.getStops().get(0).getViolationsAtStop().size(), is(equalTo(1)));
        assertThat(route.getStops().get(0).getViolationsOnWayToStop(), is(empty()));
        assertThat(route.getStops().get(0).getViolationsAtStop().get(0).getType(), is(equalTo(ViolationType.MIXED_LOADING_PROHIBITION)));
        assertThat(route.getStops().get(0).getViolationsAtStop().get(0).getMixedLoadingProhibitions(),
                contains(new MixedLoadingProhibition().conflictingLoadCategory1("0").conflictingLoadCategory2("1")));

        assertThat(route.getStops().get(1).getViolationsAtStop().size(), is(equalTo(1)));
        assertThat(route.getStops().get(1).getViolationsOnWayToStop(), is(empty()));
        assertThat(route.getStops().get(1).getViolationsAtStop().get(0).getType(), is(equalTo(ViolationType.MIXED_LOADING_PROHIBITION)));
        assertThat(route.getStops().get(1).getViolationsAtStop().get(0).getMixedLoadingProhibitions(),
                contains(new MixedLoadingProhibition().conflictingLoadCategory1("0").conflictingLoadCategory2("1")));

        assertThat(route.getStops().get(2).getViolationsAtStop(), is(empty()));
        assertThat(route.getStops().get(2).getViolationsOnWayToStop(), is(empty()));

        assertThat(route.getStops().get(3).getViolationsAtStop(), is(empty()));
        assertThat(route.getStops().get(3).getViolationsOnWayToStop(), is(empty()));

        assertThat(route.getStops().get(4).getViolationsAtStop(), is(empty()));
        assertThat(route.getStops().get(4).getViolationsOnWayToStop(), is(empty()));
    }

    @Test
    void whenGettingOperationForNotExistingPlanIdThenErrorIsReturned() throws Exception {
        ApiException thrown = assertThrows(ApiException.class,
                                () -> opsApiInstance.getOperationStatus(NOT_EXISTING_ID),
                                "Expected getOperationStatus to throw, but it did not");
        ErrorResponse errorResponse = deserialize(thrown.getResponseBody(), ErrorResponse.class);
        assertThat(errorResponse.getErrorCode(), is(equalTo("GENERAL_RESOURCE_NOT_FOUND")));
    }
        
    @Test
    void whenCreatingPlanWithValidAlternativeCapacitiesThenSuccessIsReturned() throws Exception {
        var validPlan = TestBase.getSimpleValidPlan();
        validPlan.getTransports().get(0).quantities(Arrays.asList(0, 4));
        validPlan.getTransports().get(1).quantities(Arrays.asList(4, 0));
        validPlan.getVehicles().get(0).capacities(Arrays.asList(10, 0));
        validPlan.getVehicles().get(0).alternativeCapacities(new ArrayList<>());
        validPlan.getVehicles().get(0).getAlternativeCapacities().add(Arrays.asList( 7, 2 ));
        validPlan.getVehicles().get(0).getAlternativeCapacities().add(Arrays.asList( 5, 4 ));
        for (int i = 0; i < validPlan.getVehicles().get(0).getAlternativeCapacities().size(); ++i)
        {
            assertThat(validPlan.getVehicles().get(0).getAlternativeCapacities().get(i).size(), is(equalTo(2)));
        }
        assertThat(validPlan.getVehicles().get(0).getCapacities().size(), is(equalTo(2)));
        for (int i = 0; i < validPlan.getTransports().size(); ++i)
        {
            assertThat(validPlan.getTransports().get(i).getQuantities().size(), is(equalTo(2)));
        }
        Plan createdPlan = createPlanViaApi(validPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), OptimizationQuality.HIGH, getStandardTweaksToObjective(), false );

        final Plan optimizedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(optimizedPlan);

        assertThat(optimizedPlan.getUnplannedTransportIds(), is(empty()));
        assertThat(optimizedPlan.getRoutes().size(), is(equalTo(1)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().size(), is(equalTo(3)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(0).getReportForStop().getAlternativeCapacitiesIndex(), is(equalTo(1)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(1).getReportForStop().getAlternativeCapacitiesIndex(), is(equalTo(1)));
        assertThat(optimizedPlan.getRoutes().get(0).getStops().get(2).getReportForStop().getAlternativeCapacitiesIndex(), is(nullValue()));
    }
        
    @Test
    void whenCreatingPlanWithValidWorkingHourRulesThenSuccessIsReturned() throws Exception {
        final int h = 3600;
        var validPlan = TestBase.createInputPlan(false);
        Driver driver = createNewDriver("DriverId", "Vehicle1");
        driver.breakRule(new BreakRule().breakTime(1 * h).maximumWorkingTimeBetweenBreaks(5 * h).maximumDrivingTimeBetweenBreaks(4 * h));
        driver.dailyRestRule(new DailyRestRule().dailyRestTime(10 * h).maximumTravelTimeBetweenDailyRests(20 * h));
        driver.maximumDrivingTime(20 * h);
        validPlan.setDrivers(new ArrayList<>());
        validPlan.getDrivers().add(driver);

        for (var t : validPlan.getTransports()) {
            t.deliveryServiceTime(2 * h);
        }

        Plan createdPlan = createPlanViaApi(validPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), OptimizationQuality.HIGH, getStandardTweaksToObjective(), false );

        final Plan optimizedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(optimizedPlan);

        assertThat(optimizedPlan.getUnplannedTransportIds().isEmpty(), is(true));
        assertThat(optimizedPlan.getRoutes().size(), is(equalTo(1)));
        assertThat(optimizedPlan.getRoutes().get(0).getReport().getDrivingTime(), is(lessThan(20 * h)));
        assertThat(optimizedPlan.getRoutes().get(0).getReport().getRestTime(), is(equalTo(10 * h)));
        assertThat(optimizedPlan.getRoutes().get(0).getReport().getBreakTime(), is(greaterThan(1 * h)));
    }
        
    @Test
    void whenCreatingPlanWithValidPresetAndExhaustedWorkLogbookThenSuccessIsReturned() throws Exception {
        final int h = 3600;
        var validPlan = TestBase.createInputPlan(false);
        Driver driver = createNewDriver("DriverId", "Vehicle1");
        driver.workingHoursPreset(WorkingHoursPreset.EU_DRIVING_TIME_REGULATION_FOR_MULTIPLE_DAYS);
        driver.maximumTravelTime(13 * h);
        driver.workLogbook(new WorkLogbook()
            .lastTimeTheDriverWorked(OffsetDateTime.of(2016, 12, 6, 0, 0, 0, 0, ZoneOffset.ofTotalSeconds(0)))
            .accumulatedTravelTimeSinceLastDailyRest(13 * h));
        
        validPlan.setDrivers(new ArrayList<>());
        validPlan.getDrivers().add(driver);

        for (var t : validPlan.getTransports()) {
            t.deliveryServiceTime(2 * h);
        }

        Plan createdPlan = createPlanViaApi(validPlan);

        opsApiInstance.startOptimization(createdPlan.getId(), OptimizationQuality.HIGH, getStandardTweaksToObjective(), false );

        final Plan optimizedPlan = getPlanAfterOperation(createdPlan.getId());
        checkPlan(optimizedPlan);

        assertThat(optimizedPlan.getUnplannedTransportIds().isEmpty(), is(false));
        assertThat(optimizedPlan.getRoutes().isEmpty(), is(false));
    }

}

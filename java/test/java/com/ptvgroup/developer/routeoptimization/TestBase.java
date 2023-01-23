package com.ptvgroup.developer.routeoptimization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import java.lang.Math;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ptvgroup.developer.client.routeoptimization.JSON;
import com.ptvgroup.developer.client.routeoptimization.Configuration;
import com.ptvgroup.developer.client.routeoptimization.api.OperationsApi;
import com.ptvgroup.developer.client.routeoptimization.api.PlansApi;
import com.ptvgroup.developer.client.routeoptimization.model.Driver;
import com.ptvgroup.developer.client.routeoptimization.model.Location;
import com.ptvgroup.developer.client.routeoptimization.model.LocationType;
import com.ptvgroup.developer.client.routeoptimization.model.Operation;
import com.ptvgroup.developer.client.routeoptimization.model.OperationStatus;
import com.ptvgroup.developer.client.routeoptimization.model.OptimizationQuality;
import com.ptvgroup.developer.client.routeoptimization.model.Plan;
import com.ptvgroup.developer.client.routeoptimization.model.Route;
import com.ptvgroup.developer.client.routeoptimization.model.Stop;
import com.ptvgroup.developer.client.routeoptimization.model.TimeInterval;
import com.ptvgroup.developer.client.routeoptimization.model.Transport;
import com.ptvgroup.developer.client.routeoptimization.model.TweakToObjective;
import com.ptvgroup.developer.client.routeoptimization.model.Vehicle;


public class TestBase {

    public static final String API_KEY = "YTkwZGQ2OWQzZjRiNDQ5MWI2ZjMyNWM2MDExMzIxMmU6MTgzYTMxNTQtNTk5NS00MjQ3LTkyMzktM2JhOGE0NjMzOTM2";
    public static PlansApi plansApiInstance;
    public static OperationsApi opsApiInstance;
    public static ArrayList<UUID> planIdsCreated;
    public static final OffsetDateTime PlanningDate = OffsetDateTime.of(2016, 12, 6, 0, 0, 0, 0, ZoneOffset.ofTotalSeconds(0));
    public static final UUID NOT_EXISTING_ID = UUID.fromString("01234567-ABCD-EF01-2345-6789ABCDEF01");

    @BeforeAll
    static void setup() {
        var apiClient = Configuration.getDefaultApiClient()
                .setRequestInterceptor(builder -> builder.setHeader("ApiKey", API_KEY));
        /*.setRequestInterceptor(builder -> builder.setHeader("ApiKey", API_KEY).setHeader("ptv-user-id", "ffffffff-ffff-ffff-ffff-ffffffffffff")
                .setHeader("ptv-pi-id", "ffffffffffffffffffffffffffffffff")
                .setHeader("ptv-profile", "{\"apps\":[\"api-developer-rtopt\"]}"));*/
        plansApiInstance = new PlansApi(apiClient);        
        opsApiInstance = new OperationsApi(apiClient);
        planIdsCreated = new ArrayList<UUID>();
    }

    @AfterAll
    static void teardown() {
        for (final UUID planId: planIdsCreated) {
            try {
                opsApiInstance.cancelOperation(planId);
            } catch (Exception e) {
                //do nothing
            }

            try {
                plansApiInstance.deletePlan(planId);
            } catch (Exception e) {
                //do nothing
            }
        }
    }

    public static OptimizationQuality getStandardOptimizationQuality() {
        return OptimizationQuality.STANDARD;
    }

    public static List<TweakToObjective> getStandardTweaksToObjective() {
        return null;
    }

    public static Driver createNewDriver(String id, String vehicleId) {
        Driver dr = new Driver().id(id).vehicleId(vehicleId);
        return dr;
    }

    public static Location createNewLocation(String id, Double latitude, Double longitude, LocationType type) {
        Location loc = new Location();
        loc.setId(id);
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        loc.setType(type);

        return loc;
    }

    public static Location createNewCustomer(String id, Double latitude, Double longitude, int openingStartInMinutes, int openingDurationInSeconds) {
        Location loc = createNewLocation(id, latitude, longitude, LocationType.CUSTOMER);
        List<TimeInterval> timeIntervals = new ArrayList<TimeInterval>();
        timeIntervals.add(new TimeInterval().start(PlanningDate.plusMinutes(openingStartInMinutes))
                                            .end(PlanningDate.plusMinutes(openingStartInMinutes).plusSeconds(openingDurationInSeconds)));
        
        loc.setOpeningIntervals(timeIntervals);
        return loc;
    }

    public static Location createNewDepot(String id, Double latitude, Double longitude) {
        Location loc = createNewLocation(id, latitude, longitude, LocationType.DEPOT);
        List<TimeInterval> timeIntervals = new ArrayList<TimeInterval>();
        timeIntervals.add(new TimeInterval().start(OffsetDateTime.of(2016, 12, 6, 8, 0, 0, 0, ZoneOffset.ofTotalSeconds(0)))
                                            .end(OffsetDateTime.of(2016, 12, 6, 18, 0, 0, 0, ZoneOffset.ofTotalSeconds(0))));

        loc.setOpeningIntervals(timeIntervals);
        return loc;
    }

    public static Location createNewDepotA() {
        return createNewDepot("DepotA", 49.60804, 6.113033);
    }

    public static Location createNewDepotB() {
        return createNewDepot("DepotB", 49.593632, 6.138353);
    }


    public static Location createNewCustomer(String id, Double latitude, Double longitude) {
        return createNewCustomer(id, latitude, longitude, 10*60, 7200);
    }

    public static Route createNewRoute(String vehicleId, List<Stop> stops){
        Route rt = new Route().vehicleId(vehicleId).stops(stops);
        return rt;
    }

    public static Stop createNewStop(String locationId, List<String> pickupIds, List<String> deliveryIds, String tripId) {
        Stop st = new Stop().locationId(locationId);
        st.setPickupIds(pickupIds);
        st.setDeliveryIds(deliveryIds);
        st.setTripId(tripId);

        return st;
    }

    public static Transport createNewTransport(String id, String pickupLocationId, String deliveryLocationId) {
        Transport tr = new Transport();
        tr.setId(id);
        tr.setPickupLocationId(pickupLocationId);
        tr.setDeliveryLocationId(deliveryLocationId);

        return tr;
    }

    public static Vehicle createNewVehicle(String id, String profile) {
        Vehicle veh = new Vehicle().id(id).profile(profile);
        return veh;
    }

    public static Vehicle createNewVehicle(String id, String startEndLocationId, int capacity) {
        Vehicle veh = new Vehicle().id(id);
        veh.setStartLocationId(startEndLocationId);
        veh.setEndLocationId(startEndLocationId);
        veh.setProfile("EUR_TRUCK_40T");
        veh.setCapacities(new ArrayList<Integer>(Arrays.asList(capacity)));

        return veh;
    }

    protected static Plan getSimpleValidPlan () {
        Plan svp = new Plan();

        final OffsetDateTime now = OffsetDateTime.now();
        svp.setPlanningHorizon(new TimeInterval()
                .start(now)
                .end(now.plus(Duration.ofDays(13))));
        
        List<Location> locations = new ArrayList<Location>();
        locations.add(createNewLocation("Depot", 49.6, 6.1, LocationType.DEPOT));
        locations.add(createNewLocation("Customer1", 49.7, 6.2, LocationType.CUSTOMER));
        locations.add(createNewLocation("Customer2", 49.8, 6.3, LocationType.CUSTOMER));
        svp.setLocations(locations);

        List<Driver> drivers = new ArrayList<Driver>();
        drivers.add(createNewDriver("DriverId", "4711"));
        svp.setDrivers(drivers);

        List<Transport> transports = new ArrayList<Transport>();
        transports.add(createNewTransport("76131", "Depot", "Customer1"));
        transports.add(createNewTransport("76132", "Depot", "Customer2"));
        svp.setTransports(transports);

        List<Stop> stops = new ArrayList<Stop>();
        stops.add(createNewStop("Depot", new ArrayList<String>(Arrays.asList("76131")), null, "trip0"));
        stops.add(createNewStop("Customer1", null, new ArrayList<String>(Arrays.asList("76131")), "trip0"));
        List<Route> routes = new ArrayList<Route>(Arrays.asList(createNewRoute("4711", stops)));
        svp.setRoutes(routes);

        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        vehicles.add(createNewVehicle("4711", "EUR_TRUCK_40T"));
        svp.setVehicles(vehicles);

        return svp;
    }

    protected static Plan getPlanWithRoute()
    {
        Plan plan = new Plan();

        final OffsetDateTime now = OffsetDateTime.now();
        plan.setPlanningHorizon(new TimeInterval()
                .start(now)
                .end(now.plus(Duration.ofDays(13))));

        List<Location> locations = new ArrayList<Location>();
        locations.add(createNewLocation("Depot", 49.6, 6.1, LocationType.DEPOT));
        locations.add(createNewLocation("Customer1", 49.6, 6.1, LocationType.CUSTOMER));
        locations.add(createNewLocation("Customer2", 49.7, 6.2, LocationType.CUSTOMER));
        locations.add(createNewLocation("Customer3", 49.8, 6.3, LocationType.CUSTOMER));
        plan.setLocations(locations);

        List<Transport> transports = new ArrayList<Transport>();
        transports.add(createNewTransport("PDOrder1", "Depot", "Customer1"));
        transports.add(createNewTransport("PDOrder2", "Customer2", "Depot"));
        transports.add(createNewTransport("PDOrder3", "Customer3", "Depot"));
        plan.setTransports(transports);

        List<Vehicle> vehicles = new ArrayList<Vehicle>();
        vehicles.add(createNewVehicle("4711", "EUR_TRUCK_40T"));
        plan.setVehicles(vehicles);

        List<Stop> stops = new ArrayList<Stop>();
        stops.add(createNewStop("Depot", new ArrayList<String>(Arrays.asList("PDOrder1")), null, "trip0"));
        stops.add(createNewStop("Customer2", new ArrayList<String>(Arrays.asList("PDOrder2")), null, "trip0"));
        stops.add(createNewStop("Customer1", null, new ArrayList<String>(Arrays.asList("PDOrder1")), "trip0"));
        stops.add(createNewStop("Customer3", new ArrayList<String>(Arrays.asList("PDOrder3")), null,"trip0"));
        stops.add(createNewStop("Depot", null, new ArrayList<String>(Arrays.asList("PDOrder2", "PDOrder3")), "trip0"));
        List<Route> routes = new ArrayList<Route>(Arrays.asList(createNewRoute("4711", stops)));
        plan.setRoutes(routes);

        return plan;
    }
    
    protected static Coordinate getRandomCoordinates(Coordinate baseCoordinate, double r)
    {
        Random randomno = new Random();
        // Convert Radius from meters to degrees.
        var rd = r / 111300;

        var w = rd * Math.sqrt(randomno.nextDouble());
        var t = 2 * Math.PI * randomno.nextDouble();
        var x = w * Math.cos(t);
        var y = w * Math.sin(t);

        var xp = x / Math.cos(baseCoordinate.latitude);

        // Resulting point.
        return new Coordinate(
            y + baseCoordinate.latitude,
            xp + baseCoordinate.longitude
        );
    }
    

    protected static Plan createInputPlan(boolean withMultiDepot)
    {
        var center = new Coordinate(49.610000, 6.125000);

        var locations = new ArrayList<Location>();
        locations.add(new Location()
              .latitude(center.latitude)
              .longitude(center.longitude)
              .type(LocationType.DEPOT)
              .id("Depot")
              .openingIntervals(new ArrayList<TimeInterval>(Arrays.asList(new TimeInterval()
                .start(OffsetDateTime.of(2016, 12, 6, 0, 0, 0, 0, ZoneOffset.ofTotalSeconds(0)))
                .end(OffsetDateTime.of(2016, 12, 10, 0, 0, 0, 0, ZoneOffset.ofTotalSeconds(0)))))));
            
        var transports = new ArrayList<Transport>();
        // add random transports
        for (int i = 0; i < 10; i++)
        {
            var coordinates = getRandomCoordinates(center, 10000.0); // 10 km
            locations.add(createNewLocation("Location" + i, coordinates.latitude, coordinates.longitude, (withMultiDepot && i == 2) ? LocationType.DEPOT : LocationType.CUSTOMER));
            transports.add(createNewTransport("Transport" + i, "Depot", "Location" + i));
        }
        
        var vehicles = new ArrayList<Vehicle>();
        vehicles.add(new Vehicle ().id("Vehicle1"));
        
        var stops = new ArrayList<Stop>();
        stops.add(new Stop()
                  .locationId("Depot")
                  .pickupIds(new ArrayList<String>(Arrays.asList("Transport0" ))));
        stops.add(new Stop()
                  .locationId("Location0")
                  .deliveryIds(new ArrayList<String>(Arrays.asList("Transport0" ))));
                  
        var routes = new ArrayList<Route>();
        routes.add(new Route()
              .vehicleId("Vehicle1")
              .stops(stops));

        var plan = new Plan()
          .locations(locations)
          .transports(transports)
          .vehicles(vehicles)
          .routes(routes);
        return plan;
    }
    
    protected static Plan createPlanViaApi(Plan inputPlan) throws Exception {
        Plan pl = plansApiInstance.createPlan(inputPlan);
        assertThat(pl instanceof Plan, is(true));
        assertThat(pl.getId(), notNullValue());
        assertThat(pl.getId().equals(new UUID(0, 0)), is(false));

        planIdsCreated.add(pl.getId());
        return pl;
    }

    protected static Plan getPlanAfterOperation(UUID planId) throws Exception {
        assertThat(planId, notNullValue());
        Operation op = opsApiInstance.getOperationStatus(planId);

        OffsetDateTime timeBefore = OffsetDateTime.now();
        OffsetDateTime timeNow = OffsetDateTime.now();

        while (op.getStatus() == OperationStatus.RUNNING && timeNow.isBefore(timeBefore.plusMinutes(5)))
        {
            Thread.sleep(5000);
            op = opsApiInstance.getOperationStatus(planId);
            timeNow = OffsetDateTime.now();
        }
        if (op.getStatus() == OperationStatus.RUNNING)
        {
            throw new TimeoutException("Timeout after 5 Minutes.");
        }

        assertThat(op.getError(), nullValue());
        Plan returnPlan = plansApiInstance.getPlan(planId);
        assertThat(returnPlan, notNullValue());

        return returnPlan;
    }

    protected static Route getRouteOfVehicle(Plan plan, String vehicleId) {
        assertThat(plan.getRoutes(), notNullValue());
        assertThat(plan.getRoutes().isEmpty(), is(false));

        Route route = null;
        for (Route rt : plan.getRoutes()) {
            if (rt.getVehicleId().equals(vehicleId)) {
                route = rt;
                break;
            }
        }

        return route;
    }

    protected static void checkPlan(Plan plan) throws Exception {
        assertThat(plan, notNullValue());
        assertThat(plan.getVehicles().isEmpty(), is(false));
        assertThat(plan.getRoutes().isEmpty(), is(false));

        Route routeOfVehicle0 = getRouteOfVehicle(plan, plan.getVehicles().get(0).getId());
        assertThat(routeOfVehicle0, notNullValue());

        assertThat(plan.getLocations(), notNullValue());
        assertThat(plan.getLocations().isEmpty(), is(false));

        Set<String> depotIds = new HashSet<String>();
        plan.getLocations().forEach(loc -> {if (loc.getType() == LocationType.DEPOT) depotIds.add(loc.getId());});

        Set<String> locationIdsInRoute = new HashSet<String>();
        assertThat(routeOfVehicle0.getStops(), notNullValue());
        assertThat(routeOfVehicle0.getStops().isEmpty(), is(false));
        for (final Stop st: routeOfVehicle0.getStops()) {
            assertThat(st, notNullValue());
            assertThat(st.getLocationId(), notNullValue());
            locationIdsInRoute.add(st.getLocationId());
        }

        assertThat(locationIdsInRoute.containsAll(depotIds), is(true));
    }


    public <T> T deserialize(String jsonString, Class<T> target) throws JsonProcessingException {
        return JSON.getDefault().getMapper().readValue(jsonString, target);
    }

    public static class Pair<T, U> {         
        public final T t;
        public final U u;
    
        public Pair(T t, U u) {         
            this.t= t;
            this.u= u;
         }
     }

    // lat/lng to mercator
    protected static Pair<Double, Double> wgs2SphereMercator(Pair<Double, Double> point)
    {
        Double lng = point.u * Math.PI / 180.0;
        Double lat = Math.log(Math.tan(Math.PI / 4.0 + point.t * Math.PI / 360.0));

        return new Pair<Double, Double>(lat, lng);
    }

    // mercator to lat/lng
    protected static Pair<Double, Double> sphereMercator2Wgs(Pair<Double, Double> point)
    {
        Double lng = 180.0 / Math.PI * (point.u);
        Double lat = 360 / Math.PI * (Math.atan(Math.exp(point.t)) - Math.PI / 4);

        return new Pair<Double, Double>(lat, lng);
    }
}
    
class Coordinate {
    public double longitude;
    public double latitude;
    public Coordinate(double latitude, double longitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
}

/*
 * ******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc. and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *
 * ******************************************************************************
 */
package com.redhat.iot.cargodemo.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.iot.cargodemo.model.Customer;
import com.redhat.iot.cargodemo.model.Facility;
import com.redhat.iot.cargodemo.model.LatLng;
import com.redhat.iot.cargodemo.model.Operator;
import com.redhat.iot.cargodemo.model.Shipment;
import com.redhat.iot.cargodemo.model.Summary;
import com.redhat.iot.cargodemo.model.Telemetry;
import com.redhat.iot.cargodemo.model.Vehicle;
import com.redhat.iot.cargodemo.service.DGService;

/**
 * A simple REST service which proxies requests to a local datagrid.
 */

@Path("/utils")
@Singleton
public class UtilsEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(UtilsEndpoint.class);
	
    public static final int MAX_VEHICLES = 2;
    public static final int MAX_PACKAGES_PER_VEHICLE = 1;
    public static final long DAY_IN_MS = 24*60*60*1000;

    @Inject
    DGService dgService;

    @GET
    @Path("/health")
    public String health() {
        return "ok";
    }

    @POST
    @Path("/resetAll")
    public void resetAll() {
    	boolean simBalance = false;
    	String[] strnums = null;
    	//boolean createDynamicSimulation = false;
    	logger.warn("TIM IS HERE .... TRACE THIS!!!!");
    	String simulator = System.getenv("ADDITIONAL_SENSOR_IDS");
    	logger.warn("Simulator Data = " + simulator);
    	
    	if(simulator != null && !simulator.equals("")){
    		logger.warn("Simulator data is set and will be parsed");  
    		if(simulator.startsWith("SIM:")){
    			logger.warn("Need to parse the sim data here");
    		}
    		
    		//Extract the number list ...
    		String numbers = simulator.substring("SIM:".length());
    		logger.warn("The String to work with = " + numbers);
    		//Convert number list in number Array List
    		strnums = numbers.split(":");
    		//Check size value and adjust if needed
    		if(strnums.length > Integer.valueOf(strnums[0])){
    			logger.warn("The number of trucks and packages do balance");
    			simBalance=true;
    			logger.warn("Will create the following:");
    			logger.warn("#Trucks: " + strnums[0]);
    			for(int i = 1; i < strnums.length;i++){
    				logger.warn("# Packages on Truck " + i + " = " + strnums[i]);
    			}
    		}
    		else{
    			logger.warn("Error in the Simulator data ---- will use default simulation pupulation");
    		}  		
    	}

        Map<String, Vehicle> vehiclesCache = dgService.getVehicles();
        Map<String, Customer> customerCache = dgService.getCustomers();
        Map<String, Facility> facilitiesCache = dgService.getFacilities();
        Map<String, Operator> operatorCache = dgService.getOperators();
        Map<String, Shipment> shipmentCache = dgService.getShipments();


        facilitiesCache.clear();
        vehiclesCache.clear();
        customerCache.clear();
        operatorCache.clear();
        shipmentCache.clear();

        for (String COMPANY : COMPANIES) {
            customerCache.put(COMPANY,
                    new Customer(COMPANY, "password"));
        }

        for (String oper : OPERATOR_NAMES){
            operatorCache.put(oper, new Operator(oper, "password"));
        }

        List<String> facs = new ArrayList<String>();
        facs.addAll(Arrays.asList(ORIGINS));
        facs.addAll(Arrays.asList(DESTS));

        for (String facName : facs) {

            facilitiesCache.put(facName,
                    new Facility(facName, facName,
                            new LatLng(-80, 20),
                            Math.random() * 1000.0));

        }
        
        if(simBalance){
        	insertSimulatedRandomData(facilitiesCache, customerCache, vehiclesCache, shipmentCache, strnums);
        }
        else{
        	insertFullRandomData(facilitiesCache, customerCache, vehiclesCache, shipmentCache);
        }
       
        calcUtilization();

    }
    
    private void insertSimulatedRandomData(Map<String, Facility> facilitiesCache, Map<String, Customer> customerCache,
			Map<String, Vehicle> vehiclesCache, Map<String, Shipment> shipmentCache, String[] strnums) {
		// TODO Auto-generated method stub
    	int [] numbers = new int[strnums.length];
    	for (int i = 0; i < strnums.length; i++) {
			numbers[i] = Integer.valueOf(strnums[i]);
		}
    	
    	//# Vehicles = [0]
    	//# Packages = [>0] and is balanced or bigger as this is checked before ...
    	
    	for(int i = 1; i <= numbers[0]; i++){
    		logger.warn("Creating Vehicle: " + i);
    		String vin = "truck-" + i;

            Vehicle newVehicle = new Vehicle(vin, rand(VEHICLE_TYPES));

            Facility v_origin = facilitiesCache.get(rand(ORIGINS));
            Facility v_dest = facilitiesCache.get(rand(DESTS));

            newVehicle.setOrigin(v_origin);
            newVehicle.setDestination(v_dest);

            List<Telemetry> vehicleTelemetry = new ArrayList<>();
            vehicleTelemetry.add(new Telemetry("°C", 250.0, 150.0, "Engine Temp", "temp"));
            vehicleTelemetry.add(new Telemetry("rpm", 2200.0, 500.0, "RPM", "rpm"));
            vehicleTelemetry.add(new Telemetry("psi", 80.0, 30.0, "Oil Pressure", "oilpress"));
            newVehicle.setTelemetry(vehicleTelemetry);

            Date v_eta = new Date(new Date().getTime() + DAY_IN_MS + (long)(Math.random() * DAY_IN_MS * 2));

            newVehicle.setEta(v_eta);
            vehiclesCache.put(vin, newVehicle);
            
            //Creating packages per vehicle
            //As the index is 1 bound instead of 0 bound I can just get the i parameter to reference the number of packages.
            
            for(int j = 1; j<= numbers[i]; j++){
            	logger.warn("Creating package " + j + " for truck: " + i );
            	addShipment(customerCache, facilitiesCache, shipmentCache, vin, newVehicle, v_dest, "pkg-" + i + "_" + j,
                        rand(PKG_DESCS));
            }
            
    		
    	}
		
	}

	private void insertFullRandomData(Map<String, Facility>facilitiesCache, Map<String, Customer> customerCache, Map<String, Vehicle>vehiclesCache, Map<String, Shipment>shipmentCache){
        for (int i = 1; i <= MAX_VEHICLES; i++) {

            String vin = "truck-" + i;

            Vehicle newVehicle = new Vehicle(vin, rand(VEHICLE_TYPES));

            Facility v_origin = facilitiesCache.get(rand(ORIGINS));
            Facility v_dest = facilitiesCache.get(rand(DESTS));

            newVehicle.setOrigin(v_origin);
            newVehicle.setDestination(v_dest);

            List<Telemetry> vehicleTelemetry = new ArrayList<>();
            vehicleTelemetry.add(new Telemetry("°C", 250.0, 150.0, "Engine Temp", "temp"));
            vehicleTelemetry.add(new Telemetry("rpm", 2200.0, 500.0, "RPM", "rpm"));
            vehicleTelemetry.add(new Telemetry("psi", 80.0, 30.0, "Oil Pressure", "oilpress"));
            newVehicle.setTelemetry(vehicleTelemetry);

            Date v_eta = new Date(new Date().getTime() + DAY_IN_MS + (long)(Math.random() * DAY_IN_MS * 2));

            newVehicle.setEta(v_eta);
            vehiclesCache.put(vin, newVehicle);

            for (int j = 1; j <= MAX_PACKAGES_PER_VEHICLE; j++) {

                addShipment(customerCache, facilitiesCache, shipmentCache, vin, newVehicle, v_dest, "pkg-" + j,
                        rand(PKG_DESCS));
            }

            // Add additional sensor ids if present
            String addl = System.getenv("ADDITIONAL_SENSOR_IDS");
            if (addl != null) {
                String[] ids = addl.split(",");
                for (String id: ids) {
                    addShipment(customerCache, facilitiesCache, shipmentCache, vin, newVehicle, v_dest, id.trim(),
                            rand(PKG_DESCS) + " [" + id.trim() + "]");
                }
            }

        }
    }

    private void addShipment(Map<String, Customer> customerCache, Map<String, Facility> facilitiesCache,
                             Map<String, Shipment> shipmentCache, String vin, Vehicle v, Facility v_dest, String sensor_id,
                             String pkgDesc) {
        List<Facility> route = new ArrayList<Facility>();

        Facility p_origin = facilitiesCache.get(rand(ORIGINS));
        Facility p_dest = facilitiesCache.get(rand(DESTS));

        route.add(p_origin);
        route.add(v_dest);
        route.add(p_dest);

        List<Telemetry> telemetry = new ArrayList<>();
        telemetry.add(new Telemetry("°C", 40.0, 0.0, "Temperature", "Ambient"));
        telemetry.add(new Telemetry("%", 100.0, 0.0, "Humidity", "Humidity"));
        telemetry.add(new Telemetry("lm", 400.0, 100.0, "Light", "Light"));
        telemetry.add(new Telemetry("inHg", 31, 29, "Pressure", "Pressure"));

        Customer cust = customerCache.get(rand(COMPANIES));

        // left ~3 days, ago eta ~5 days from now
        Date etd = new Date(new Date().getTime() - DAY_IN_MS - (long)(Math.random() * DAY_IN_MS * 3));
        Date eta = new Date(new Date().getTime() + DAY_IN_MS + (long)(Math.random() * DAY_IN_MS * 4));

        Shipment s = new Shipment(customerCache.get(rand(COMPANIES)),
                "Package " + sensor_id + "/" + vin, pkgDesc,
                sensor_id, route, etd, eta, Math.random() * 2000, v);

        s.setTelemetry(telemetry);
        shipmentCache.put(sensor_id + "/" + vin, s);
    }

    private String rand(String[] strs) {
        return strs[(int)Math.floor(Math.random() * strs.length)];
    }

    private void calcUtilization() {
        Map<String, Facility> facCache = dgService.getFacilities();
        Map<String, Shipment> shipCache = dgService.getShipments();

        Map<String, Integer> facCount = new HashMap<>();

        int total = 0;

        for (String s1 : shipCache.keySet()) {
            Shipment s = shipCache.get(s1);
            for (Facility f : s.getRoute()) {
                total++;
                if (facCount.containsKey(f.getName())) {
                    facCount.put(f.getName(), facCount.get(f.getName()) + 1);
                } else {
                    facCount.put(f.getName(), 1);
                }
            }
        }

        for (String s1 : facCache.keySet()) {
            Facility f = facCache.get(s1);
            if (!facCount.containsKey(f.getName())) {
                f.setUtilization(0);
            } else {
                f.setUtilization(2.5 * ((double) facCount.get(f.getName()) / (double) total));
            }
            facCache.put(f.getName(), f);
        }
    }

    @PUT
    @Path("/{id}")
    public void put(@PathParam("id") String id, Vehicle value) {
        dgService.getVehicles().put(id, value);
    }

    @GET
    @Path("/summaries")
    @Produces({"application/json"})
    public List<Summary> getSummaries() {

        List<Summary> result = new ArrayList<>();

        Summary vehicleSummary = getVehicleSummary();
        Summary clientSummary = getClientSUmmary();
        Summary packageSummary = getPackageSummary();
        Summary facilitySummary = getFacilitySummary();
        Summary operatorSummary = getOperatorSummary();

        result.add(clientSummary);
        result.add(packageSummary);
        result.add(vehicleSummary);
        result.add(operatorSummary);
        result.add(facilitySummary);

        Summary mgrs = new Summary();
        mgrs.setName("fake");
        mgrs.setTitle("Managers");
        mgrs.setCount(23);
        mgrs.setWarningCount(4);
        mgrs.setErrorCount(1);
        result.add(mgrs);
        return result;
    }

    private Summary getOperatorSummary() {
        Map<String, Operator> cache = dgService.getOperators();

        Summary summary = new Summary();
        summary.setName("operators");
        summary.setTitle("Operators");
        summary.setCount(cache.keySet().size());

        return summary;

    }

    private Summary getFacilitySummary() {
        Map<String, Facility> cache = dgService.getFacilities();

        Summary summary = new Summary();
        summary.setName("facilities");
        summary.setTitle("Facilities");
        summary.setCount(cache.keySet().size());

        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> v.getUtilization() < .7 && v.getUtilization() > .5)
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> v.getUtilization() < .5)
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);

        return summary;
    }

    private Summary getPackageSummary() {
        Map<String, Shipment> cache = dgService.getShipments();

        Summary summary = new Summary();
        summary.setName("packages");
        summary.setTitle("Packages");
        summary.setCount(cache.keySet().size());


        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> "warning".equalsIgnoreCase(v.getStatus()))
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> "error".equalsIgnoreCase(v.getStatus()))
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);
        return summary;

    }

    private Summary getClientSUmmary() {
        Map<String, Customer> cache = dgService.getCustomers();

        Summary summary = new Summary();
        summary.setName("clients");
        summary.setTitle("Clients");
        summary.setCount(cache.keySet().size());
        return summary;

    }

    private Summary getVehicleSummary() {
        Map<String, Vehicle> cache = dgService.getVehicles();

        Summary summary = new Summary();
        summary.setName("vehicles");
        summary.setTitle("Vehicles");
        summary.setCount(cache.keySet().size());


        long warningCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> "warning".equalsIgnoreCase(v.getStatus()))
                .count();

        long errorCount = cache.keySet().stream()
                .map(cache::get)
                .filter(v -> "error".equalsIgnoreCase(v.getStatus()))
                .count();

        summary.setWarningCount(warningCount);
        summary.setErrorCount(errorCount);
        return summary;
    }


    public static final String[] COMPANIES = new String[]{
            "Wonka Industries",
            "Acme Corp",
            "Stark Industries",
            "Ollivander's Wand Shop",
            "Gekko & Co",
            "Wayne Enterprises",
            "Cyberdyne Systems",
            "Cheers",
            "Genco Pura",
            "NY Enquirer",
            "Duff Beer",
            "Bubba Gump Shrimp Co",
            "Olivia Pope & Associates",
            "Sterling Cooper",
            "Soylent",
            "Hooli",
            "Good Burger"
    };

    public static final String[] ORIGINS = new String[]{
            "Winter Springs, FL",
            "Raleigh, NC",
            "Westford, MA",
            "Atlanta, GA",
            "Charleston, SC",
            "Tarboro, NC",
            "Huntsville, AL",
            "Knoxville, TN",
            "Showshoe, WV",
            "Washington, D.C.",
            "Virginia Beach, VA",
            "New York, NY",
            "Jacksonville, FL"
    };

    public static final String[] DESTS = new String[]{
            "Chatanooga, TN",
            "Louisville, KY",
            "Omaha, NE",
            "Chicago, IL",
            "Des Moines, IA",
            "Lexington, KY",
            "New Orleans, LA",
            "Mobile, AL"
    };

    public static final String[] VEHICLE_TYPES = new String[] {

            "Box truck",
            "Van",
            "Cutaway van chassis",
            "Medium Duty Truck such as Ford F-650 in North America",
            "Medium Standard Truck",
            "Platform truck",
            "Flatbed truck (may also be light duty trucks)",
            "Firetruck (may also be a heavy truck)",
            "Recreational Vehicle or Motorhome",
            "Concrete transport truck (cement mixer)",
            "Mobile crane",
            "Dump truck",
            "Garbage truck",
            "Log carrier",
            "Refrigerator truck",
            "Tractor unit",
            "Tank truck",
            "Heavy Hauler",
            "F-35"
    };

    public static final String[] PKG_DESCS = new String[] {
            "Spare F-22 Parts",
            "Violins",
            "Antique Baseballs",
            "Frozen Cells",
            "Machined Parts",
            "Misc. Assembly Fasteners",
            "Fresh Fruit",
            "Frozen Steaks",
            "Precious Jewels",
            "Optical Hard Drives",
            "Polyjuice Potion",
            "Live Bait"
    };

    public static final String[] OPERATOR_NAMES = new String[]{
            "R. Kint",
            "H. Potter",
            "A. Ventura",
            "H. Lime",
            "S. Kowalski",
            "D. Vader",
            "S. Spade",
            "D. Strangelove",
            "T. Montana",
            "N. Rae",
            "J. Benjamin",
            "A. DeLarge",
            "J. Cousteau",
            "E. Scissorhands",
            "G. Bailey",
            "Lt. Kilgore",
            "T. Dude",
            "F. Booth",
            "F. Kreuger"
    };

}


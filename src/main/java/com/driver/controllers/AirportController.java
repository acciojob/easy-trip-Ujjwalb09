package com.driver.controllers;


import com.driver.model.Airport;
import com.driver.model.City;
import com.driver.model.Flight;
import com.driver.model.Passenger;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
public class AirportController {

    HashMap<String, Airport> airportDb = new HashMap<>();
    HashMap<Integer, Flight> flightDb = new HashMap<>();
    HashMap<Integer, Passenger> passengerDb = new HashMap<>();
    HashMap<Integer, List<Integer>> ticketDb = new HashMap<>();
    HashMap<Integer, Integer> revenueDb = new HashMap<>();
    HashMap<Integer, Integer> passengerPayment = new HashMap<>();

    @PostMapping("/add_airport")
    public String addAirport(@RequestBody Airport airport){

        //Simply add airport details to your database
        //Return a String message "SUCCESS"

        airportDb.put(airport.getAirportName(), airport);

        return "SUCCESS";
    }

    @GetMapping("/get-largest-aiport")
    public String getLargestAirportName(){

        //Largest airport is in terms of terminals. 3 terminal airport is larger than 2 terminal airport
        //Incase of a tie return the Lexicographically smallest airportName

        String ans = "";
        int terminal = 0;

        for(Airport airport: airportDb.values()){
            if(airport.getNoOfTerminals() > terminal){
                ans = airport.getAirportName();
                terminal = airport.getNoOfTerminals();
            } else if(airport.getNoOfTerminals()==terminal){
                 if(airport.getAirportName().compareTo(ans) < 0){
                     ans = airport.getAirportName();
                 }
            }
        }

        return ans;
    }

    @GetMapping("/get-shortest-time-travel-between-cities")
    public double getShortestDurationOfPossibleBetweenTwoCities(@RequestParam("fromCity") City fromCity, @RequestParam("toCity")City toCity){

        //Find the duration by finding the shortest flight that connects these 2 cities directly
        //If there is no direct flight between 2 cities return -1.

        double distance = Integer.MAX_VALUE;

        for(Flight flight : flightDb.values()){
            if(fromCity==flight.getFromCity() && toCity==flight.getToCity()){
                    distance = Math.min(distance, flight.getDuration());
            }
        }

        if(distance==Integer.MAX_VALUE){
            return -1;
        }

       return distance;
    }

    @GetMapping("/get-number-of-people-on-airport-on/{date}")
    public int getNumberOfPeopleOn(@PathVariable("date") Date date,@RequestParam("airportName")String airportName){

        //Calculate the total number of people who have flights on that day on a particular airport
        //This includes both the people who have come for a flight and who have landed on an airport after their flight

        Airport airport = airportDb.get(airportName);

        if(Objects.isNull(airport)) {
            return 0;
        }

        City city = airport.getCity();
        int count = 0;
        for(Flight flights : flightDb.values()){
            if(date.equals(flights.getFlightDate())){
                if(flights.getFromCity().equals(city) || flights.getToCity().equals(city)){
                    int flightId = flights.getFlightId();

                    count = count + ticketDb.get(flightId).size();
                }
            }
        }

        return count;
    }

    @GetMapping("/calculate-fare")
    public int calculateFlightFare(@RequestParam("flightId")Integer flightId){

        //Calculation of flight prices is a function of number of people who have booked the flight already.
        //Price for any flight will be : 3000 + noOfPeopleWhoHaveAlreadyBooked*50
        //Suppose if 2 people have booked the flight already : the price of flight for the third person will be 3000 + 2*50 = 3100
        //This will not include the current person who is trying to book, he might also be just checking price

        if(Objects.isNull(ticketDb.get(flightId).size())){
            return 0;
        }


        int noOfPeopleBooked = ticketDb.get(flightId).size();

        return noOfPeopleBooked*50 + 3000;

    }


    @PostMapping("/book-a-ticket")
    public String bookATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the numberOfPassengers who have booked the flight is greater than : maxCapacity, in that case :
        //return a String "FAILURE"
        //Also if the passenger has already booked a flight then also return "FAILURE".
        //else if you are able to book a ticket then return "SUCCESS"

        //1st case:- checking if flight has reached max capacity or not

        if(Objects.nonNull(ticketDb.get(flightId)) && ticketDb.get(flightId).size() < flightDb.get(flightId).getMaxCapacity()){

            if(ticketDb.get(flightId).contains(passengerId)){ //If passenger has already booked a ticket
                return "FAILURE";
            }

            List<Integer> passengers = ticketDb.get(flightId);
            passengers.add(passengerId);

            int fare = calculateFlightFare(flightId);
            passengerPayment.put(passengerId, fare);
            int revenue = revenueDb.getOrDefault(flightId, 0);
            revenue = revenue + fare;
            revenueDb.put(flightId, revenue);

            ticketDb.put(flightId, passengers);

            return "SUCCESS";


        } else if(Objects.isNull(ticketDb.get(flightId))){ //booking passenger for the first time

            //ticketDb.put(flightId, new ArrayList<>());

            List<Integer> passengers = new ArrayList<>();
            passengers.add(passengerId);

            int fare = calculateFlightFare(flightId);
            passengerPayment.put(passengerId, fare);
            int revenue = revenueDb.getOrDefault(flightId, 0);
            revenue = revenue + fare;
            revenueDb.put(flightId, revenue);

            ticketDb.put(flightId, passengers);

            return "SUCCESS";
        }

        return "FAILURE";

    }

    @PutMapping("/cancel-a-ticket")
    public String cancelATicket(@RequestParam("flightId")Integer flightId,@RequestParam("passengerId")Integer passengerId){

        //If the passenger has not booked a ticket for that flight or the flightId is invalid or in any other failure case
        // then return a "FAILURE" message
        // Otherwise return a "SUCCESS" message
        // and also cancel the ticket that passenger had booked earlier on the given flightId

        if(!ticketDb.containsKey(flightId)){
            return "FAILURE";
        }

        List<Integer> passenger = ticketDb.get(flightId);

        if(!passenger.contains(passengerId)){ //if passenger has not booked a ticket
            return "FAILURE";
        }

        int passengerPaid = passengerPayment.get(passengerId);
        passengerPayment.remove(passengerId);

        int revenue = revenueDb.get(flightId);
        revenue = revenue-passengerPaid;
        revenueDb.put(flightId, revenue);

        ticketDb.get(flightId).remove(passengerId);

       return "SUCCESS";
    }


    @GetMapping("/get-count-of-bookings-done-by-a-passenger/{passengerId}")
    public int countOfBookingsDoneByPassengerAllCombined(@PathVariable("passengerId")Integer passengerId){

        //Tell the count of flight bookings done by a passenger: This will tell the total count of flight bookings done by a passenger :

        int count = 0;

        for(List<Integer> passenger: ticketDb.values()){
            if(passenger.contains(passengerId)) count++;
        }
       return count;
    }

    @PostMapping("/add-flight")
    public String addFlight(@RequestBody Flight flight){

        //Return a "SUCCESS" message string after adding a flight.

        flightDb.put(flight.getFlightId(), flight);

       return "SUCCESS";
    }


    @GetMapping("/get-aiportName-from-flight-takeoff/{flightId}")
    public String getAirportNameFromFlightId(@PathVariable("flightId")Integer flightId){

        //We need to get the starting airport Name from where the flight will be taking off (Hint think of City variable if that can be of some use)
        //return null incase the flightId is invalid or you are not able to find the airportName

        String airportName = "";

        if(!flightDb.containsKey(flightId)){
            return null;
        } else {
            Flight flight = flightDb.get(flightId);

            City city = flight.getFromCity();

            for(Airport airport: airportDb.values()){
                if(airport.getCity().equals(city)){
                    airportName = airport.getAirportName();
                }
            }
        }

        return airportName;
    }


    @GetMapping("/calculate-revenue-collected/{flightId}")
    public int calculateRevenueOfAFlight(@PathVariable("flightId")Integer flightId){

        //Calculate the total revenue that a flight could have
        //That is of all the passengers that have booked a flight till now and then calculate the revenue
        //Revenue will also decrease if some passenger cancels the flight

        return revenueDb.getOrDefault(flightId, 0);
    }


    @PostMapping("/add-passenger")
    public String addPassenger(@RequestBody Passenger passenger){

        //Add a passenger to the database
        //And return a "SUCCESS" message if the passenger has been added successfully.

        passengerDb.put(passenger.getPassengerId(), passenger);

       return "SUCCESS";
    }


}

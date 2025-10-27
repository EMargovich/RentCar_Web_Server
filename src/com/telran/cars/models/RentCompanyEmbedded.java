package com.telran.cars.models;

import java.io.FileInputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import com.telran.cars.dto.*;
import com.telran.cars.dto.enums.CarsReturnCode;
import com.telran.cars.dto.enums.State;
import com.telran.utils.Persistable;
import static com.telran.cars.dto.enums.CarsReturnCode.*;

@SuppressWarnings("serial")
public class RentCompanyEmbedded extends AbstractRentCompany implements Persistable {
	private static final int REMOVE_THRESHOLD = 60;
	private static final int BAD_THRESHOLD = 30;
	private static final int GOOD_THRESHOLD = 10;
//sprint 1
	Map<String, Car> cars = new HashMap<>();// key regNumber
	Map<Long, Driver> drivers = new HashMap<>(); // key license Driver
	Map<String, Model> models = new HashMap<>();// key model name
//sprint 2
	Map<String, List<Car>> modelCars = new HashMap<>();// key model name -> list cars
	Map<Long, List<RentRecord>> driverRecords = new HashMap<>();// key license id
	Map<String, List<RentRecord>> carRecords = new HashMap<>();// key reg number
	Map<LocalDate, List<RentRecord>> records = new TreeMap<>();

//Map<LocalDate, Map<String, List<Car>> availability
	@Override
	public CarsReturnCode addModel(Model model) {

		return models.putIfAbsent(model.getModelName(), model) == null ?
		// CarsReturnCode.OK:CarsReturnCode.MODEL_EXISTS;
				OK : MODEL_EXISTS;
	}

	@Override
	public Model getModel(String modelName) {
		return models.get(modelName);
	}

	@Override
	public CarsReturnCode addCar(Car car) {
		if (!models.containsKey(car.getModelName()))
			return NO_MODEL;
		boolean res = cars.putIfAbsent(car.getRegNumber(), car) == null;
		if (!res)
			return CAR_EXISTS;
		addModelCars(car);
		return OK;
	}

	private void addModelCars(Car car) {
		String modelName = car.getModelName();
		List<Car> listCars = modelCars.getOrDefault(modelName, new ArrayList<>());
		listCars.add(car);
		modelCars.putIfAbsent(modelName, listCars);
	}

	@Override
	public Car getCar(String regNumber) {

		return cars.get(regNumber);
	}

	@Override
	public CarsReturnCode addDriver(Driver driver) {

		return drivers.putIfAbsent(driver.getLicenseId(), driver) == null ? OK : DRIVER_EXISTS;
	}

	@Override
	public Driver getDriver(long licenseId) {

		return drivers.get(licenseId);
	}

	@Override
	public void save(String fileName) {
		try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(fileName))) {
			outputStream.writeObject(this);
		} catch (IOException e) {
			System.out.println("Error in method save " + e.getMessage());
		}

	}

	public static IRentCompany restoreFromFile(String fileName) {
		try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(fileName))) {
			return (IRentCompany) inputStream.readObject();
		} catch (Exception e) {
			System.out.println(fileName + " new object has been created " + e.getMessage());
			return new RentCompanyEmbedded();
		}

	}

	@Override
	public CarsReturnCode rentCar(String regNumber, long licenseId, LocalDate rentDate, int rentDays) {
		Car car = getCar(regNumber);
		if (car == null)
			return NO_CAR;
		if (car.isFlRemoved())
			return CAR_REMOVED;
		if (car.isInUse())
			return CAR_IN_USE;
		if (!drivers.containsKey(licenseId))
			return NO_DRIVER;
		RentRecord record = new RentRecord(regNumber, licenseId, rentDate, rentDays);
		addToCarRecords(record);
		addToDriverRecords(record);
		addToRecords(record);
		car.setInUse(true);
		return OK;
	}

	private void addToDriverRecords(RentRecord record) {
		driverRecords.computeIfAbsent(record.getLicenseId(), k -> new ArrayList<>()).add(record);

	}

	private void addToCarRecords(RentRecord record) {
		carRecords.computeIfAbsent(record.getRegNumber(), k -> new ArrayList<>()).add(record);

	}

	private void addToRecords(RentRecord record) {
//		LocalDate rentDate = record.getRentDate();
//		List<RentRecord> listRecords = records.getOrDefault(rentDate, new ArrayList<>());
//		listRecords.add(record);
//		records.putIfAbsent(rentDate, listRecords);

		records.computeIfAbsent(record.getRentDate(), k -> new ArrayList<>()).add(record);

	}

	@Override
	public List<Car> getCarsByDriver(long licenseId) {
		List<RentRecord> listRecords = driverRecords.getOrDefault(licenseId, new ArrayList<>());
		return listRecords.stream().map(r -> getCar(r.getRegNumber())).distinct().toList();
	}

	@Override
	public List<Driver> getDriversByCar(String regNumber) {
		List<RentRecord> listRecords = carRecords.getOrDefault(regNumber, new ArrayList<>());
		return listRecords.stream().map(r -> getDriver(r.getLicenseId())).distinct().toList();
	}

	@Override
	public List<Car> getCarsByModel(String modelName) {
		List<Car> res = modelCars.getOrDefault(modelName, new ArrayList<>());

		return res.stream().filter(c -> !c.isFlRemoved() && !c.isInUse()).toList();
	}

	@Override
	public List<RentRecord> getRentRecordsAtDate(LocalDate from, LocalDate to) {
		Collection<List<RentRecord>> res = ((TreeMap<LocalDate, List<RentRecord>>) records).subMap(from, to).values();
		return res.stream().flatMap(l -> l.stream()).toList();
	}

	@Override
	public RemovedCarData removeCar(String regNumber) {
		Car car = getCar(regNumber);
		if (car == null || car.isFlRemoved())
			return null;
		car.setFlRemoved(true);
		return car.isInUse() ? new RemovedCarData(car, null) : actualCarRemove(car);
	}

	private RemovedCarData actualCarRemove(Car car) {
		String regNumber = car.getRegNumber();
		List<RentRecord> removedRecords = carRecords.get(regNumber);
		if (removedRecords != null && !removedRecords.isEmpty()) {
			removeFromDriverRecords(removedRecords);
			removeFromRecords(removedRecords);
		}
		// Map<Car, RemovedCarData> List<RemovedCarData>
		cars.remove(regNumber);
		carRecords.remove(regNumber);
		String modelName = car.getModelName();
		List<Car> list = modelCars.get(modelName);
		if (list != null) {
			list.removeIf(c -> regNumber.equals(c.getRegNumber()));

		}
		return new RemovedCarData(car, removedRecords == null ? new ArrayList<>() : removedRecords);
	}

	private void removeFromRecords(List<RentRecord> removedRecords) {
		if (removedRecords == null)
			return;
		removedRecords.forEach(r -> {
			List<RentRecord> dayList = records.get(r.getRentDate());
			if (dayList != null)
				dayList.remove(r);
		});

	}

	private void removeFromDriverRecords(List<RentRecord> removedRecords) {
		if (removedRecords == null)
			return;
		removedRecords.forEach(r -> {
			List<RentRecord> drList = driverRecords.get(r.getLicenseId());
			if (drList != null)
				drList.remove(r);
		});

	}

//
	@Override
	public List<RemovedCarData> removeModel(String modelName) {
		List<Car> carsModel = modelCars.getOrDefault(modelName, new ArrayList<>());
		return carsModel.stream().filter(c -> !c.isFlRemoved()).map(c -> removeCar(c.getRegNumber()))
				// .filter(Objects::nonNull)
				.toList();
	}

	@Override
	public RemovedCarData returnCar(String regNumber, long licenseId, LocalDate returnDate, int damages,
			int tankPercent) {
		RentRecord record = getRentRecord(regNumber, licenseId);
		if (record == null)
			return new RemovedCarData(null, null);
		Car car = getCar(regNumber);
		updateRecord(record, returnDate, damages, tankPercent);

		updateCar(car, damages);
		if (damages > REMOVE_THRESHOLD || car.isFlRemoved()) {
			car.setFlRemoved(true);
			RemovedCarData remCarData = actualCarRemove(car);
			return remCarData != null ? remCarData : new RemovedCarData(car, new ArrayList<>());
		}
		return new RemovedCarData(car, null);
	}

	private void updateRecord(RentRecord record, LocalDate returnDate, int damages, int tankPercent) {
		// regNumber licenseId rentDate days
		record.setDamages(damages);
		record.setTankPercent(tankPercent);
		record.setReturnDate(returnDate);
		double cost = computeCost(getRentPricePerDay(record.getRegNumber()), 
				record.getRentDays(), getDaysDelay(record),
				tankPercent, getTankVolume(record.getRegNumber()));
		record.setCost(cost);
	}

	private int getTankVolume(String regNumber) {
		String modelName = cars.get(regNumber).getModelName();
		return models.get(modelName).getGasTank();
	}

	private int getDaysDelay(RentRecord record) {
		long actualDays = ChronoUnit.DAYS.between(record.getRentDate(), 
				record.getReturnDate());
		int delta = (int) (actualDays - record.getRentDays());
		return delta <= 0 ? 0 : delta;
	}

	private int getRentPricePerDay(String regNumber) {
		String modelName = cars.get(regNumber).getModelName();
		return models.get(modelName).getPriceDay();
	}

	private void updateCar(Car car, int damages) {
		car.setInUse(false);
		if (damages >= BAD_THRESHOLD)
			car.setState(State.BAD);
		else if (damages >= GOOD_THRESHOLD)
			car.setState(State.GOOD);
		else
			car.setState(State.EXCELLENT);

	}

	private RentRecord getRentRecord(String regNumber, long licenseId) {
		List<RentRecord> list = driverRecords.get(licenseId);
		if (list == null)
			return null;
		return list.stream().filter(r -> r.getRegNumber().equals(regNumber) && r.getReturnDate() == null).findFirst()
				.orElse(null);
	}

	@Override
	public List<String> getMostPopularCarModels(LocalDate fromDate, LocalDate toDate, int fromAge, int toAge) {
		List<RentRecord> recordsFromTo = getRentRecordsAtDate(fromDate, toDate);
		if(recordsFromTo.isEmpty()) return new ArrayList<>();
		Map<String, Long> mapTemp = recordsFromTo.stream()
				.filter(r->isProperAge(r,fromAge, toAge))
				.collect(Collectors.groupingBy(r->
				getCar(r.getRegNumber()).getModelName(), Collectors.counting()));
		
		if(mapTemp.isEmpty()) return new ArrayList<>();
		long maxValue = Collections.max(mapTemp.values());
		List<String> res = new ArrayList<>();
		mapTemp.forEach((k,v)->
		{
			if(v==maxValue)
				res.add(k);
		});
		return res;
	}

	private boolean isProperAge(RentRecord r, int fromAge, int toAge) {
		LocalDate rentDate = r.getRentDate();
		Driver driver = getDriver(r.getLicenseId());
		int driverAge = rentDate.getYear()-driver.getBirthYear();
		return driverAge>=fromAge&& driverAge<toAge;
	}

	@Override
	public List<String> getMostProfitableCarModels(LocalDate fromDate, LocalDate toDate) {
		List<RentRecord> recordsFromTo = getRentRecordsAtDate(fromDate, toDate);
		if(recordsFromTo.isEmpty()) return new ArrayList<>();
		Map<String, Double> modelCost = recordsFromTo.stream()
				.collect(Collectors.groupingBy(r->
				getCar(r.getRegNumber()).getModelName(), 
				Collectors.summingDouble(RentRecord::getCost)));
		if(modelCost.isEmpty()) return new ArrayList<>();
		double max = getMaxCost(modelCost);//100.00 100.000005 max==v
		List<String> res = new ArrayList<>();
		double eps = 0.01;
		modelCost.forEach((k,v)->{
			if(Math.abs(max-v)<=eps)
				res.add(k);
		});
		return res;
	}

	private double getMaxCost(Map<String, Double> modelCost) {
		
		return modelCost.values().stream().mapToDouble(x->x).max().getAsDouble();
	}

	@Override
	public List<Driver> getMostActiveDrivers() {
		long max=0;
		for(List<RentRecord> value: driverRecords.values()) {
			max=value.size()>max? value.size():max;
		}// licenseiD1 ->List(rr1,rr2,rr3)
		// licenseiD2 ->List(rr1)
		long maxV=max;
		List<Driver> res = new ArrayList<>();
		driverRecords.forEach((k,v)->{
			if(v.size()==maxV) res.add(getDriver(k));
		});
		return res;
	}

	@Override
	public List<String> getModelNames() {
		
		//return models.values().stream().map(Model::getModelName).toList();
		return new ArrayList<>(models.keySet());
	}

}

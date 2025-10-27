package com.telran;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import com.telran.cars.dto.Car;
import com.telran.cars.dto.Driver;
import com.telran.cars.dto.Model;
import com.telran.cars.dto.RentRecord;
import com.telran.cars.models.IRentCompany;
import com.telran.cars.models.RentCompanyEmbedded;
import com.telran.utils.Persistable;

public class RentCompanyAppl {

	public static void main(String[] args) throws IOException {
		final String MODEL_NAME = "Model1";
		final int GAS_TANK = 50;
		final int PRICE_PER_DAY = 200;
		final long LICENSE = 1000L;
		final String REG_NUMBER = "100";
		final String COMPANY = "Company1";
		final String COUNTRY = "Country1";
		final String COLOR = "color1";
		final String NAME = "name1";
		final int YEAR_OB = 1990;
		final String PHONE = "123456789";
		final LocalDate RENT_DATE = LocalDate.of(2025, 8, 1);
		final int RENT_DAYS = 3;
		
		IRentCompany company = new RentCompanyEmbedded();
		Model model1 = new Model(MODEL_NAME, GAS_TANK, COMPANY, COUNTRY, PRICE_PER_DAY);
		Car car1 = new Car(REG_NUMBER, COLOR, MODEL_NAME);
		Driver driver1 = new Driver(LICENSE, NAME, YEAR_OB, PHONE);

		// add
		System.out.println("add car " + company.addCar(car1));// NO_MODEL
		System.out.println("add model " + company.addModel(model1));// OK
		System.out.println("add car " + company.addCar(car1));// OK
		System.out.println("add driver " + company.addDriver(driver1));// OK

		// get
		System.out.println("get model " + company.getModel(MODEL_NAME));
		System.out.println("get model " + company.getModel(NAME));
		System.out.println("get car " + company.getCar(REG_NUMBER));
		System.out.println("get car " + company.getCar(PHONE));
		System.out.println("get driver " + company.getDriver(LICENSE));
		System.out.println("get driver " + company.getDriver(2L));
		
		// rent car
		System.out.println("rent car "+company.rentCar(REG_NUMBER, LICENSE, RENT_DATE, RENT_DAYS));
		System.out.println("rent car "+company.rentCar(REG_NUMBER, LICENSE, RENT_DATE, RENT_DAYS));
		
		Car car2 = new Car(REG_NUMBER+1, COLOR, MODEL_NAME);
		System.out.println("add car " + company.addCar(car2));// 
		car2.setFlRemoved(true);
		System.out.println("rent car2 "+company.rentCar(REG_NUMBER+1, LICENSE, RENT_DATE, RENT_DAYS));
		System.out.println("rent car2 "+company.rentCar(REG_NUMBER+100, LICENSE, RENT_DATE, RENT_DAYS));
		
		Car car3 = new Car(REG_NUMBER+2, COLOR, MODEL_NAME);
		System.out.println("add car " + company.addCar(car3));// 
		System.out.println("rent car3 "+company.rentCar(REG_NUMBER+2, LICENSE+1, RENT_DATE, RENT_DAYS));
		
		
		String file = "company1.data";
		((Persistable) company).save(file);
		IRentCompany restored = RentCompanyEmbedded.restoreFromFile(file);
		System.out.println("===========From restored============");
		System.out.println("get model " + restored.getModel(MODEL_NAME));

		System.out.println("get car " + restored.getCar(REG_NUMBER));

		System.out.println("get driver " + restored.getDriver(LICENSE));
		
		List<Car> carsOfModel = restored.getCarsByModel(MODEL_NAME);
		System.out.println(carsOfModel);
		List<RentRecord> records= restored.getRentRecordsAtDate(RENT_DATE, RENT_DATE.plusDays(RENT_DAYS));
		System.out.println(records);
		List<Car> carsByDriver =restored.getCarsByDriver(LICENSE);
		System.out.println(carsByDriver);
		List<Driver> driversByCar = restored.getDriversByCar(REG_NUMBER);
		System.out.println(driversByCar);

		
		//negative
		List<Car> carsOfModelN = restored.getCarsByModel(MODEL_NAME+1);
		System.out.println(carsOfModelN);
		List<RentRecord> recordsN= restored.getRentRecordsAtDate(RENT_DATE.plusDays(RENT_DAYS), RENT_DATE.plusDays(10));
		System.out.println(recordsN);
		List<Car> carsByDriverN =restored.getCarsByDriver(LICENSE+1000);
		System.out.println(carsByDriverN);
		List<Driver> driversByCarN = restored.getDriversByCar(REG_NUMBER+1000);
		System.out.println(driversByCarN);
	}

}

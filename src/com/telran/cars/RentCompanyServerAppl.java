package com.telran.cars;

import com.telran.cars.models.IRentCompany;
import com.telran.cars.models.RentCompanyEmbedded;
import com.telran.cars.models.RentCompanyProtocol;
import com.telran.utils.Persistable;
import telran.net.protocol.ProtocolJava;
import telran.net.server.ServerJava;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RentCompanyServerAppl {

    static final int N_THREADS = 12;
    static final String FILE_NAME = "company.data";
    static final int TIMEOUT = 10000;
    static final int PORT = 30000;

    public static void main(String[] args) throws Exception {
        IRentCompany company = RentCompanyEmbedded.restoreFromFile(FILE_NAME);
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ProtocolJava protocol = new RentCompanyProtocol(company);
        ServerJava server = new ServerJava(protocol, PORT, TIMEOUT, N_THREADS);
        executor.execute(server);
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter exit for shutdown");
        while(true) {
            String line = scanner.nextLine();
            if(line.equalsIgnoreCase("exit"))
                break;
        }

        executor.shutdown();
        server.shutdown();
        executor.awaitTermination(TIMEOUT, TimeUnit.MILLISECONDS);
        ((Persistable) company).save(FILE_NAME);
        scanner.close();
    }
}
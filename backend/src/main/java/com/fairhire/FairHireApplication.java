package com.fairhire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FairHireApplication {

    public static void main(String[] args) {
        SpringApplication.run(FairHireApplication.class, args);
        System.out.println("\n=======================================================");
        System.out.println("  FairHire AI Spring Boot Backend active on port 8080! ");
        System.out.println("  H2 Console: http://localhost:8080/h2-console");
        System.out.println("  Connected to Python AI Service on http://localhost:5000");
        System.out.println("=======================================================\n");
    }
}

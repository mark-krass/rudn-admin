package ru.rudn.rudnadmin;

import org.springframework.boot.SpringApplication;

public class TestRudnAdminApplication {

    public static void main(String[] args) {
        SpringApplication.from(RudnAdminApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

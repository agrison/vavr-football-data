package me.grison.metz;

import java.time.LocalDate;

public record Player(String firstName, String lastName, LocalDate dob, String type) {

    static Player fromCsv(String line) {
        String[] parts = line.split(",");
        return new Player(parts[0], parts[1], LocalDate.parse(parts[2]), parts[3]);
    }

    String name() {
        return firstName + " " + lastName;
    }
}
package me.grison.metz;

import io.vavr.collection.List;
import io.vavr.collection.Seq;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import static java.lang.Integer.parseInt;

public record Game(LocalDateTime dateTime, String opponent, String location, int ownScore, int opponentScore,
            String championship, String event, Seq<Goal> goals) {
    static Game fromCsv(String line, Function<String, Player> playerFinder) {
        String[] parts = line.split(",");
        return new Game(LocalDateTime.parse(parts[0], DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                parts[1], parts[2], parseInt(parts[3]),
                parseInt(parts[4]), parts[5], parts[6],
                parts.length == 8 ?
                        List.of(parts[7].split(":"))
                                .map(s -> s.split("@"))
                                .map(p -> new Goal(playerFinder.apply(p[0]), parseInt(p[1])))
                        : List.empty());
    }

    String display() {
        return String.format("%s - %s/%s: %s",
                dateTime, championship, event, (location.equals("home")
                        ? "Metz " + ownScore + " - " + opponentScore + " " + opponent
                        : opponent + " " + opponentScore + " - " + ownScore + " Metz"));
    }

    int points() {
        return ownScore > opponentScore ? 3 : ownScore == opponentScore ? 1 : 0;
    }

    boolean draw() {
        return points() == 1;
    }

    boolean won() {
        return points() == 3;
    }

    boolean lost() {
        return points() == 0;
    }
}
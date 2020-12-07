package me.grison.metz;

import io.vavr.Function1;
import io.vavr.Tuple;
import io.vavr.collection.HashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Stream;
import io.vavr.collection.Vector;
import io.vavr.control.Try;

import java.io.InputStream;
import java.util.function.Function;

import static java.lang.System.lineSeparator;
import static java.util.Comparator.reverseOrder;

public class FootballWithVavr {
    public static void main(String[] args) {
        Seq<Player> players = Try.withResources(() -> FootballWithVavr.class.getResourceAsStream("/players.txt"))
                .of(InputStream::readAllBytes)
                .map(b -> Vector.of(new String(b).split(lineSeparator())))
                .map(line -> line.map(Player::fromCsv))
                .getOrElseThrow(e -> new RuntimeException("boom", e));

        Function<String, Player> findPlayer = Function1.<String, Player>of(
                s -> players.find(p -> s.equals(p.lastName()))
                        .getOrElseThrow(() -> new RuntimeException("No player: " + s))).memoized();

        Seq<Game> games = Try.withResources(() -> FootballWithVavr.class.getResourceAsStream("/games.txt"))
                .of(InputStream::readAllBytes)
                .map(b -> Vector.of(new String(b).split(lineSeparator())))
                .map(line -> line.map(g -> Game.fromCsv(g, findPlayer)))
                .map(g -> g.sortBy(Game::dateTime))
                .getOrElseThrow(e -> new RuntimeException("boom", e));

        // first and last to score + total goals
        System.out.println(games.head().goals().head().player().name());
        System.out.println(games.last().goals().last().player().name());
        System.out.println("Total goals: " + games.flatMap(Game::goals).length());

        // best striker
        System.out.println("Best striker: " + games.flatMap(Game::goals)
                .groupBy(Goal::player)
                .maxBy(t -> t._2.size())
                .map(t -> t._1.name() + ": " + t._2.size() + " goals").getOrElse("???"));

        // top 5 strikers
        System.out.println("Best 5 strikers: \n\t" + games.flatMap(Game::goals)
                .groupBy(Goal::player)
                .map(t -> Tuple.of(t._1, t._2.size()))
                .sortBy(reverseOrder(), t -> t._2)
                .take(5)
                .map(t -> t._1.name() + ": " + t._2 + " goals").mkString("\n\t"));

        // best win & worst lose
        System.out.println("Best win: " + games.sortBy(reverseOrder(), Game::ownScore).maxBy(g -> g.ownScore() - g.opponentScore()).map(Game::display).getOrElse("???"));
        System.out.println("Worst lose: " + games.sortBy(reverseOrder(), Game::opponentScore).maxBy(g -> g.opponentScore() - g.ownScore()).map(Game::display).getOrElse("???"));

        // ligue 1 specific
        Seq<Game> ligue1 = games.filter(g -> "Ligue 1".equals(g.championship()));
        System.out.println("Total points: " + ligue1.map(Game::points).sum());
        System.out.println("Lost against: " + ligue1.filter(Game::lost).flatMap(g -> HashSet.of(g.opponent())).mkString(", "));
        System.out.println("Draw against: " + ligue1.filter(Game::draw).flatMap(g -> HashSet.of(g.opponent())).mkString(", "));
        System.out.println("Won against: " + ligue1.filter(Game::won).flatMap(g -> HashSet.of(g.opponent())).mkString(", "));
        System.out.println("Longest undefeated in Ligue1: \n\t" +
                Stream.range(0, ligue1.length())
                        .map(i -> Tuple.of(i, ligue1.segmentLength(g -> g.points() > 0, i)))
                        .maxBy(t -> t._2)
                        .map(t -> ligue1.subSequence(t._1 - 1, t._1 + t._2 + 1).map(Game::display).mkString("\n\t")).getOrElse("???"));

        System.out.println("Longest undefeated competitions: \n\t" + Stream.range(0, games.length())
                .map(i -> Tuple.of(i, games.segmentLength(g -> g.points() > 0, i))).maxBy(t -> t._2)
                .map(t -> games.subSequence(t._1 - 1, t._1 + t._2 + 1).map(Game::display).mkString("\n\t")).getOrElse("???"));

        System.out.println("Average goals per match: " + games.map(Game::ownScore).average().getOrElse(0d));

        System.out.println("Most goals in " +
                games.flatMap(Game::goals)
                        .groupBy(g -> g.time() <= 45)
                        .maxBy(t -> t._2.size())
                        .map(t -> (t._1 ? "first" : "second") + " (" + t._2.size() + ")")

                        .getOrElse("???") + " period");

        System.out.println(games.flatMap(Game::goals).sortBy(reverseOrder(), g -> g.player().dob()).map(Goal::player).head());

        // better away or  at home?
        System.out.println(
                games.groupBy(Game::location)
                        .mapValues(g -> g.map(Game::points).reduce(Integer::sum))
                        .maxBy(t -> t._2));

        // points evolution
        Seq<Integer> points = ligue1.map(Game::points).scan(0, Integer::sum).tail();
        Seq<Integer> goals = ligue1.map(g -> g.goals().length()).scan(0, Integer::sum).tail();
        System.out.println("            Pts  Goals\n" +
                ligue1.map(Game::event).zipWith(points, (game, pts) -> game + ":  " + pts)
                        .zipWith(goals, (game, g) -> game + "  " + g)
                        .mkString("\n"));
    }
}

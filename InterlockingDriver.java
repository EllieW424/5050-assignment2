import java.util.Arrays;
import java.util.Scanner;

/**
 * Interactive driver for manually exercising InterlockingImpl.
 *
 * This file is for local testing only. It is deliberately kept outside the
 * submission files and does not change Interlocking.java or InterlockingImpl.java.
 */
public final class InterlockingDriver {
    private static final int FIRST_SECTION = 1;
    private static final int LAST_SECTION = 11;

    private Interlocking railway = new InterlockingImpl();

    public static void main(String[] args) {
        new InterlockingDriver().run();
    }

    private void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("COMP5050 Interlocking manual test driver");
            System.out.println("This driver does not modify the assignment source files.");

            boolean running = true;
            while (running) {
                printMenu();
                String choice = readLine(scanner, "Choose 0-7: ");

                try {
                    switch (choice) {
                        case "1":
                            addTrain(scanner);
                            break;
                        case "2":
                            moveTrains(scanner);
                            break;
                        case "3":
                            findTrain(scanner);
                            break;
                        case "4":
                            inspectSection(scanner);
                            break;
                        case "5":
                            showAllSections();
                            break;
                        case "6":
                            railway = new InterlockingImpl();
                            System.out.println("Railway reset: all trains removed.");
                            break;
                        case "7":
                            runQuickDemo();
                            break;
                        case "0":
                            running = false;
                            break;
                        default:
                            System.out.println("Unknown option. Enter a number from 0 to 7.");
                    }
                } catch (IllegalArgumentException | IllegalStateException exception) {
                    System.out.println(
                        "Expected API exception: "
                            + exception.getClass().getSimpleName()
                            + (exception.getMessage() == null ? "" : " - " + exception.getMessage())
                    );
                }
            }

            System.out.println("Manual test driver closed.");
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1 - Add a train");
        System.out.println("2 - Move one or more trains");
        System.out.println("3 - Find a train");
        System.out.println("4 - Inspect one section");
        System.out.println("5 - Show sections 1-11");
        System.out.println("6 - Reset the railway");
        System.out.println("7 - Run quick route demo (P1: 1 -> 5 -> 8 -> exit)");
        System.out.println("0 - Exit");
    }

    private void addTrain(Scanner scanner) {
        String name = readLine(scanner, "Train name: ");
        int entry = readInt(scanner, "Entry section: ");
        int destination = readInt(scanner, "Destination section: ");
        railway.addTrain(name, entry, destination);
        System.out.println("Added " + name + " at section " + railway.getTrain(name) + ".");
    }

    private void moveTrains(Scanner scanner) {
        String input = readLine(scanner, "Train names, separated by commas: ");
        String[] names = Arrays.stream(input.split(","))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .toArray(String[]::new);

        if (names.length == 0) {
            System.out.println("No train names entered.");
            return;
        }

        int moved = railway.moveTrains(names);
        System.out.println("Moved trains: " + moved);
        for (String name : names) {
            System.out.println("  " + name + " -> section " + railway.getTrain(name));
        }
    }

    private void findTrain(Scanner scanner) {
        String name = readLine(scanner, "Train name: ");
        int section = railway.getTrain(name);
        if (section == -1) {
            System.out.println(name + " has exited the railway.");
        } else {
            System.out.println(name + " is in section " + section + ".");
        }
    }

    private void inspectSection(Scanner scanner) {
        int section = readInt(scanner, "Section number: ");
        String occupant = railway.getSection(section);
        System.out.println(
            occupant == null
                ? "Section " + section + " is empty."
                : "Section " + section + " contains " + occupant + "."
        );
    }

    private void showAllSections() {
        for (int section = FIRST_SECTION; section <= LAST_SECTION; section++) {
            String occupant = railway.getSection(section);
            System.out.printf(
                "Section %2d: %s%n",
                section,
                occupant == null ? "empty" : occupant
            );
        }
    }

    private void runQuickDemo() {
        railway = new InterlockingImpl();
        railway.addTrain("P1", 1, 8);
        System.out.println("P1 starts at section " + railway.getTrain("P1"));

        for (int step = 1; step <= 3; step++) {
            int moved = railway.moveTrains(new String[] {"P1"});
            System.out.println(
                "Step " + step + ": moved=" + moved + ", P1 section=" + railway.getTrain("P1")
            );
        }
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            String input = readLine(scanner, prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Enter a whole number.");
            }
        }
    }
}

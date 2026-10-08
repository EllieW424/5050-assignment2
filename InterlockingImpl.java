import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controls train movement through the eleven-section Islington interlocking.
 *
 * <p>The implementation plans each call to {@link #moveTrains(String[])} from one snapshot and
 * applies the chosen moves atomically. This permits safe forward chains while preventing two
 * trains from occupying one section, swapping across an edge, or crossing through a junction at
 * the same time.
 */
public final class InterlockingImpl implements Interlocking {
  private static final int FIRST_SECTION = 1;
  private static final int LAST_SECTION = 11;
  private static final int EXIT = -1;

  private final Map<String, TrainState> trains = new HashMap<>();
  private final Map<Integer, String> occupants = new HashMap<>();
  private long nextSequence;

  /** Creates an empty railway corridor. */
  public InterlockingImpl() {}

  @Override
  public synchronized void addTrain(
      String trainName, int entryTrackSection, int destinationTrackSection) {
    validateNewName(trainName);
    int[] route = routeFor(entryTrackSection, destinationTrackSection);
    if (route == null) {
      throw new IllegalArgumentException("No valid route for the requested entry and destination");
    }
    if (occupants.containsKey(entryTrackSection)) {
      throw new IllegalStateException("The entry track section is occupied");
    }

    TrainState train = new TrainState(trainName, route, nextSequence++);
    trains.put(trainName, train);
    occupants.put(entryTrackSection, trainName);
  }

  @Override
  public synchronized int moveTrains(String[] trainNames) {
    List<TrainState> requested = validateAndDeduplicate(trainNames);
    if (requested.isEmpty()) {
      return 0;
    }

    List<Move> candidates = new ArrayList<>();
    for (int i = 0; i < requested.size(); i++) {
      TrainState train = requested.get(i);
      int from = train.currentSection();
      int to = train.atDestination() ? EXIT : train.nextSection();
      candidates.add(new Move(train, from, to, i));
    }

    int selectedMask = selectBestSafeSet(candidates);
    applySelectedMoves(candidates, selectedMask);
    updateWaitingRounds(candidates, selectedMask);
    return Integer.bitCount(selectedMask);
  }

  @Override
  public synchronized String getSection(int trackSection) {
    validateSection(trackSection);
    return occupants.get(trackSection);
  }

  @Override
  public synchronized int getTrain(String trainName) {
    TrainState train = trains.get(trainName);
    if (trainName == null || train == null) {
      throw new IllegalArgumentException("The train name does not exist");
    }
    return train.active ? train.currentSection() : EXIT;
  }

  private int selectBestSafeSet(List<Move> candidates) {
    int bestMask = 0;
    SelectionScore bestScore = SelectionScore.EMPTY;
    int combinations = 1 << candidates.size();

    for (int mask = 1; mask < combinations; mask++) {
      if (!isSafeSelection(candidates, mask)) {
        continue;
      }
      SelectionScore score = SelectionScore.forSelection(candidates, mask);
      if (score.isBetterThan(bestScore)) {
        bestMask = mask;
        bestScore = score;
      }
    }
    return bestMask;
  }

  private boolean isSafeSelection(List<Move> candidates, int mask) {
    Map<Integer, Move> selectedByOrigin = new HashMap<>();
    Map<Integer, Move> selectedByTarget = new HashMap<>();
    List<Move> selected = new ArrayList<>();

    for (int i = 0; i < candidates.size(); i++) {
      if (!isSelected(mask, i)) {
        continue;
      }
      Move move = candidates.get(i);
      selected.add(move);
      selectedByOrigin.put(move.from, move);
      if (!move.isExit() && selectedByTarget.put(move.to, move) != null) {
        return false;
      }
    }

    for (Move move : selected) {
      if (move.isExit()) {
        continue;
      }
      String occupantName = occupants.get(move.to);
      if (occupantName != null) {
        Move occupantMove = selectedByOrigin.get(move.to);
        if (occupantMove == null || !occupantMove.train.name.equals(occupantName)) {
          return false;
        }
      }
    }

    for (int i = 0; i < selected.size(); i++) {
      Move first = selected.get(i);
      for (int j = i + 1; j < selected.size(); j++) {
        Move second = selected.get(j);
        if (movesSwapEdges(first, second) || movementsConflictAtJunction(first, second)) {
          return false;
        }
      }
    }
    return true;
  }

  private static boolean movesSwapEdges(Move first, Move second) {
    return !first.isExit()
        && !second.isExit()
        && first.from == second.to
        && first.to == second.from;
  }

  private static boolean movementsConflictAtJunction(Move first, Move second) {
    boolean westernConflict =
        (first.isPassengerWesternCrossing() && second.isFreightBranchCrossing())
            || (second.isPassengerWesternCrossing() && first.isFreightBranchCrossing());
    boolean easternConflict =
        (first.isEasternThroughMove() && second.isEasternReturnMove())
            || (second.isEasternThroughMove() && first.isEasternReturnMove());
    return westernConflict || easternConflict;
  }

  private void applySelectedMoves(List<Move> candidates, int mask) {
    for (int i = 0; i < candidates.size(); i++) {
      if (isSelected(mask, i)) {
        occupants.remove(candidates.get(i).from);
      }
    }

    for (int i = 0; i < candidates.size(); i++) {
      if (!isSelected(mask, i)) {
        continue;
      }
      Move move = candidates.get(i);
      if (move.isExit()) {
        move.train.active = false;
      } else {
        move.train.routeIndex++;
        occupants.put(move.to, move.train.name);
      }
    }
  }

  private static void updateWaitingRounds(List<Move> candidates, int mask) {
    for (int i = 0; i < candidates.size(); i++) {
      TrainState train = candidates.get(i).train;
      if (isSelected(mask, i)) {
        train.waitingRounds = 0;
      } else {
        train.waitingRounds++;
      }
    }
  }

  private List<TrainState> validateAndDeduplicate(String[] trainNames) {
    if (trainNames == null) {
      throw new IllegalArgumentException("The train list cannot be null");
    }

    LinkedHashMap<String, TrainState> unique = new LinkedHashMap<>();
    for (String trainName : trainNames) {
      TrainState train = trains.get(trainName);
      if (trainName == null || train == null || !train.active) {
        throw new IllegalArgumentException("A requested train is not in the rail corridor");
      }
      unique.putIfAbsent(trainName, train);
    }
    return new ArrayList<>(unique.values());
  }

  private void validateNewName(String trainName) {
    if (trainName == null || trainName.isEmpty()) {
      throw new IllegalArgumentException("The train name cannot be null or empty");
    }
    TrainState existingTrain = trains.get(trainName);
    if (existingTrain != null && existingTrain.active) {
      throw new IllegalArgumentException("The train name is already in use");
    }
  }

  private static void validateSection(int section) {
    if (section < FIRST_SECTION || section > LAST_SECTION) {
      throw new IllegalArgumentException("The track section does not exist");
    }
  }

  private static int[] routeFor(int entry, int destination) {
    if (entry == 1 && destination == 8) {
      return new int[] {1, 5, 8};
    }
    if (entry == 1 && destination == 9) {
      return new int[] {1, 5, 9};
    }
    if (entry == 9 && destination == 2) {
      return new int[] {9, 6, 2};
    }
    if (entry == 10 && destination == 2) {
      return new int[] {10, 6, 2};
    }
    if (entry == 3 && destination == 4) {
      return new int[] {3, 4};
    }
    if (entry == 3 && destination == 11) {
      return new int[] {3, 7, 11};
    }
    if (entry == 4 && destination == 3) {
      return new int[] {4, 3};
    }
    if (entry == 11 && destination == 3) {
      return new int[] {11, 7, 3};
    }
    return null;
  }

  private static boolean isSelected(int mask, int index) {
    return (mask & (1 << index)) != 0;
  }

  private static final class TrainState {
    private final String name;
    private final int[] route;
    private final long sequence;
    private int routeIndex;
    private int waitingRounds;
    private boolean active = true;

    private TrainState(String name, int[] route, long sequence) {
      this.name = name;
      this.route = route;
      this.sequence = sequence;
    }

    private int currentSection() {
      return route[routeIndex];
    }

    private int nextSection() {
      return route[routeIndex + 1];
    }

    private boolean atDestination() {
      return routeIndex == route.length - 1;
    }
  }

  private static final class Move {
    private final TrainState train;
    private final int from;
    private final int to;
    private final int requestOrder;

    private Move(TrainState train, int from, int to, int requestOrder) {
      this.train = train;
      this.from = from;
      this.to = to;
      this.requestOrder = requestOrder;
    }

    private boolean isExit() {
      return to == EXIT;
    }

    private boolean isPassengerWesternCrossing() {
      return (from == 1 && to == 5) || (from == 6 && to == 2);
    }

    private boolean isFreightBranchCrossing() {
      return (from == 3 && to == 4) || (from == 4 && to == 3);
    }

    private boolean isEasternThroughMove() {
      return from == 5 && to == 9;
    }

    private boolean isEasternReturnMove() {
      return from == 9 && to == 6;
    }
  }

  private static final class SelectionScore {
    private static final SelectionScore EMPTY = new SelectionScore(0, 0, 0, 0, 0);

    private final int passengerWesternMoves;
    private final int moveCount;
    private final long waitedRounds;
    private final long sequencePreference;
    private final long requestPreference;

    private SelectionScore(
        int passengerWesternMoves,
        int moveCount,
        long waitedRounds,
        long sequencePreference,
        long requestPreference) {
      this.passengerWesternMoves = passengerWesternMoves;
      this.moveCount = moveCount;
      this.waitedRounds = waitedRounds;
      this.sequencePreference = sequencePreference;
      this.requestPreference = requestPreference;
    }

    private static SelectionScore forSelection(List<Move> candidates, int mask) {
      int passengerMoves = 0;
      int count = 0;
      long waits = 0;
      long sequences = 0;
      long requests = 0;
      for (int i = 0; i < candidates.size(); i++) {
        if (!isSelected(mask, i)) {
          continue;
        }
        Move move = candidates.get(i);
        passengerMoves += move.isPassengerWesternCrossing() ? 1 : 0;
        count++;
        waits += move.train.waitingRounds;
        sequences -= move.train.sequence;
        requests -= move.requestOrder;
      }
      return new SelectionScore(passengerMoves, count, waits, sequences, requests);
    }

    private boolean isBetterThan(SelectionScore other) {
      if (passengerWesternMoves != other.passengerWesternMoves) {
        return passengerWesternMoves > other.passengerWesternMoves;
      }
      if (moveCount != other.moveCount) {
        return moveCount > other.moveCount;
      }
      if (waitedRounds != other.waitedRounds) {
        return waitedRounds > other.waitedRounds;
      }
      if (sequencePreference != other.sequencePreference) {
        return sequencePreference > other.sequencePreference;
      }
      return requestPreference > other.requestPreference;
    }
  }
}

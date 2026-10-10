import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** Tests simultaneous movement, priority, collision exclusion and progress behaviour. */
public class InterlockingSafety_Test {
  @Test
  public void westernCrossing_givesSouthPassengerPriorityOverFreightBranch() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("passenger", 1, 8);
    interlocking.addTrain("freight", 3, 4);

    assertEquals(1, interlocking.moveTrains(new String[] {"freight", "passenger"}));
    assertEquals(5, interlocking.getTrain("passenger"));
    assertEquals(3, interlocking.getTrain("freight"));
  }

  @Test
  public void westernCrossing_givesNorthPassengerPriorityOverFreightBranch() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("passenger", 9, 2);
    interlocking.addTrain("freight", 4, 3);
    interlocking.moveTrains(new String[] {"passenger"});

    assertEquals(1, interlocking.moveTrains(new String[] {"freight", "passenger"}));
    assertEquals(2, interlocking.getTrain("passenger"));
    assertEquals(4, interlocking.getTrain("freight"));
  }

  @Test
  public void blockedPassengerDoesNotPreventSafeFreightMovement() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 1, 8);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("waiting", 1, 9);
    interlocking.addTrain("freight", 3, 4);

    assertEquals(1, interlocking.moveTrains(new String[] {"waiting", "freight"}));
    assertEquals(1, interlocking.getTrain("waiting"));
    assertEquals(4, interlocking.getTrain("freight"));
  }

  @Test
  public void sectionVacatedInSameCallCannotBeEntered() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 1, 8);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("following", 1, 8);

    assertEquals(1, interlocking.moveTrains(new String[] {"following", "lead"}));
    assertEquals(1, interlocking.getTrain("following"));
    assertEquals(8, interlocking.getTrain("lead"));

    assertEquals(1, interlocking.moveTrains(new String[] {"following"}));
    assertEquals(5, interlocking.getTrain("following"));
  }

  @Test
  public void exitAndEntryOccurOnSeparateCalls() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("leaving", 1, 8);
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.addTrain("following", 1, 8);
    interlocking.moveTrains(new String[] {"following"});

    assertEquals(1, interlocking.moveTrains(new String[] {"following", "leaving"}));
    assertEquals(-1, interlocking.getTrain("leaving"));
    assertEquals(5, interlocking.getTrain("following"));
  }

  @Test
  public void twoIncomingPassengerTrainsCannotBothEnterSection6() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("older", 9, 2);
    interlocking.addTrain("newer", 10, 2);

    assertEquals(1, interlocking.moveTrains(new String[] {"newer", "older"}));
    assertEquals(6, interlocking.getTrain("older"));
    assertEquals(10, interlocking.getTrain("newer"));
  }

  @Test
  public void waitingTrainMovesAfterContestedSectionIsReleased() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("first", 9, 2);
    interlocking.addTrain("second", 10, 2);
    interlocking.moveTrains(new String[] {"first", "second"});

    assertEquals(1, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(2, interlocking.getTrain("first"));
    assertEquals(10, interlocking.getTrain("second"));

    assertEquals(2, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(-1, interlocking.getTrain("first"));
    assertEquals(6, interlocking.getTrain("second"));
  }

  @Test
  public void easternJunctionSerialisesOpposingUseAroundSection9() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 1, 9);
    interlocking.moveTrains(new String[] {"south"});
    interlocking.addTrain("north", 9, 2);

    assertEquals(1, interlocking.moveTrains(new String[] {"south", "north"}));
    assertEquals(5, interlocking.getTrain("south"));
    assertEquals(6, interlocking.getTrain("north"));
  }

  @Test
  public void easternJunctionIsSafeWhenRequestOrderIsReversed() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 1, 9);
    interlocking.moveTrains(new String[] {"south"});
    interlocking.addTrain("north", 9, 2);

    assertEquals(1, interlocking.moveTrains(new String[] {"north", "south"}));
    assertEquals(5, interlocking.getTrain("south"));
    assertEquals(6, interlocking.getTrain("north"));
  }

  @Test
  public void opposingFreightBranchTrainsDoNotSwapSections() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 4);
    interlocking.addTrain("north", 4, 3);

    assertEquals(0, interlocking.moveTrains(new String[] {"south", "north"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(4, interlocking.getTrain("north"));
  }

  @Test
  public void freightTrainDoesNotEnterSection7TowardsWaitingOpponent() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 11);
    interlocking.addTrain("north", 11, 3);

    assertEquals(0, interlocking.moveTrains(new String[] {"south", "north"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(11, interlocking.getTrain("north"));
  }

  @Test
  public void freightTrainDoesNotEnterSection7WhileOpponentWaitsAtFarEnd() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("north", 11, 3);
    interlocking.addTrain("south", 3, 11);

    assertEquals(0, interlocking.moveTrains(new String[] {"south"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(11, interlocking.getTrain("north"));
  }

  @Test
  public void freightTrainEntersSection7WhenFarEndTrainIsLeaving() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 3, 11);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("follow", 3, 11);

    assertEquals(1, interlocking.moveTrains(new String[] {"follow"}));
    assertEquals(7, interlocking.getTrain("follow"));
  }

  @Test
  public void freightTrainEntersSection7WhenOpponentIsNotWaiting() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 11);

    assertEquals(1, interlocking.moveTrains(new String[] {"south"}));
    assertEquals(7, interlocking.getTrain("south"));
  }

  /**
   * Two opposing freight trains were both requested while section 7 was free for either of them.
   * Each entry would trap the entering train against the other one, so the interlocking keeps both
   * trains at the ends of the single-track line instead of approving either entry.
   */
  @Test
  public void opposingFreightTrainsOnSingleTrackWaitAtTheLineEnds() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("branchNorth", 4, 3);
    interlocking.addTrain("passNorth", 1, 9);
    interlocking.addTrain("passEast", 10, 2);
    interlocking.moveTrains(new String[] {"passEast"});
    interlocking.addTrain("freightSouth", 3, 11);
    interlocking.addTrain("freightNorth", 11, 3);

    assertEquals(4, interlocking.getTrain("branchNorth"));
    assertEquals(1, interlocking.getTrain("passNorth"));
    assertEquals(6, interlocking.getTrain("passEast"));
    assertEquals(3, interlocking.getTrain("freightSouth"));
    assertEquals(11, interlocking.getTrain("freightNorth"));

    assertEquals(2, interlocking.moveTrains(new String[] {"branchNorth", "passNorth", "passEast",
        "freightSouth", "freightNorth"}));
    assertEquals(4, interlocking.getTrain("branchNorth"));
    assertEquals(5, interlocking.getTrain("passNorth"));
    assertEquals(2, interlocking.getTrain("passEast"));
    assertEquals(3, interlocking.getTrain("freightSouth"));
    assertEquals(11, interlocking.getTrain("freightNorth"));
  }

  /**
   * The older train leaves section 6 in the same round while two newer trains are queued behind it.
   * Section 6 must stay unavailable to both of them for this round, so exactly one train moves.
   */
  @Test
  public void sectionVacatedInTheSameRoundStaysUnavailableToQueuedTrains() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("older", 9, 2);
    interlocking.moveTrains(new String[] {"older"});
    interlocking.addTrain("newer9", 9, 2);
    interlocking.addTrain("newer10", 10, 2);

    assertEquals(1, interlocking.moveTrains(new String[] {"older", "newer9", "newer10"}));
    assertEquals(2, interlocking.getTrain("older"));
    assertEquals(9, interlocking.getTrain("newer9"));
    assertEquals(10, interlocking.getTrain("newer10"));
  }

  /**
   * A column of freight trains would each step into the section that the train ahead of it frees in
   * the same round. Only the furthest train may leave, so no train ever enters a section that was
   * occupied when the round started.
   */
  @Test
  public void freightColumnCannotChainIntoVacatedSectionsInOneRound() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 4, 3);
    interlocking.addTrain("arrived", 11, 3);
    interlocking.moveTrains(new String[] {"arrived"});
    interlocking.moveTrains(new String[] {"arrived"});
    interlocking.addTrain("middle", 11, 3);
    interlocking.moveTrains(new String[] {"middle"});
    interlocking.addTrain("rear", 11, 3);

    assertEquals(3, interlocking.getTrain("arrived"));
    assertEquals(7, interlocking.getTrain("middle"));
    assertEquals(11, interlocking.getTrain("rear"));

    assertEquals(1, interlocking.moveTrains(new String[] {"lead", "arrived", "middle", "rear"}));
    assertEquals(4, interlocking.getTrain("lead"));
    assertEquals(-1, interlocking.getTrain("arrived"));
    assertEquals(7, interlocking.getTrain("middle"));
    assertEquals(11, interlocking.getTrain("rear"));
  }

  @Test
  public void scriptedTrafficAlwaysMaintainsUniqueSectionOccupancy() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("p1", 1, 8);
    interlocking.addTrain("p2", 9, 2);
    interlocking.addTrain("f1", 3, 11);
    assertUniqueOccupancy(interlocking);

    interlocking.moveTrains(new String[] {"p1", "p2", "f1"});
    assertUniqueOccupancy(interlocking);
    interlocking.addTrain("p3", 10, 2);

    interlocking.moveTrains(new String[] {"p1", "p2", "p3", "f1"});
    assertUniqueOccupancy(interlocking);
    interlocking.moveTrains(new String[] {"p1", "p2", "p3", "f1"});
    assertUniqueOccupancy(interlocking);
  }

  private static void assertUniqueOccupancy(Interlocking interlocking) {
    Set<String> seen = new HashSet<>();
    for (int section = 1; section <= 11; section++) {
      String train = interlocking.getSection(section);
      if (train != null) {
        assertEquals("A train appeared in two sections", true, seen.add(train));
      }
    }
  }
}

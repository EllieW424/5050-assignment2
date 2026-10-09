import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
  public void uncontestedFollowerAdvancesWhenLeadVacatesSection() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 1, 8);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("following", 1, 8);

    assertEquals(2, interlocking.moveTrains(new String[] {"following", "lead"}));
    assertEquals(5, interlocking.getTrain("following"));
    assertEquals(8, interlocking.getTrain("lead"));
  }

  @Test
  public void departingTrainCanBeReplacedInSameCall() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("leaving", 1, 8);
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.addTrain("following", 1, 8);
    interlocking.moveTrains(new String[] {"following"});

    assertEquals(2, interlocking.moveTrains(new String[] {"following", "leaving"}));
    assertEquals(-1, interlocking.getTrain("leaving"));
    assertEquals(8, interlocking.getTrain("following"));
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

    assertEquals(2, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(2, interlocking.getTrain("first"));
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
  public void contestedOccupiedSectionCannotBeInheritedThroughChaining() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 9, 2);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("first", 9, 2);
    interlocking.addTrain("second", 10, 2);

    // Both followers request section 6 while the lead still occupies it; the
    // lead reaches its destination but neither follower may inherit the
    // contested section.
    assertEquals(1, interlocking.moveTrains(new String[] {"lead", "first", "second"}));
    assertEquals(2, interlocking.getTrain("lead"));
    assertEquals(9, interlocking.getTrain("first"));
    assertEquals(10, interlocking.getTrain("second"));

    assertEquals(1, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(6, interlocking.getTrain("first"));
    assertEquals(10, interlocking.getTrain("second"));
  }

  @Test
  public void freightTrainEntersSection7WhenUnrequestedOpponentWaitsAtFarEnd() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("north", 11, 3);
    interlocking.addTrain("south", 3, 11);

    assertEquals(1, interlocking.moveTrains(new String[] {"south"}));
    assertEquals(7, interlocking.getTrain("south"));
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

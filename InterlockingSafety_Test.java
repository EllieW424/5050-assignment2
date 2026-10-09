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
  public void atomicPlanningDoesNotEnterAnOccupiedSection() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 1, 8);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("following", 1, 8);

    assertEquals(1, interlocking.moveTrains(new String[] {"following", "lead"}));
    assertEquals(1, interlocking.getTrain("following"));
    assertEquals(8, interlocking.getTrain("lead"));
  }

  @Test
  public void exitMakesTheSectionAvailableOnTheNextRound() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("leaving", 1, 8);
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.moveTrains(new String[] {"leaving"});
    interlocking.addTrain("following", 1, 8);
    interlocking.moveTrains(new String[] {"following"});

    assertEquals(1, interlocking.moveTrains(new String[] {"following", "leaving"}));
    assertEquals(-1, interlocking.getTrain("leaving"));
    assertEquals(5, interlocking.getTrain("following"));
    assertEquals(1, interlocking.moveTrains(new String[] {"following"}));
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

    assertEquals(1, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(6, interlocking.getTrain("first"));
    assertEquals(10, interlocking.getTrain("second"));
    assertEquals(1, interlocking.moveTrains(new String[] {"first", "second"}));
    assertEquals(2, interlocking.getTrain("first"));
    assertEquals(10, interlocking.getTrain("second"));
    assertEquals(1, interlocking.moveTrains(new String[] {"second"}));
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
  public void opposingFreightTrainsCannotBothEnterSection7() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 11);
    interlocking.addTrain("north", 11, 3);

    assertEquals(0, interlocking.moveTrains(new String[] {"north", "south"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(11, interlocking.getTrain("north"));
    // Repeating the request cannot create a passing place on the single track.
    assertEquals(0, interlocking.moveTrains(new String[] {"north", "south"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(11, interlocking.getTrain("north"));
  }

  @Test
  public void freightCanEnterEmptySection7WhenTheOpposingTrainIsNotRequested() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 11);
    interlocking.addTrain("north", 11, 3);

    assertEquals(1, interlocking.moveTrains(new String[] {"south"}));
    assertEquals(0, interlocking.moveTrains(new String[] {"north"}));
    assertEquals(7, interlocking.getTrain("south"));
    assertEquals(11, interlocking.getTrain("north"));
    assertEquals("south", interlocking.getSection(7));
  }

  @Test
  public void northFreightCanEnterEmptySection7WithUnrequestedSouthFreightAt3() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("south", 3, 11);
    interlocking.addTrain("north", 11, 3);

    assertEquals(1, interlocking.moveTrains(new String[] {"north", "north"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(7, interlocking.getTrain("north"));
    assertEquals("north", interlocking.getSection(7));
    assertNull(interlocking.getSection(11));

    // Entry into empty 7 is one step; neither train can then enter an occupied target.
    assertEquals(0, interlocking.moveTrains(new String[] {"south", "north"}));
    assertEquals(3, interlocking.getTrain("south"));
    assertEquals(7, interlocking.getTrain("north"));
  }

  @Test
  public void requestedOpposingFreightTrainsWaitRegardlessOfRequestOrder() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("north", 11, 3);
    interlocking.addTrain("south", 3, 11);

    assertEquals(0, interlocking.moveTrains(new String[] {"south", "north", "south"}));
    assertEquals(11, interlocking.getTrain("north"));
    assertEquals(3, interlocking.getTrain("south"));
    assertNull(interlocking.getSection(7));
  }

  @Test
  public void freightCanFollowATrainAtItsDestinationWithoutAnOpposingDeadlock() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("lead", 3, 11);
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.moveTrains(new String[] {"lead"});
    interlocking.addTrain("following", 3, 11);

    assertEquals(1, interlocking.moveTrains(new String[] {"following"}));
    assertEquals(7, interlocking.getTrain("following"));
    assertEquals(11, interlocking.getTrain("lead"));
    assertEquals(1, interlocking.moveTrains(new String[] {"lead", "following"}));
    assertEquals(-1, interlocking.getTrain("lead"));
    assertEquals(7, interlocking.getTrain("following"));
    assertEquals(1, interlocking.moveTrains(new String[] {"following"}));
    assertEquals(11, interlocking.getTrain("following"));
  }

  @Test
  public void freightCanEnterSection7WhileTheOtherEndIsHeadingForSection4() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("branch", 3, 4);
    interlocking.addTrain("through", 11, 3);

    assertEquals(2, interlocking.moveTrains(new String[] {"through", "branch"}));
    assertEquals(4, interlocking.getTrain("branch"));
    assertEquals(7, interlocking.getTrain("through"));
    assertEquals(2, interlocking.moveTrains(new String[] {"through", "branch"}));
    assertEquals(-1, interlocking.getTrain("branch"));
    assertEquals(3, interlocking.getTrain("through"));
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

  @Test
  public void bulkScenario005_preventsOpposingFreightDeadlock() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("Train465", 4, 3);
    interlocking.addTrain("Train466", 1, 9);
    interlocking.addTrain("Train467", 10, 2);
    // The supplied log places Train467 at 6 before the bulk movement request.
    assertEquals(1, interlocking.moveTrains(new String[] {"Train467"}));
    interlocking.addTrain("Train468", 3, 11);
    interlocking.addTrain("Train469", 11, 3);

    assertEquals(
        2,
        interlocking.moveTrains(
            new String[] {"Train465", "Train466", "Train467", "Train468", "Train469"}));
    assertEquals(4, interlocking.getTrain("Train465"));
    assertEquals(5, interlocking.getTrain("Train466"));
    assertEquals(2, interlocking.getTrain("Train467"));
    assertEquals(3, interlocking.getTrain("Train468"));
    assertEquals(11, interlocking.getTrain("Train469"));
  }

  @Test
  public void bulkScenario010_doesNotEnterSection6DuringTheRoundItIsVacated() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("Train486", 9, 2);
    assertEquals(1, interlocking.moveTrains(new String[] {"Train486"}));
    interlocking.addTrain("Train487", 9, 2);
    interlocking.addTrain("Train488", 10, 2);

    assertEquals(
        1,
        interlocking.moveTrains(new String[] {"Train486", "Train487", "Train488"}));
    assertEquals(2, interlocking.getTrain("Train486"));
    assertEquals(9, interlocking.getTrain("Train487"));
    assertEquals(10, interlocking.getTrain("Train488"));

    // On the next call 6 really is free. Select one incoming train immediately.
    assertEquals(1, interlocking.moveTrains(new String[] {"Train487", "Train488"}));
    assertEquals(6, interlocking.getTrain("Train487"));
    assertEquals(10, interlocking.getTrain("Train488"));
  }

  @Test
  public void bulkScenario015_doesNotUseAForwardFreightChain() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("Train501", 4, 3);
    interlocking.addTrain("Train502", 11, 3);
    interlocking.moveTrains(new String[] {"Train502"});
    interlocking.moveTrains(new String[] {"Train502"});
    interlocking.addTrain("Train503", 11, 3);
    interlocking.moveTrains(new String[] {"Train503"});
    interlocking.addTrain("Train504", 11, 3);

    assertEquals(
        1,
        interlocking.moveTrains(
            new String[] {"Train501", "Train502", "Train503", "Train504"}));
    assertEquals(4, interlocking.getTrain("Train501"));
    assertEquals(-1, interlocking.getTrain("Train502"));
    assertEquals(7, interlocking.getTrain("Train503"));
    assertEquals(11, interlocking.getTrain("Train504"));

    // Once 3 is free, choose one contender. Its follower still cannot enter occupied 7.
    assertEquals(1, interlocking.moveTrains(new String[] {"Train501", "Train503", "Train504"}));
    assertEquals(3, interlocking.getTrain("Train501"));
    assertEquals(7, interlocking.getTrain("Train503"));
    assertEquals(11, interlocking.getTrain("Train504"));
    assertEquals(1, interlocking.moveTrains(new String[] {"Train501", "Train503", "Train504"}));
    assertEquals(-1, interlocking.getTrain("Train501"));
    assertEquals(7, interlocking.getTrain("Train503"));
    assertEquals(11, interlocking.getTrain("Train504"));
    assertEquals(1, interlocking.moveTrains(new String[] {"Train503", "Train504"}));
    assertEquals(3, interlocking.getTrain("Train503"));
    assertEquals(11, interlocking.getTrain("Train504"));
    assertEquals(2, interlocking.moveTrains(new String[] {"Train503", "Train504"}));
    assertEquals(-1, interlocking.getTrain("Train503"));
    assertEquals(7, interlocking.getTrain("Train504"));
    assertEquals(1, interlocking.moveTrains(new String[] {"Train504"}));
    assertEquals(3, interlocking.getTrain("Train504"));
    assertEquals(1, interlocking.moveTrains(new String[] {"Train504"}));
    assertEquals(-1, interlocking.getTrain("Train504"));
  }

  @Test(timeout = 1000)
  public void bulkScenario020_setupCompletesBeforeOccupiedSectionsBlockTheBulkMove() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("Train517", 3, 4);
    assertEquals(1, interlocking.moveTrains(new String[] {"Train517"}));
    interlocking.addTrain("Train518", 3, 11);
    interlocking.addTrain("Train519", 11, 3);

    // The grader first moves only Train519 to its temporary stop at 7.
    assertEquals(1, interlocking.moveTrains(new String[] {"Train519"}));
    assertEquals(4, interlocking.getTrain("Train517"));
    assertEquals(3, interlocking.getTrain("Train518"));
    assertEquals(7, interlocking.getTrain("Train519"));

    assertEquals(
        1,
        interlocking.moveTrains(new String[] {"Train517", "Train518", "Train519"}));
    assertEquals(-1, interlocking.getTrain("Train517"));
    assertEquals(3, interlocking.getTrain("Train518"));
    assertEquals(7, interlocking.getTrain("Train519"));
    assertUniqueOccupancy(interlocking);
  }

  @Test(timeout = 1000)
  public void passengerTransitionsWorkWithTheFreightSetupFromScenario020() {
    int[][] routes = {{1, 5, 8}, {1, 5, 9}, {9, 6, 2}, {10, 6, 2}};
    for (int[] route : routes) {
      Interlocking interlocking = new InterlockingImpl();
      interlocking.addTrain("stationary", 3, 11);
      interlocking.addTrain("parked", 11, 3);
      assertEquals(1, interlocking.moveTrains(new String[] {"parked"}));
      interlocking.addTrain("passenger", route[0], route[route.length - 1]);

      for (int i = 1; i < route.length; i++) {
        assertEquals(1, interlocking.moveTrains(new String[] {"passenger"}));
        assertEquals(route[i], interlocking.getTrain("passenger"));
        assertEquals(3, interlocking.getTrain("stationary"));
        assertEquals(7, interlocking.getTrain("parked"));
        assertUniqueOccupancy(interlocking);
      }
      assertEquals(1, interlocking.moveTrains(new String[] {"passenger"}));
      assertEquals(-1, interlocking.getTrain("passenger"));
    }
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

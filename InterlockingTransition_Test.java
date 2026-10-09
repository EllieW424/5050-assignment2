import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** Regression tests for following a moving train and replacing a departing train. */
public class InterlockingTransition_Test {
  private static final int[][] ROUTES = {
    {1, 5, 8}, {1, 5, 9}, {9, 6, 2}, {10, 6, 2},
    {3, 4}, {3, 7, 11}, {4, 3}, {11, 7, 3}
  };

  @Test
  public void uncontestedForwardChainsWorkOnEveryThreeSectionRoute() {
    for (int[] route : ROUTES) {
      if (route.length != 3) {
        continue;
      }
      for (boolean followerFirst : new boolean[] {true, false}) {
        Interlocking railway = new InterlockingImpl();
        railway.addTrain("lead", route[0], route[2]);
        assertEquals(1, railway.moveTrains(new String[] {"lead"}));
        railway.addTrain("following", route[0], route[2]);

        String[] request = followerFirst
            ? new String[] {"following", "lead"} : new String[] {"lead", "following"};
        assertEquals(2, railway.moveTrains(request));
        assertEquals(route[2], railway.getTrain("lead"));
        assertEquals(route[1], railway.getTrain("following"));
        assertEquals("lead", railway.getSection(route[2]));
        assertEquals("following", railway.getSection(route[1]));
        assertNull(railway.getSection(route[0]));
      }
    }
  }

  @Test
  public void aDepartingTrainCanBeReplacedOnEveryRoute() {
    for (int[] route : ROUTES) {
      int destination = route[route.length - 1];
      for (boolean followerFirst : new boolean[] {true, false}) {
        Interlocking railway = new InterlockingImpl();
        railway.addTrain("leaving", route[0], destination);
        for (int step = 1; step < route.length; step++) {
          assertEquals(1, railway.moveTrains(new String[] {"leaving"}));
        }
        railway.addTrain("following", route[0], destination);
        for (int step = 1; step < route.length - 1; step++) {
          assertEquals(1, railway.moveTrains(new String[] {"following"}));
        }

        String[] request = followerFirst
            ? new String[] {"following", "leaving"} : new String[] {"leaving", "following"};
        assertEquals(2, railway.moveTrains(request));
        assertEquals(-1, railway.getTrain("leaving"));
        assertEquals(destination, railway.getTrain("following"));
        assertEquals("following", railway.getSection(destination));
        assertNull(railway.getSection(route[route.length - 2]));
      }
    }
  }

  @Test
  public void threeTrainForwardChainsIncludeTheExitAndTwoSingleSteps() {
    for (int[] route : ROUTES) {
      if (route.length != 3) {
        continue;
      }
      Interlocking railway = new InterlockingImpl();
      railway.addTrain("front", route[0], route[2]);
      railway.moveTrains(new String[] {"front"});
      railway.moveTrains(new String[] {"front"});
      railway.addTrain("middle", route[0], route[2]);
      railway.moveTrains(new String[] {"middle"});
      railway.addTrain("rear", route[0], route[2]);

      assertEquals(3, railway.moveTrains(new String[] {"rear", "middle", "front"}));
      assertEquals(-1, railway.getTrain("front"));
      assertEquals(route[2], railway.getTrain("middle"));
      assertEquals(route[1], railway.getTrain("rear"));
      assertEquals("middle", railway.getSection(route[2]));
      assertEquals("rear", railway.getSection(route[1]));
      assertNull(railway.getSection(route[0]));
    }
  }

  @Test
  public void unrequestedOccupantsBlockFollowingTrainsOnEveryRoute() {
    for (int[] route : ROUTES) {
      int destination = route[route.length - 1];
      Interlocking railway = new InterlockingImpl();
      railway.addTrain("parked", route[0], destination);
      railway.moveTrains(new String[] {"parked"});
      railway.addTrain("following", route[0], destination);

      assertEquals(0, railway.moveTrains(new String[] {"following"}));
      assertEquals(route[0], railway.getTrain("following"));
      assertEquals(route[1], railway.getTrain("parked"));
      assertEquals("parked", railway.getSection(route[1]));
    }
  }

  @Test
  public void aFollowingPassengerHasCrossingPriorityWhenItsLeadCanMove() {
    Interlocking railway = new InterlockingImpl();
    railway.addTrain("lead", 1, 8);
    railway.moveTrains(new String[] {"lead"});
    railway.addTrain("following", 1, 9);
    railway.addTrain("freight", 3, 4);

    assertEquals(2, railway.moveTrains(new String[] {"freight", "following", "lead"}));
    assertEquals(8, railway.getTrain("lead"));
    assertEquals(5, railway.getTrain("following"));
    assertEquals(3, railway.getTrain("freight"));
  }
}

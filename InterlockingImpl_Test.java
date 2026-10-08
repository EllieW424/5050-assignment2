import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Tests the public contract and every legal route. */
public class InterlockingImpl_Test {
  @Test
  public void newCorridor_hasElevenEmptySections() {
    Interlocking interlocking = new InterlockingImpl();
    for (int section = 1; section <= 11; section++) {
      assertNull(interlocking.getSection(section));
    }
  }

  @Test
  public void southPassenger_canTravelFrom1To8AndExit() {
    verifyRoute(1, 8, new int[] {1, 5, 8});
  }

  @Test
  public void southPassenger_canTravelFrom1To9AndExit() {
    verifyRoute(1, 9, new int[] {1, 5, 9});
  }

  @Test
  public void northPassenger_canTravelFrom9To2AndExit() {
    verifyRoute(9, 2, new int[] {9, 6, 2});
  }

  @Test
  public void northPassenger_canTravelFrom10To2AndExit() {
    verifyRoute(10, 2, new int[] {10, 6, 2});
  }

  @Test
  public void southFreight_canTravelFrom3To4AndExit() {
    verifyRoute(3, 4, new int[] {3, 4});
  }

  @Test
  public void southFreight_canTravelFrom3To11AndExit() {
    verifyRoute(3, 11, new int[] {3, 7, 11});
  }

  @Test
  public void northFreight_canTravelFrom4To3AndExit() {
    verifyRoute(4, 3, new int[] {4, 3});
  }

  @Test
  public void northFreight_canTravelFrom11To3AndExit() {
    verifyRoute(11, 3, new int[] {11, 7, 3});
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsPassengerToFreightDestination() {
    new InterlockingImpl().addTrain("mixed", 1, 11);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsFreightToPassengerDestination() {
    new InterlockingImpl().addTrain("mixed", 3, 8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsNonEntrySection() {
    new InterlockingImpl().addTrain("invalid", 5, 8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsUnknownDestinationSection() {
    new InterlockingImpl().addTrain("invalid", 1, 12);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsNullName() {
    new InterlockingImpl().addTrain(null, 1, 8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsEmptyName() {
    new InterlockingImpl().addTrain("", 1, 8);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addTrain_rejectsDuplicateKnownName() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("same", 1, 8);
    interlocking.addTrain("same", 3, 4);
  }

  @Test(expected = IllegalStateException.class)
  public void addTrain_rejectsOccupiedEntry() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("first", 1, 8);
    interlocking.addTrain("second", 1, 9);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSection_rejectsSectionZero() {
    new InterlockingImpl().getSection(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSection_rejectsSectionTwelve() {
    new InterlockingImpl().getSection(12);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getTrain_rejectsUnknownName() {
    new InterlockingImpl().getTrain("unknown");
  }

  @Test(expected = IllegalArgumentException.class)
  public void getTrain_rejectsNullName() {
    new InterlockingImpl().getTrain(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void moveTrains_rejectsNullArray() {
    new InterlockingImpl().moveTrains(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void moveTrains_rejectsNullNameInsideArray() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("valid", 1, 8);
    interlocking.moveTrains(new String[] {"valid", null});
  }

  @Test
  public void moveTrains_emptyArrayMovesNothing() {
    assertEquals(0, new InterlockingImpl().moveTrains(new String[0]));
  }

  @Test
  public void moveTrains_duplicateNameMovesOnce() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("passenger", 1, 8);

    assertEquals(1, interlocking.moveTrains(new String[] {"passenger", "passenger"}));
    assertEquals(5, interlocking.getTrain("passenger"));
  }

  @Test
  public void moveTrains_invalidNameDoesNotPartiallyMoveValidTrain() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("valid", 1, 8);

    try {
      interlocking.moveTrains(new String[] {"valid", "missing"});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertEquals(1, interlocking.getTrain("valid"));
    }
  }

  @Test
  public void completedTrain_remainsKnownButCannotMoveAgain() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("complete", 3, 4);
    interlocking.moveTrains(new String[] {"complete"});
    interlocking.moveTrains(new String[] {"complete"});

    assertEquals(-1, interlocking.getTrain("complete"));
    assertNull(interlocking.getSection(4));
    try {
      interlocking.moveTrains(new String[] {"complete"});
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertEquals(-1, interlocking.getTrain("complete"));
    }
  }

  @Test
  public void completedTrainName_canBeReusedForANewTrain() {
    Interlocking interlocking = new InterlockingImpl();
    interlocking.addTrain("reusable", 3, 4);
    interlocking.moveTrains(new String[] {"reusable"});
    interlocking.moveTrains(new String[] {"reusable"});

    interlocking.addTrain("reusable", 1, 8);
    assertEquals(1, interlocking.getTrain("reusable"));
    assertEquals("reusable", interlocking.getSection(1));
  }

  private static void verifyRoute(int entry, int destination, int[] route) {
    Interlocking interlocking = new InterlockingImpl();
    String name = "route" + entry + "to" + destination;
    interlocking.addTrain(name, entry, destination);
    assertEquals(entry, interlocking.getTrain(name));
    assertEquals(name, interlocking.getSection(entry));

    for (int i = 1; i < route.length; i++) {
      int oldSection = route[i - 1];
      assertEquals(1, interlocking.moveTrains(new String[] {name}));
      assertNull(interlocking.getSection(oldSection));
      assertEquals(route[i], interlocking.getTrain(name));
      assertEquals(name, interlocking.getSection(route[i]));
    }

    assertEquals(1, interlocking.moveTrains(new String[] {name}));
    assertEquals(-1, interlocking.getTrain(name));
    assertNull(interlocking.getSection(destination));
  }
}

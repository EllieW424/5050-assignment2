COMP5050 Programming Assignment 2
Railway Interlocking

This repository contains a JDK 11 implementation of the supplied Interlocking
interface, a Petri-net design, and JUnit 4 tests for the Islington railway
network.

SUBMISSION FILES

- Interlocking.java: supplied interface; unchanged.
- InterlockingImpl.java: railway interlocking implementation.
- InterlockingImpl_Test.java: public API and route tests.
- InterlockingSafety_Test.java: simultaneous movement and safety tests.
- PetriNetDesign.pdf: Petri-net plan, guards, routes, and invariants.

IMPLEMENTATION SUMMARY

The implementation stores one ordered route for each valid entry/destination
pair. Each moveTrains call is planned from a single snapshot. It selects a
maximum safe subset, clears all selected origins, and then applies all selected
destinations. A selected set is rejected if two trains target the same section,
if a target remains occupied, if two trains swap across an edge, or if their
movements conflict at a junction. Passenger movements have priority over
freight movements at the western crossing. Waiting time, train insertion order,
and request order provide deterministic tie-breaking.

VALID ROUTES

Passenger: 1-5-8, 1-5-9, 9-6-2, 10-6-2
Freight:   3-4, 3-7-11, 4-3, 11-7-3

BUILD AND TEST

Requirements:
- JDK 11
- JUnit 4.13.2
- Hamcrest Core 1.3

Windows:
  javac -Xlint:all -cp "junit-4.13.2.jar;hamcrest-core-1.3.jar" Interlocking.java InterlockingImpl.java InterlockingImpl_Test.java InterlockingSafety_Test.java
  java -cp ".;junit-4.13.2.jar;hamcrest-core-1.3.jar" org.junit.runner.JUnitCore InterlockingImpl_Test InterlockingSafety_Test

macOS or Linux:
  javac -Xlint:all -cp "junit-4.13.2.jar:hamcrest-core-1.3.jar" Interlocking.java InterlockingImpl.java InterlockingImpl_Test.java InterlockingSafety_Test.java
  java -cp ".:junit-4.13.2.jar:hamcrest-core-1.3.jar" org.junit.runner.JUnitCore InterlockingImpl_Test InterlockingSafety_Test

VERIFICATION RESULT

The final local run used JDK 11.0.32.1 and JUnit 4.13.2. All 39 tests passed.
JaCoCo measured 98.9% line coverage and 90.3% branch coverage across the
production implementation and its nested classes.

KNOWN OPERATING ASSUMPTION

moveTrains only considers the active trains named by the caller. A train not
listed in a call remains in its section. Opposing trains can therefore block
one another on a single-track edge; the interlocking preserves safety and does
not move an unrequested train to manufacture progress.

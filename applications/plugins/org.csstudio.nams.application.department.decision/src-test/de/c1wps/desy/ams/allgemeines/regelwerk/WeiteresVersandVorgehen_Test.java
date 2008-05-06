package de.c1wps.desy.ams.allgemeines.regelwerk;

import de.c1wps.desy.ams.AbstractObject_TestCase;

public class WeiteresVersandVorgehen_Test extends
		AbstractObject_TestCase<WeiteresVersandVorgehen> /*
															 * TODO TestCase
															 * fuer Enums
															 * erstellen!
															 */{

	public void testAnzahlDerElemente() {
		assertEquals(4, WeiteresVersandVorgehen.values().length);
	}

	@Override
	protected WeiteresVersandVorgehen getNewInstanceOfClassUnderTest() {
		return WeiteresVersandVorgehen.ERNEUT_PRUEFEN;
	}

	@Override
	protected Object getNewInstanceOfIncompareableTypeInAccordingToClassUnderTest() {
		return new Object();
	}

	@Override
	protected WeiteresVersandVorgehen[] getThreeDiffrentNewInstanceOfClassUnderTest() {
		return new WeiteresVersandVorgehen[] {
				WeiteresVersandVorgehen.VERSENDEN,
				WeiteresVersandVorgehen.NICHT_VERSENDEN,
				WeiteresVersandVorgehen.NOCH_NICHT_GEPRUEFT };
	}

}

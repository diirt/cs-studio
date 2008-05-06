package de.c1wps.desy.ams.alarmentscheidungsbuero;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class Alarmentscheidungsbuero_SubSystemTestSuite extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for de.c1wps.desy.ams.alarmentscheidungsbuero");
		//$JUnit-BEGIN$
		suite.addTestSuite(AlarmEntscheidungsBuero_Test.class);
		suite.addTestSuite(Abteilungsleiter_Test.class);
		suite.addTestSuite(Sachbearbeiter_Test.class);
		suite.addTestSuite(TerminAssistenz_Test.class);
		suite.addTestSuite(Terminnotiz_Test.class);
		//$JUnit-END$
		return suite;
	}

}

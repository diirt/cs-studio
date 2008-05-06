/* 
 * Copyright (c) 2008 C1 WPS mbH, 
 * HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. 
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR
 * PURPOSE AND  NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE 
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, 
 * REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL
 * PART OF THIS LICENSE. NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER 
 * EXCEPT UNDER THIS DISCLAIMER.
 * C1 WPS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, 
 * ENHANCEMENTS, OR MODIFICATIONS. THE FULL LICENSE SPECIFYING FOR THE 
 * SOFTWARE THE REDISTRIBUTION, MODIFICATION, USAGE AND OTHER RIGHTS AND 
 * OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS 
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU 
 * MAY FIND A COPY AT
 * {@link http://www.eclipse.org/org/documents/epl-v10.html}.
 */
package de.c1wps.desy.ams.alarmentscheidungsbuero;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import de.c1wps.desy.ams.allgemeines.Ausgangskorb;
import de.c1wps.desy.ams.allgemeines.Eingangskorb;
import de.c1wps.desy.ams.allgemeines.StandardAblagekorb;
import de.c1wps.desy.ams.allgemeines.Vorgangsmappe;
import de.c1wps.desy.ams.allgemeines.regelwerk.Regelwerk;
import de.c1wps.desy.ams.allgemeines.wam.Arbeitsumgebung;

/**
 * Repraesentiert die Abteilung Alarm-Entscheidungs-Buero mit Ihrer gesammten
 * Einrichtung. Dieses Buero trifft die Entscheidung, ob eine Nachricht
 * versendet werden soll oder nicht.
 * 
 * Note: Dieses ist die einzige exportierte Klasse dieses Sub-Systems.
 */
@Arbeitsumgebung
public class AlarmEntscheidungsBuero {

	private Eingangskorb<Vorgangsmappe> alarmVorgangEingangskorb;
	private Ausgangskorb<Vorgangsmappe> ausgangskorb;
	
	private TerminAssistenz _assistenz;
	private Abteilungsleiter _abteilungsleiter;
	private List<Sachbearbeiter> _sachbearbeiterList;

	/**
	 * Legt ein neues Alarmbuero an. Es wird zugesichert, das nur hier
	 * angeforderte system-externe Komponenten in diesem sub-System verwendet
	 * werden.
	 * 
	 * TODO Logger-Service hinzufuegen/reinreichen um das Logging testbar zu machen, da dieses wichtig fuer Nachweiszwecke ist.
	 */
	public AlarmEntscheidungsBuero(Regelwerk[] regelwerke) {
		alarmVorgangEingangskorb = new StandardAblagekorb<Vorgangsmappe>();
		ausgangskorb = new StandardAblagekorb<Vorgangsmappe>();
		_sachbearbeiterList = new LinkedList<Sachbearbeiter>();
		// Sachbearbeiter und Koerbe anlegen:
		StandardAblagekorb<Terminnotiz> terminAssistenzEingangskorb = new StandardAblagekorb<Terminnotiz>();
		Eingangskorb<Vorgangsmappe>[] eingangskorbe = erzeugeEingangskoerbe(regelwerke.length);
		Map<String, Eingangskorb<Terminnotiz>> terminEingangskoerbeDerSachbearbeiter = new HashMap<String, Eingangskorb<Terminnotiz>>();
		for (int zaehler = 0; zaehler < regelwerke.length; zaehler++) {
			Eingangskorb<Vorgangsmappe> eingangskorb = new StandardAblagekorb<Vorgangsmappe>();
			eingangskorbe[zaehler] = eingangskorb;
			StandardAblagekorb<Terminnotiz> terminEingangskorb = new StandardAblagekorb<Terminnotiz>();
			Sachbearbeiter sachbearbeiter = new Sachbearbeiter("" + zaehler,
					eingangskorb,
					terminEingangskorb,
					new StandardAblagekorb<Vorgangsmappe>(),
					terminAssistenzEingangskorb,
					ausgangskorb
					/*
															 * TODO ggf. in
															 * selben
															 * Ausgangskorb
															 * legen
															 */, regelwerke[zaehler]);
			
			terminEingangskoerbeDerSachbearbeiter.put(sachbearbeiter.gibName(), terminEingangskorb);
			_sachbearbeiterList.add(sachbearbeiter);
			sachbearbeiter.beginneArbeit();
		}

		_assistenz = new TerminAssistenz(terminAssistenzEingangskorb, terminEingangskoerbeDerSachbearbeiter, new Timer());
		_assistenz.beginneArbeit();
		
		_abteilungsleiter = new Abteilungsleiter(
				gibAlarmVorgangEingangskorb(), eingangskorbe);
		// Starten...
		_abteilungsleiter.beginneArbeit();
	}

	@SuppressWarnings("unchecked")
	private Eingangskorb<Vorgangsmappe>[] erzeugeEingangskoerbe(int length) {
		return new Eingangskorb[length];
	}

	/**
	 * Gibt zugriff auf den Eingangskorb für Alarmvorgaenge. Die Mappe wird so
	 * verwendet, wie diese hineingereicht wird. Das Kapitel
	 * "AlarmEntscheidungsbuero" sollte extern nicht veraendert werden.
	 * 
	 * @return Einen Eingangskorb fuer neue Vorgaenge.
	 */
	public Eingangskorb<Vorgangsmappe> gibAlarmVorgangEingangskorb() {
		return alarmVorgangEingangskorb;
	}
	
	/**
	 * Liefert den Ausgangskorb, in dem die bearbeiteten Vorgaenge abgelegt werden.
	 */
	public Ausgangskorb<Vorgangsmappe> gibAlarmVorgangAusgangskorb() {
		return ausgangskorb;
	}
	
	/**
	 * Inspector fuer Tests. Liefert die Referenz auf die Terminassistenz.
	 */
	TerminAssistenz gibAssistenzFuerTest() {
		return _assistenz;
	}
	
	/**
	 * Inspector fuer Tests. Liefert die Referenz auf den Abteilungsleiter.
	 */
	Abteilungsleiter gibAbteilungsleiterFuerTest() {
		return _abteilungsleiter;
	}
	
	/**
	 * Inspector fuer Tests. Liefert die Referenzen auf alle Sachbearbeiter.
	 */
	Collection<Sachbearbeiter> gibListeDerSachbearbeiterFuerTest() {
		return _sachbearbeiterList;
	}
}

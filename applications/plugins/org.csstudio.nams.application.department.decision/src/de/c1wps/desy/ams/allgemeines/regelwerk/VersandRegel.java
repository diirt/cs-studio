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
package de.c1wps.desy.ams.allgemeines.regelwerk;

import de.c1wps.desy.ams.allgemeines.AlarmNachricht;
import de.c1wps.desy.ams.allgemeines.Millisekunden;

public interface VersandRegel {
	/**
	 * Evaluiert initial ob eine Nachricht ausgeliefert werden soll. Dieses
	 * geschieht duch eintragen von TRUE, FALSE oder MAYBE in das
	 * "Ergebnis-Array".
	 * 
	 * @param nachricht
	 * @param ergebnisListe
	 * @return Die Anzahl der Millisekunden, bis die
	 *         {@link #pruefeNachricht(AlarmNachricht, Ergebnis, Millisekunden)}
	 *         Operation gerufen werden soll, falls der Vorgang nicht vorher
	 *         abgeschlossen werden kann, oder {@code null}, falls die Regel
	 *         unverzueglich ausgewertet wurde.
	 */
	public Millisekunden pruefeNachrichtErstmalig(AlarmNachricht nachricht,
			Pruefliste ergebnisListe);

	/**
	 * Führt eine erneute Evaluation aufgrund einer Benachrichtigung durch die
	 * Assistenz durch.
	 * 
	 * @param nachricht
	 * @param bisherigesErgebnis
	 * @param verstricheneZeit
	 *            seit der ersten Pruefung verstrichene Millisekunden
	 * @return Die Anzahl der Millisekunden, bis diese Operation erneut gerufen
	 *         werden soll, falls der Vorgang nicht vorher abgeschlossen werden
	 *         kann, oder {@code null}, falls die Regel unverzueglich
	 *         ausgewertet wurde.
	 */
	public Millisekunden pruefeNachrichtAufTimeOuts(
			Pruefliste bisherigesErgebnis,
			Millisekunden verstricheneZeitSeitErsterPruefung);

	/**
	 * Wendet die Aufhebungs und Bestätigungsregeln auf eine Nachricht an.
	 * 
	 * @param nachricht
	 * @param bisherigesErgebnis
	 */
	public void pruefeNachrichtAufBestaetigungsUndAufhebungsNachricht(
			AlarmNachricht nachricht, Pruefliste bisherigesErgebnis);

	// public Millisekunden gibverbleibendeWartezeit(Millisekunden
	// bereitsVerstricheneWarteZeit);
}

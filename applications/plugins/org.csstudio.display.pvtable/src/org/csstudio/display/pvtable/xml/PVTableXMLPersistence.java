/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pvtable.xml;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.csstudio.apputil.xml.DOMHelper;
import org.csstudio.apputil.xml.XMLWriter;
import org.csstudio.display.pvtable.model.PVTableItem;
import org.csstudio.display.pvtable.model.PVTableModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Persist PVTableModel as XML file
 *  @author Kay Kasemir
 */
public class PVTableXMLPersistence
{
    final private static String XML_HEADER = "<?xml version=\"1.0\"?>\n<pvtable version=\"2.0\">"; //$NON-NLS-1$
    final private static String XML_TAIL = "</pvtable>\n"; //$NON-NLS-1$
	final private static String ROOT = "pvtable";
	final private static String TOLERANCE = "tolerance";
	final private static String PVLIST = "pvlist";
	final private static String PV = "pv";
	final private static String SELECTED = "selected";
	final private static String NAME = "name";
	final private static String SAVED = "saved_value";
	
	/** @param stream XML stream
	 *  @return PV table model
	 *  @throws Exception on error
	 */
	public static PVTableModel read(final InputStream stream) throws Exception
	{
		final DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder();
        Document doc = docBuilder.parse(stream);
        return read(doc);
	}

	/** @param doc XML document
	 *  @return PV table model
	 *  @throws Exception on error
	 */
	private static PVTableModel read(final Document doc) throws Exception
	{
		final PVTableModel model = new PVTableModel();
        // Check if it's a <pvtable/>.
        doc.getDocumentElement().normalize();
        Element root_node = doc.getDocumentElement();
        String root_name = root_node.getNodeName();
        if (!root_name.equals(ROOT))
            throw new Exception("Expected <" + ROOT + ">, found <" + root_name
                    + ">");
        // Get the default <tolerance> entry
        double default_tolerance = PVTableItem.DEFAULT_TOLERANCE;
        try
        {
        	default_tolerance = DOMHelper.getSubelementDouble(root_node, TOLERANCE);
        }
        catch (Exception ex)
        {
        	default_tolerance = PVTableItem.DEFAULT_TOLERANCE;
        }

        // Get the <pvlist> entry
        Element pvlist = DOMHelper.findFirstElementNode(root_node
                .getFirstChild(), PVLIST);
        if (pvlist != null)
        {
            Element pv = DOMHelper.findFirstElementNode(pvlist.getFirstChild(),
                    PV);
            while (pv != null)
            {
            	final double tolerance = DOMHelper.getSubelementDouble(pv, TOLERANCE, default_tolerance);
            	final boolean selected = DOMHelper.getSubelementBoolean(pv, SELECTED, true);
                final String pv_name = DOMHelper.getSubelementString(pv, NAME);
                // TODO SavedValue saved = SavedValue.fromString(
                //         DOMHelper.getSubelementString(pv, saved_value));
                final PVTableItem item = model.addItem(pv_name, tolerance);
                item.setSelected(selected);
                pv = DOMHelper.findNextElementNode(pv, PV);
            }
        }
        return model;
	}

	/** @param model PV table model
	 *  @param stream Stream to which model is written as XML
	 */
	public static void write(final PVTableModel model, final OutputStream stream)
	{
		final PrintWriter out = new PrintWriter(stream);

		out.println(XML_HEADER);
		
		XMLWriter.start(out, 1, PVLIST);
		out.println();
		
		final int N = model.getItemCount();
		for (int i=0; i<N; ++i)
		{
			final PVTableItem item = model.getItem(i);
			XMLWriter.start(out, 2, PV);
			out.println();
			XMLWriter.XML(out, 3, SELECTED, true);
			XMLWriter.XML(out, 3, NAME, item.getName());
			XMLWriter.XML(out, 3, TOLERANCE, item.getTolerance());
			// TODO XMLWriter.XML(out, 3, SAVED, item.getName());
			XMLWriter.end(out, 2, PV);
			out.println();
		}
		XMLWriter.end(out, 1, PVLIST);
		out.println();
		out.println(XML_TAIL);
		
		out.flush();
	}
}

/**
 * 
 */
package org.csstudio.logbook.olog.ui;

import org.csstudio.logbook.Property;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import edu.msu.nscl.olog.api.PropertyBuilder;

import static org.csstudio.logbook.olog.OlogLogbookClient.OlogProperty;
import org.eclipse.swt.widgets.Label;

/**
 * @author shroffk
 * 
 */
public class ContextPropertyWidgetTest extends ApplicationWindow {

	public ContextPropertyWidgetTest() {
		super(null);
		addToolBar(SWT.FLAT | SWT.WRAP);
		addMenuBar();
		addStatusLine();
	}

	/**
	 * Create contents of the application window.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		final ContextPropertyWidget contextPropertyWidget = new ContextPropertyWidget(
				container, SWT.NONE,"context");
		contextPropertyWidget.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				true, true, 1, 1));
		return container;
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		try {
			ContextPropertyWidgetTest window = new ContextPropertyWidgetTest();
			window.setBlockOnOpen(true);
			window.open();
			Display.getCurrent().dispose();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Configure the shell.
	 * 
	 * @param newShell
	 */
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Test ImageStackWidgetTest");
	}

	/**
	 * Return the initial size of the window.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(473, 541);
	}
}

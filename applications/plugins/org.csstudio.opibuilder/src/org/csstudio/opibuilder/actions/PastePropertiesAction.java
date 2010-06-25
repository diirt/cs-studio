package org.csstudio.opibuilder.actions;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.opibuilder.commands.SetWidgetPropertyCommand;
import org.csstudio.opibuilder.datadefinition.PropertiesCopyData;
import org.csstudio.opibuilder.editor.OPIEditor;
import org.csstudio.opibuilder.editparts.AbstractBaseEditPart;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

/**The action that paste the properties from clipboard.
 * @author Xihui Chen
 *
 */
public class PastePropertiesAction extends SelectionAction {

	public static final String ID = "org.csstudio.opibuilder.actions.pasteproperties";	
	
	
	public PastePropertiesAction(IWorkbenchPart part) {
		super(part);
		setText("Paste Properties");
		setId(ID);
	}

	@Override
	protected boolean calculateEnabled() {
		if(getSelectedWidgetModels().size() >0 && getPropetiesCopyDataFromClipboard() != null)
			return true;
		return false;
	}
	
	
	public Command createPasteCommand(){
		PropertiesCopyData propData = getPropetiesCopyDataFromClipboard();
		CompoundCommand cmd = new CompoundCommand("Paste Properties");
		
		for(AbstractWidgetModel targetWidget : getSelectedWidgetModels()){				
			for(String prop_id : propData.getPropIDList()){
				if(targetWidget.getAllPropertyIDs().contains(prop_id)){
					cmd.add(new SetWidgetPropertyCommand(
							targetWidget, prop_id, propData.getWidgetModel().getPropertyValue(prop_id)));
				}
			}
		}

		
		return cmd;
		
	}
	
	
	@Override
	public void run() {
		execute(createPasteCommand());
	}
	
	/**
	 * Returns a list with widget models that are currently stored on the
	 * clipboard.
	 * 
	 * @return a list with widget models or an empty list
	 */
	private PropertiesCopyData getPropetiesCopyDataFromClipboard() {	
		Object result = getOPIEditor().getClipboard()
				.getContents(PropertiesCopyDataTransfer.getInstance());
		if(result != null && result instanceof PropertiesCopyData){
			return (PropertiesCopyData)result;		
		}
		return null;
	}
	
	
	/**
	 * Returns the currently open OPI editor.
	 * 
	 * @return the currently open OPI editor
	 */
	private OPIEditor getOPIEditor() {

			return (OPIEditor) getWorkbenchPart();

	}
	
	/**
	 * Gets the widget models of all currently selected EditParts.
	 * 
	 * @return a list with all widget models that are currently selected
	 */
	@SuppressWarnings("unchecked")
	protected final List<AbstractWidgetModel> getSelectedWidgetModels() {
		List selection = getSelectedObjects();
	
		List<AbstractWidgetModel> selectedWidgetModels = new ArrayList<AbstractWidgetModel>();
	
		for (Object o : selection) {
			if (o instanceof AbstractBaseEditPart) {
				selectedWidgetModels.add(((AbstractBaseEditPart) o)
						.getWidgetModel());
			}
		}
		return selectedWidgetModels;
	}
}

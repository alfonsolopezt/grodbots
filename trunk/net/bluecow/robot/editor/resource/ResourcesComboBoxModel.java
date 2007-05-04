/*
 * Created on Sep 28, 2006
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot.editor.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.bluecow.robot.resource.ResourceManager;
import net.bluecow.robot.resource.ResourceNameFilter;

public class ResourcesComboBoxModel implements ComboBoxModel {

    private final List<String> items;
    
    private String selectedItem;
    
    /**
     * Creates a new combo box model with a snapshot of the resource manager's
     * resources.  The contents of the combo box do not currently change to
     * reflect changes in the resource manager, but this class might get that
     * upgrade later on.
     * 
     * @param resourceManager The resource manager to populate resource names from
     * @param filter The filter to apply to resource names. Specifying a null
     * filter has the same effect as specifying a filter that accepts everything.
     * @throws RuntimeException if the resource manager throws an IOException,
     * this constructor wraps it in a RuntimeException for you.
     */
    public ResourcesComboBoxModel(ResourceManager resourceManager, ResourceNameFilter filter) {
        try {
            this.items = resourceManager.listAll(filter);
            this.selectedItem = null;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public Object getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Object anItem) {
        selectedItem = (String) anItem;
        fireSelectionChanged();
    }


    public Object getElementAt(int index) {
        return items.get(index);
    }

    public int getSize() {
        return items.size();
    }

    public List<ListDataListener> listeners = new ArrayList<ListDataListener>();
    
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }
    
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    private void fireSelectionChanged() {
        // not sure why we use a list data event, but this is what DefaultComboBoxModel does.
        ListDataEvent evt = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        for (int i = listeners.size() - 1; i >= 0; i--) {
            listeners.get(i).contentsChanged(evt);
        }
    }

}

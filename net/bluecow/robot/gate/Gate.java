package net.bluecow.robot.gate;

import javax.swing.event.*;

public interface Gate {

	/**
	 * Gets the label text that is associated with this gate.
	 * 
	 * @return The text associated with this gate, or null if the gate has no label.
	 */
	public String getLabel();
	
    /**
     * Returns the current output state of this gate.
     */
    public abstract boolean getOutputState();

    public Gate.Input[] getInputs();
    
    /**
     * The input interface represents an input to a gate.  A gate will
     * have 0 or more inputs.
     */
    public interface Input {

	/**
	 * Connects this input to the given gate's output.
	 */
	public void connect(Gate g);

	/**
	 * Returns the current state of this input.
	 */
	public boolean getState();
	
	/**
	 * Returns the gate that this input is connected to.  The value is null
	 * if this input is not connected.
	 */
	public Gate getConnectedGate();
	
	/**
	 * Returns the gate that this is an input for.
	 */
	public Gate getGate();
	
    }

    /**
     * Adds a listener which will be notified every time this gate's
     * output state changes.
     */
    public void addChangeListener(ChangeListener l);

    /**
     * Removes a listener from the notification list.  If the given
     * listener was not already on the list, this method fails
     * silently.
     */
    public void removeChangeListener(ChangeListener l);
}

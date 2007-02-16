/*
 * Created on Apr 21, 2006
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.bluecow.robot.sprite.Sprite;
import bsh.EvalError;
import bsh.Interpreter;

/**
 * LevelConfig represents the configuration of a particular level.  It is not
 * immutible because the level might need to change during play, for example
 * if a switch is thrown then the square configuration, robot speeds, and anything
 * else might change.
 *
 * @author fuerth
 * @version $Id$
 */
public class LevelConfig {
    
    private static final boolean debugOn = false;
    
    /**
     * The Switch class represents an effect that can happen to a level
     * when a robot enters or leaves a square.
     */
    public static class Switch implements Labelable {
        private LevelConfig level;
        private Point position;
        private String id;
        private Sprite sprite;
        private String onEnter;
        private String onExit;
        private boolean enabled = true;
        private String label;
        private boolean labelEnabled;
        private Direction labelDirection = Direction.EAST;
        
        public Switch(Point position, String id, String label, Sprite sprite, String onEnter) {
            this.position = new Point(position);
            this.id = id;
            this.label = label;
            this.sprite = sprite;
            this.onEnter = onEnter;
        }
        
        /**
         * Copy constructor.  Creates a switch instance with all the same properties
         * as the given switch.  The new copy will not be part of any particular level,
         * and its level reference will be null.
         */
        public Switch(Switch copyMe) {
            copyFrom(copyMe);
        }

        /**
         * Copies all properties of the switch except the level reference,
         * which is left alone.
         * 
         * @param copyMe The Switch whose properties to copy to this one.
         */
        public final void copyFrom(Switch copyMe) {
            this.position = new Point(copyMe.position);
            this.id = copyMe.id;
            this.sprite = copyMe.sprite;
            this.onEnter = copyMe.onEnter;
            this.onExit = copyMe.onExit;
            this.enabled = copyMe.enabled;
            this.label = copyMe.label;
            this.labelEnabled = copyMe.labelEnabled;
            this.labelDirection = copyMe.labelDirection;
        }
        
        /**
         * Invokes this switch's onEnter script.  You should call this every
         * time a robot enters the square occupied by this switch.
         * 
         * @param robot The robot that just entered this switch
         * @throws EvalError if there is a scripting error
         */
        public void onEnter(Robot robot) throws EvalError {
            if (level == null) {
                throw new IllegalStateException(
                        "Can't evaluate switch onEnter: Switch is not attached to a level.");
            }
            if (onEnter == null) return;
            if (!enabled) return;

            Interpreter bsh = level.getBshInterpreter();
            bsh.set("robot", robot);
            
            if (debugOn) {
                System.out.printf("Dump of scripting variables: (bsh=0x%x, interpreter=0x%x)\n", System.identityHashCode(bsh), System.identityHashCode(bsh.getNameSpace()));
                for (String name : bsh.getNameSpace().getVariableNames()) {
                    System.out.println("  "+name+": "+bsh.get(name));;
                }
                System.out.println("Dump of actual objects:");
                System.out.println("  level: "+level);
                for (Robot r : level.getRobots()) {
                    System.out.println("  "+r.getId()+": "+r);
                }
            }
            bsh.eval(onEnter);
            bsh.set("robot", null);
        }

        /**
         * Invokes this switch's onExit script.  You should call this every
         * time a robot exits the square occupied by this switch.
         * 
         * @param robot The robot that just left this switch
         * @throws EvalError if there is a scripting error
         */
        public void onExit(Robot robot) throws EvalError {
            if (level == null) {
                throw new IllegalStateException(
                        "Can't evaluate switch onExit: Switch is not attached to a level.");
            }
            if (onExit == null) return;
            if (!enabled) return;

            Interpreter bsh = level.getBshInterpreter();
            bsh.set("robot", robot);
            bsh.eval(onExit);
            bsh.set("robot", null);
        }

        /**
         * Returns a copy of the point that determines this switch's position.
         */
        public Point getPosition() {
            return new Point(position);
        }

        /**
         * Moves this switch to the given (x,y) position.
         * 
         * @param x the X coordinate
         * @param y the Y coordinate
         */
        public void setPosition(int x, int y) {
            this.position = new Point(x, y);
        }

        /**
         * Moves this switch to the given (x,y) position.
         * 
         * @param p The new position.  A copy of p will be made; you are free to
         * modify p after calling this method without side effects.
         */
        public void setPosition(Point p) {
            setPosition(p.x, p.y);
        }
        
        public int getX() {
            return position.x;
        }
        
        public void setX(int x) {
            position.x = x;
        }

        public int getY() {
            return position.y;
        }
        
        public void setY(int y) {
            position.y = y;
        }

        public String getId() {
            return id;
        }
        
        public void setId(String newid) {
            this.id = newid;
        }
        
        public Sprite getSprite() {
            return sprite;
        }
        
        public void setSprite(Sprite sprite) {
            this.sprite = sprite;
        }

        public String getOnEnter() {
            return onEnter;
        }
        
        public void setOnEnter(String onEnter) {
            this.onEnter = onEnter;
        }

        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public boolean isLabelEnabled() {
            return labelEnabled;
        }
        
        public void setLabelEnabled(boolean enabled) {
            this.labelEnabled = enabled;
        }
        
        public Direction getLabelDirection() {
            return labelDirection;
        }
        
        public void setLabelDirection(Direction direction) {
            this.labelDirection = direction;
        }

        @Override
        public String toString() {
            return "Switch@("+position.x+","+position.y+") \""+id+"\": "+(enabled?"en":"dis")+"abled; onEnter \""+onEnter+"\"; onExit\""+onExit+"\"";
        }

    }

    /**
     * The level's name. Should be 1-3 words or so.. not too long.
     */
    private String name;
    
    /**
     * This level's long description, in HTML markup.  This string will
     * be put into the body of an HTML document, so you don't have to
     * provide the &lt;html&gt;, &lt;head&gt; and &lt;body&gt; tags.
     */
    private String description;
    
    private List<Robot> robots = new ArrayList<Robot>();
    
    /**
     * All the switches in this level.
     * <p>
     * Implementation note: This can't be a map of points to switches 
     * because the bsh scripts are allowed to modify the switch positions
     * (and the key in the map wouldn't update accordingly).
     */
    private List<Switch> switches = new ArrayList<Switch>();

    private Square[][] map = new Square[0][0];
    private Interpreter bsh;
    private int score;
    
    /**
     * Provides property change event services.
     */
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    
    /**
     * A snapshot of this configuration which can be restored at a later time.
     */
    private LevelConfig snapshot;
    
    /**
     * Normal constructor.  Creates a LevelConfig with default settings.
     */
    public LevelConfig() {
        try {
            initInterpreter();
        } catch (EvalError e) {
            throw new RuntimeException("Couldn't add level config to bsh context");
        }
    }
    
    /**
     * Copy Constructor.  Creates a LevelConfig that is a copy of the given
     * LevelConfig.
     */
    public LevelConfig(LevelConfig copyMe) {
        this();
        copyState(copyMe, this, true);
    }
    
    private void initInterpreter() throws EvalError {
        bsh = new Interpreter();
        System.out.printf("Created new BSH interpreter 0x%x (namespace 0x%x, level 0x%x)\n", System.identityHashCode(bsh), System.identityHashCode(bsh.getNameSpace()), System.identityHashCode(LevelConfig.this));
        bsh.set("level", this);
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        pcs.firePropertyChange("name", oldName, name);
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Adds the given robot to this level config.
     * 
     * @param r The robot to add, which must have a non-null id which is unique
     * among all robots and switches within this level.
     * @throws NullPointerException If r or r.getId() is null
     * @throws IllegalArgumentException if the robot's id is the same as an existing switch or robot id in this level
     */
    public void addRobot(Robot r) {
        if (r == null) throw new NullPointerException("Null robots are not allowed");
        if (r.getId() == null) throw new NullPointerException("Null robot id not allowed");
        try {
            if (bsh.get(r.getId()) != null) {
                System.out.println("Found duplicate object in LevelConfig.addRobot(): old="+bsh.get(r.getId())+" new="+r);
                throw new IllegalArgumentException("This level already has a scripting object with id \""+r.getId()+"\"");
            }
            bsh.set(r.getId(), r);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
        robots.add(r);
        r.setLevel(this);
        pcs.firePropertyChange("robots", null, null);
    }

    /**
     * Removes the given robot from this level.  If the robot was not
     * part of this level, has no effect.
     * <p>
     * NOTE: when you remove a robot from a level, there might still be
     * scripts that refer to it.  This method does not attempt to verify
     * that the scripts still work after the robot is removed.
     * 
     * @param robot The robot to remove.
     * @throws RuntimeException (wrapping an EvalError) if there is an error 
     * removing the robot's scripting object from the BSH interpreter.
     * This does not imply that there is any checking of the scripts themselves
     * (there isn't).
     */
    public void removeRobot(Robot r) {
        try {
            bsh.unset(r.getId());
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
        robots.remove(r);
        pcs.firePropertyChange("robots", null, null);
    }

    /**
     * Sets the size of the map for this level, in squares.  The new grid of squares
     * is initialised to nulls. If there was a map before, it will be wiped out.
     */
    public void setSize(int width, int height) {
        Square[][] oldMap = map;
        Square[][] newMap = new Square[width][height];
        for (int y = 0; y < oldMap.length && y < newMap.length; y++) {
            for (int x = 0; x < oldMap[0].length && x < newMap[0].length; x++) {
                newMap[y][x] = oldMap[y][x];
            }
        }
        setMap(newMap);
    }
    
    public void setMap(Square[][] map) {
        final Square[][] oldMap = this.map;
        this.map = map;
        try {
            bsh.set("map", map);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
        pcs.firePropertyChange("map", oldMap, map);
    }
    
    public int getWidth() {
        return map.length;
    }
    
    public int getHeight() {
        return map[0].length;
    }

    public Dimension getSize() {
        return new Dimension(getWidth(), getHeight());
    }

    public void setSquare(int x, int y, Square square) {
        map[x][y] = square;
    }
    
    public Square getSquare(int x, int y) {
        return map[x][y];
    }

    public Square getSquare(float x, float y) {
        return getSquare((int) x, (int) y);
    }

    public Square getSquare(Point2D.Float position) {
        return getSquare((int) position.x, (int) position.y);
    }

    /**
     * Adds the given switch to this level's list of switches and its BSH
     * scripting context.  Also updates the switch's level reference to point
     * to this level.
     * 
     * @param s
     */
    public void addSwitch(Switch s) {
        try {
            if (bsh.get(s.getId()) != null) {
                throw new IllegalArgumentException("Level \""+name+"\" already has a scripting object with id \""+s.getId()+"\"");
            }
            bsh.set(s.getId(), s);
            switches.add(s);
            s.level = this;
            pcs.firePropertyChange("switches", null, null);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes the given switch from this level and its BSH scripting context.
     * This operation will break any scripts that referred to the removed switch,
     * and there will be no warning about that from this method, so be careful!
     */
    public void removeSwitch(Switch sw) {
        try {
            bsh.unset(sw.getId());
            switches.remove(sw);
            pcs.firePropertyChange("switches", null, null);
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
    }

    /** Returns an unmodifiable list of this level's switches. */
    public List<Switch> getSwitches() {
        return Collections.unmodifiableList(switches);
    }

    /**
     * Returns the switch located on the given map position.
     * 
     * @param position The map position.  The given point object will not be modified.
     * @return The switch located on the given map position, or null if there are no
     * switches there.
     */
    public Switch getSwitch(Point2D.Float position) {
        Point point = new Point((int) position.x, (int) position.y);
        for (Switch s : switches) {
            if (s.getPosition().equals(point)) {
                return s;
            }
        }
        return null;
    }
    
    /** Returns an unmodifiable list of this level's robots. */
    public List<Robot> getRobots() {
        return Collections.unmodifiableList(robots);
    }
    
    /**
     * Returns the actual map that this level uses.  Modifications to the map
     * will be reflected in the level config, but the LevelConfig has no way of
     * noticing these changes.  It would be better to use setSquare(x,y) to
     * modify the map.
     */
    public Square[][] getMap() {
        return map;
    }
    
    /**
     * Increases the score by the given amount.
     * 
     * <p>Causes a property change event for the property "score".
     * 
     * @param amount The amount to add to the score.  If this value is
     * negative, the score will decrease by that amount.
     */
    public void increaseScore(int amount) {
        setScore(score + amount);
    }
    
    /**
     * Sets the score for this level to the given value.
     * 
     * <p>Causes a property change event for the property "score".
     */
    public void setScore(int newScore) {
        int oldScore = score;
        this.score = newScore;
        pcs.firePropertyChange("score", oldScore, newScore);
    }
    
    /**
     * Returns the score for this level.
     * 
     * <p>This is a bound property.  You will get a property change event when
     * its value changes.
     */
    public int getScore() {
        return score;
    }
    
    /**
     * Resets this level's state to its values last time snapshotState() was called.
     */
    public void resetState() {
        if (snapshot == null) throw new IllegalStateException("No snapshot has been made yet.");
        copyState(snapshot, this, false);
    }
    
    /**
     * Takes a snapshot of this level's state so that it can be restored by a future
     * call to resetState().
     *
     */
    public void snapshotState() {
        snapshot = new LevelConfig();
        copyState(this, snapshot, false);
    }

    /**
     * Copies the value of all public properties of src to dst.  If this results
     * in changes to any bound properties, the property change events will be
     * fired.
     * 
     * @param src The level config to copy properties from
     * @param dst The level config to copy properties to.  This object may fire
     * PropertyChangeEvents as a result of this method call.
     * @param fullyIndependant If true, all mutable properties will be duplicated.
     * This is the correct behaviour for the copy constructor.  Otherwise, robots
     * and switches will not be duplicated. This is the correct case for resetState().
     */
    private static void copyState(LevelConfig src, LevelConfig dst, boolean fullyIndependant) {
        try {
            dst.initInterpreter();
            
            final Square[][] srcMap = src.getMap();
            Square[][] map = new Square[srcMap.length][srcMap[0].length];
            for (int x = 0; x < map.length; x++) {
                for (int y = 0; y < map[0].length; y++) {

                    /* Square objects are immutable, so they can be shared */
                    map[x][y] = srcMap[x][y];
                }
            }
            dst.setMap(map);
            
            dst.setName(src.getName());
            dst.setDescription(src.getDescription());
            
            // need to use addRobot() for each robot to get them into the bsh interpreter
            dst.robots = new ArrayList<Robot>();
            for (Robot r : src.robots) {
                Robot robotToAdd;
                if (fullyIndependant) {
                    robotToAdd = new Robot(r, dst);
                } else {
                    robotToAdd = r;
                }
                dst.addRobot(robotToAdd);
            }
            dst.setScore(src.score);

            // addSwitch() adds the switch to the BSH interpreter
            dst.switches = new ArrayList<Switch>();
            for (Switch s : src.switches) {
                Switch switchToAdd;
                if (fullyIndependant) {
                    switchToAdd = new Switch(s);
                } else {
                    switchToAdd = s;
                }
                dst.addSwitch(switchToAdd);
            }
        } catch (EvalError e) {
            throw new RuntimeException(e);
        }
    }
    
    // Property change listener methods
    public void addPropertyChangeListener(PropertyChangeListener l) { pcs.addPropertyChangeListener(l); }
    public void addPropertyChangeListener(String property, PropertyChangeListener l) { pcs.addPropertyChangeListener(property, l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }

    public Interpreter getBshInterpreter() {
        return bsh;
    }
}
package net.bluecow.robot;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import net.bluecow.robot.GameConfig.SquareConfig;
import net.bluecow.robot.LevelConfig.Switch;
import net.bluecow.robot.sprite.Sprite;

/**
 * Playfield
 */
public class Playfield extends JPanel {
    
    private class RoboStuff {
        private Robot robot;
        private Composite composite;
        
        public RoboStuff(Robot robot, Composite composite) {
            this.robot = robot;
            this.composite = composite;
        }

        public Robot getRobot() {
            return robot;
        }

        public Composite getComposite() {
            return composite;
        }
    }
    
    /**
     * The config for the whole game (currently used only to find out
     * the total score).
     */
    private GameConfig game;
    
    private LevelConfig level;
    
    private int squareWidth = 25;
    
    private String winMessage;
    
    private Integer frameCount;
    
    private List<RoboStuff> robots;

    /**
     * Controls whether or not labels will be displayed by fading the opacity
     * toward zero when false and toward one when true.
     */
    private boolean labellingOn = true;
    
    /**
     * The opacity of a label.  Fades up and down in paint() according to whether
     * or not labellingOn is set.
     */
    private float labelOpacity = 1.0f;
    
    /**
     * The amount that the label opacity fades up or down per frame.
     */
    private float labelFadeStep = 0.1f;

    /**
     * The colour that drawLabel() will use to paint the box underneath labels.
     */
    private Color boxColor = new Color(.5f, .5f, .5f, .5f);

    /**
     * The colour that drawLabel() will use to paint the text of labels.
     */
    private Color labelColor = Color.WHITE;

    /**
     * The number of milliseconds to delay between frames when the async repaint
     * manager is repainting this playfield.
     */
    private int frameDelay = 50;
    
    /**
     * Creates a new playfield with the specified map.
     * 
     * @param map The map.
     */
    public Playfield(GameConfig game, LevelConfig level) {
        setGame(game);
        setLevel(level);
        setupKeyboardActions();
    }

    final void setGame(GameConfig game) {
        this.game = game;
    }
    
    /**
     * Sets the value of level to the given level, and resets the list of robostuff
     * to contain only the robots described in the level.
     *
     * @param level
     */
    final void setLevel(LevelConfig level) {
        robots = new ArrayList<RoboStuff>();
        this.level = level;
        for (Robot r : level.getRobots()) {
            addRobot(r, AlphaComposite.SrcOver);
        }
    }
    
    /**
     * Adds the given robot to this playfield.  The robot will be drawn with
     * the specified composite operation.
     */
    public final void addRobot(Robot robot, Composite drawComposite) {
        robots.add(new RoboStuff(robot, drawComposite));
        repaint();
    }
    
    /**
     * Removes the given robot from this playfield.
     */
    public final void removeRobot(Robot robot) {
        for (Iterator<RoboStuff> it = robots.iterator(); it.hasNext(); ) {
            if (it.next().getRobot() == robot) {
                it.remove();
            }
        }
        repaint();
    }
    
    private void setupKeyboardActions() {
        // no actions right now
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        Square[][] squares = level.getMap();
        for (int i = 0; i < squares.length; i++) {
            for (int j = 0; j < squares[0].length; j++) {
                Rectangle r = new Rectangle(i*squareWidth, j*squareWidth, squareWidth, squareWidth);
                if (squares[i][j] != null) {
                    squares[i][j].getSprite().paint(g2, r.x, r.y);
                } else {
                    g2.setColor(Color.red);
                    g2.fillRect(r.x, r.y, r.width, r.height);
                    g2.setColor(Color.white);
                    g2.drawString("null", r.x, r.y+10);
                }
                
            }
        }
        
        for (LevelConfig.Switch s : level.getSwitches()) {
            Point p = s.getLocation();
            s.getSprite().paint(g2, p.x*squareWidth, p.y*squareWidth);
            if (!s.isEnabled()) {
                g2.setColor(Color.RED);
                int x = p.x*squareWidth;
                int y = p.y*squareWidth;
                g2.drawLine(x, y, x+squareWidth, y+squareWidth);
                g2.drawLine(x, y+squareWidth, x+squareWidth, y);
            }
        }
        
        Composite backupComposite = g2.getComposite();
        for (RoboStuff rs : robots) {
            Robot robot = rs.getRobot();
            g2.setComposite(rs.getComposite());

            Sprite sprite = robot.getSprite();
            Point2D.Float roboPos = robot.getPosition();
            AffineTransform backupXform = g2.getTransform();

            g2.translate(
                    (squareWidth * roboPos.x) - (sprite.getWidth() / 2.0),
                    (squareWidth * roboPos.y) - (sprite.getHeight() / 2.0));
            
            AffineTransform iconXform = new AffineTransform();
            iconXform.rotate(robot.getIconHeading(), sprite.getWidth()/2.0, sprite.getHeight()/2.0);
            g2.transform(iconXform);
            sprite.paint(g2, 0, 0);
            g2.setTransform(backupXform);
        }
        g2.setComposite(backupComposite);
        
        FontMetrics fm = getFontMetrics(getFont());
        if (frameCount != null) {
            String fc = String.format("%4d", frameCount);
            int width = fm.stringWidth(fc);
            int height = fm.getHeight();
            int x = getWidth() - width - 3;
            int y = 3;
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, width, height);
            g2.setColor(Color.WHITE);
            g2.drawString(fc, x, y + height - fm.getDescent());
        }

        {
            String score = String.format("Overall Score: %4d", game.getScore());
            int width = fm.stringWidth(score);
            int height = fm.getHeight();
            int x = getWidth() - width - 3;
            int y = 3 + height;
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, width, height);
            g2.setColor(Color.WHITE);
            g2.drawString(score, x, y + height - fm.getDescent());
        }        

        {
            String levelScore = String.format("%s Score: %4d", level.getName(), level.getScore());
            int width = fm.stringWidth(levelScore);
            int height = fm.getHeight();
            int x = getWidth() - width - 3;
            int y = 3 + height*2;
            g2.setColor(Color.BLACK);
            g2.fillRect(x, y, width, height);
            g2.setColor(Color.WHITE);
            g2.drawString(levelScore, x, y + height - fm.getDescent());
        }

        if (labelOpacity > 0.0) {
            for (RoboStuff rs : robots) {
                Robot robot = rs.getRobot();
                drawLabel(g2, fm, robot.getName(), robot.getPosition());
            }
            
            for (Switch s : level.getSwitches()) {
                drawLabel(g2, fm, s.getLabel(), s.getLocation());
            }
        }
        
        if (winMessage != null) {
            g2.setFont(g2.getFont().deriveFont(50f));
            g2.setColor(Color.BLACK);
            g2.drawString(winMessage, 20, getHeight()/2);
            g2.setColor(Color.RED);
            g2.drawString(winMessage, 15, getHeight()/2-5);
        }
    }

    /**
     * Tells all the sprites and effects to get ready for the next frame.
     */
    private void nextFrame() {
        for (SquareConfig sc : game.getSquareTypes()) {
            sc.getSprite().nextFrame();
        }
        
        for (LevelConfig.Switch s : level.getSwitches()) {
            s.getSprite().nextFrame();
        }

        for (RoboStuff rs : robots) {
            rs.getRobot().getSprite().nextFrame();
        }

        if (labellingOn) {
            labelOpacity = (float) Math.min(1.0, labelOpacity + labelFadeStep);
        } else {
            labelOpacity = (float) Math.max(0.0, labelOpacity - labelFadeStep);
        }
    }

    /**
     * Draws a label for the centre of the square at the given position.
     */
    private void drawLabel(Graphics2D g2, FontMetrics fm, String label, Point position) {
        drawLabel(g2, fm, label, new Point2D.Float(position.x + 0.5f, position.y + 0.5f));
    }
    
    /**
     * Draws a label over a background box with an arrow to given square position.
     * 
     * <p>See also {@link #squareWidth}, {@link #boxColor}, and {@link #labelColor}.
     * 
     * @param position The map position.  This is not a screen coordinate; it's
     * a map location.  For example, the point (x,y) = (3.5, 2.5) is at the screen
     * position (3.5*squareWidth, 2.5*squareWidth).
     */
    private void drawLabel(Graphics2D g2, FontMetrics fm, String label, Point2D.Float position) {
        if (label == null) return;
        float x = position.x * squareWidth;
        float y = position.y * squareWidth;
        Composite backupComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, labelOpacity));
        g2.setColor(boxColor);
        GeneralPath arrow = new GeneralPath();
        arrow.moveTo(x, y);
        arrow.lineTo(x - 10, y + 5);
        arrow.lineTo(x - 7, y + 5);
        arrow.lineTo(x - 7, y + 10);
        arrow.lineTo(x + 7, y + 10);
        arrow.lineTo(x + 7, y + 5);
        arrow.lineTo(x + 10, y + 5);
        arrow.lineTo(x, y);
        g2.fill(arrow);
        Rectangle box = new Rectangle(
                (int) x - 15, (int) y + 10 ,
                fm.stringWidth(label) + fm.getHeight()*2, fm.getHeight()*2);
        g2.fillRoundRect(box.x, box.y, box.width, box.height, 4, 4);
        g2.setColor(labelColor);
        g2.drawString(label, box.x + fm.getHeight(), box.y + fm.getHeight()/2 + fm.getAscent());
        g2.setComposite(backupComposite);
    }
    
    public Dimension getPreferredSize() {
        return new Dimension(level.getWidth() * getSquareWidth(),
                			 level.getHeight() * getSquareWidth());
    }
    
    // ACCESSORS AND MUTATORS
    
    public int getSquareWidth() {
        return squareWidth;
    }

    public void setSquareWidth(int squareWidth) {
        this.squareWidth = squareWidth;
    }
    
    public Square getSquareAt(Point p) {
        return level.getSquare(p.x, p.y);
    }

    public Square getSquareAt(Point2D.Float p) {
        return level.getSquare(p.x, p.y);
    }
    
    public void setWinMessage(String m) {
        winMessage = m;
        repaint();
    }
    
    public void setFrameCount(Integer c) {
        frameCount = c;
        nextFrame();
    }
    
    /**
     * Returns the LevelConfig that determines this playfield's configuration.
     */
    public LevelConfig getLevel() {
        return level;
    }
    
    public boolean isLabellingOn() {
        return labellingOn;
    }
    
    public void setLabellingOn(boolean labellingOn) {
        this.labellingOn = labellingOn;
    }
    
    /**
     * This flag controls the asynchronous versus synchronous repaint mode of
     * the playfield.  When it is in synchronous mode, outside code has to trigger
     * each repaint when it wants a new frame.  When in asynchronous mode, the
     * playfield will repaint itself periodically if necessary (for example, because
     * the labels are still in the process of fading in or out).
     * 
     * <p>As a rule of thumb, this flag should be set <tt>true</tt> when the game
     * is under the control of the Swing UI, and <tt>false</tt> when the game loop
     * is controlling all the repaints.
     */
    public void setAsyncRepaint(boolean asyncRepaint) {
        repaintManager.setEnabled(asyncRepaint);
    }

    private AsyncRepaintManager repaintManager = new AsyncRepaintManager(frameDelay);
    
    private class AsyncRepaintManager implements ActionListener, AncestorListener {
        private boolean enabled = true;
        private Timer timer;
        
        AsyncRepaintManager(int delay) {
            Playfield.this.addAncestorListener(this);
            timer = new Timer(delay, this);
            timer.start();
        }

        public synchronized void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public synchronized void actionPerformed(ActionEvent e) {
            if (enabled) {
                nextFrame();
                repaint();
            }
        }

        
        // AncestorListener implementation: kills the timer when this component goes away

        public void ancestorAdded(AncestorEvent event) {
            if (!timer.isRunning()) timer.start();
        }

        public void ancestorRemoved(AncestorEvent event) {
            timer.stop();
        }

        public void ancestorMoved(AncestorEvent event) {
            // don't care
        }
    }
}
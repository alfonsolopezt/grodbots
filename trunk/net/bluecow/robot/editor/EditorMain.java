/*
 * Created on Aug 25, 2006
 *
 * This code belongs to Jonathan Fuerth
 */
package net.bluecow.robot.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.bluecow.robot.FileFormatException;
import net.bluecow.robot.GameConfig;
import net.bluecow.robot.LevelConfig;
import net.bluecow.robot.Playfield;
import net.bluecow.robot.Robot;
import net.bluecow.robot.RobotUtils;
import net.bluecow.robot.GameConfig.SensorConfig;
import net.bluecow.robot.GameConfig.SquareConfig;
import net.bluecow.robot.LevelConfig.Switch;
import net.bluecow.robot.sprite.Sprite;
import net.bluecow.robot.sprite.SpriteManager;

public class EditorMain {

    private class LoadProjectAction extends AbstractAction {
        
        
        public LoadProjectAction() {
            super("Open Project...");
            putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        }
        
        public void actionPerformed(ActionEvent e) {
            Project proj = promptUserForProject();
            if (proj != null) {
                setProject(proj);
            }
        }
    }
    
    private class SaveLevelPackAction extends AbstractAction {
        
        public SaveLevelPackAction() {
            super("Save Level Pack...");
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
        }
        
        public void actionPerformed(ActionEvent e) {
            Preferences recentFiles = RobotUtils.getPrefs().node("recentGameFiles");
            Writer out = null;
            try {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Save Level Pack");
                fc.setCurrentDirectory(new File(recentFiles.get("0", System.getProperty("user.home"))));
                int choice = fc.showSaveDialog(frame);
                if (choice == JFileChooser.APPROVE_OPTION) {
                    project.saveLevelPack(fc.getSelectedFile());
                    RobotUtils.updateRecentFiles(recentFiles, fc.getSelectedFile());
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Save Failed: "+ex.getMessage());
            } catch (BackingStoreException ex) {
                System.out.println("Couldn't update user prefs");
                ex.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                } catch (IOException e1) {
                    System.out.println("Bad luck.. couldn't close output file!");
                    e1.printStackTrace();
                }
            }
        }
    }

    private Action addSquareTypeAction = new AbstractAction("Add Square Type") {
        public void actionPerformed(ActionEvent e) {
            SquareConfig squareConfig = new GameConfig.SquareConfig();
            JDialog d = makeSquarePropsDialog(frame, project, squareConfig);
            d.setModal(true);
            d.setVisible(true);
            project.getGameConfig().addSquareType(squareConfig);
        }
    };
    
    private Action addSensorTypeAction = new AbstractAction("Add Sensor Type") {
        public void actionPerformed(ActionEvent e) {
            SensorConfig sensorConfig = new GameConfig.SensorConfig("");
            JDialog d = makeSensorPropsDialog(frame, project.getGameConfig(), sensorConfig);
            d.setModal(true);
            d.setVisible(true);
            project.getGameConfig().addSensorType(sensorConfig);
        }
    };

    private Action addLevelAction = new AbstractAction("Add Level") {
        public void actionPerformed(ActionEvent e) {
            LevelConfig level = new LevelConfig();
            level.setName("New Level");
            project.getGameConfig().addLevel(level);
            levelChooser.setSelectedValue(level, true);
        }
    };

    /**
     * The preferences node that stores a list of most recently saved
     * and opened project locations.
     */
    private static Preferences recentProjects = RobotUtils.getPrefs().node("recentProjects");

    /**
     * The project this editor is currently editing.
     */
    private Project project;

    private JFrame frame;
    private JPanel levelEditPanel;
    private LevelEditor editor;
    private JList levelChooser;
    private LevelChooserListModel levelChooserListModel;
    private SensorTypeListModel sensorTypeListModel;
    private SquareChooserListModel squareChooserListModel;
    
    private LoadProjectAction loadProjectAction = new LoadProjectAction();
    private SaveLevelPackAction saveLevelPackAction = new SaveLevelPackAction();

    
    private static JDialog makeSensorPropsDialog(final JFrame parent, GameConfig gc, final SensorConfig sc) {
        final JDialog d = new JDialog(parent, "Sensor Type Properties");
        final JTextField nameField = new JTextField(sc.getId() == null ? "" : sc.getId(), 20);
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sc.setId(nameField.getText());
                    d.dispose();
                } catch (Exception ex) {
                    showException(parent, "Couldn't apply sensor config", ex);
                }
            }
        });

        // set up the form
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel cp = new JPanel(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.insets = new Insets(4, 4, 4, 4);
        
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Square Type Name:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(nameField, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        cp.add(okButton, gbc);

        cp.getActionMap().put("cancel", new DialogCancelAction(d));
        cp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        d.setContentPane(cp);
        d.getRootPane().setDefaultButton(okButton);
        d.pack();
        return d;
    }
    
    public static Project promptUserForProject() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choose a Robot Project Directory");
        fc.setCurrentDirectory(new File(recentProjects.get("0", System.getProperty("user.home"))));
        int choice = fc.showOpenDialog(null);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            try {
                Project proj = Project.load(f);
                RobotUtils.updateRecentFiles(recentProjects, fc.getSelectedFile());
                return proj;
            } catch (BackingStoreException ex) {
                System.out.println("Couldn't update user prefs");
                ex.printStackTrace();
            } catch (FileFormatException ex) {
                RobotUtils.showFileFormatException(ex);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Could not find file '"+f.getPath()+"'");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        null,
                        "Couldn't load the levels:\n\n"
                           +ex.getMessage()+"\n\n"
                           +"A stack trace is available on the Java Console.",
                        "Load Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }

        // either load failed, or user cancelled
        return null;
    }

    private static JDialog makeSquarePropsDialog(final JFrame parent, final Project project, final SquareConfig sc) {
        final GameConfig gc = project.getGameConfig();
        final JDialog d = new JDialog(parent, "Square Type Properties");
        final JTextField nameField = new JTextField(sc.getName() == null ? "" : sc.getName(), 20);
        final JTextField mapCharField = new JTextField(String.valueOf(sc.getMapChar()));
        final JCheckBox occupiableBox = new JCheckBox("Occupiable", sc.isOccupiable());
        final JList sensorTypesList = new JList(gc.getSensorTypes().toArray());
        sensorTypesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        // have to jump through hoops to make an array of selection indices
        List<Integer> sensorsToSelect = new ArrayList<Integer>();
        for (GameConfig.SensorConfig sensor : sc.getSensorTypes()) {
            sensorsToSelect.add(gc.getSensorTypes().indexOf(sensor));
        }
        int[] selectionIndices = new int[sensorsToSelect.size()];
        for (int i = 0; i < sensorsToSelect.size(); i++) {
            selectionIndices[i] = sensorsToSelect.get(i);
        }
        sensorTypesList.setSelectedIndices(selectionIndices);
        
        final JComboBox spritePathField = new JComboBox(new ResourcesComboBoxModel(project));
        if (sc.getSprite() != null) {
            spritePathField.setSelectedItem(sc.getSprite().getAttributes().get(Sprite.KEY_HREF));
        }
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sc.setName(nameField.getText());
                    sc.setMapChar(mapCharField.getText().charAt(0));
                    sc.setOccupiable(occupiableBox.isSelected());
                    List<SensorConfig> sensorTypes = new ArrayList<SensorConfig>();
                    Object[] selectedItems = sensorTypesList.getSelectedValues();
                    for (Object sensor : selectedItems) {
                        sensorTypes.add((SensorConfig) sensor);
                    }
                    sc.setSensorTypes(sensorTypes);
                    sc.setSprite(SpriteManager.load(
                                            gc.getResourceLoader(),
                                            (String) spritePathField.getSelectedItem()));
                    d.dispose();
                } catch (Exception ex) {
                    showException(parent, "Couldn't apply square config", ex);
                }
            }
        });

        // set up the form
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel cp = new JPanel(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.insets = new Insets(4, 4, 4, 4);
        
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Square Type Name:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(nameField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Map Character:"), gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(mapCharField, gbc);
        
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel(""), gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.LINE_START;
        cp.add(occupiableBox, gbc);

        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Sensor Types Activated:"), gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        cp.add(new JScrollPane(sensorTypesList), gbc);

        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Sprite File Location in Project:"), gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(spritePathField, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        cp.add(okButton, gbc);

        cp.getActionMap().put("cancel", new DialogCancelAction(d));
        cp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        d.setContentPane(cp);
        d.getRootPane().setDefaultButton(okButton);
        d.pack();
        return d;
    }
    
    /**
     * Creates a JDialog with a GUI for editing the properties of the given
     * robot.  The GUI will default to describing the current state of the robot.
     * The dialog has an OK button which, when pressed, will update the robot's
     * properties to reflect the new values in the GUI.
     * 
     * @param parent The frame that owns this dialog
     * @param project The project that the robot ultimately belongs to
     * @param robot The robot to edit
     * @param okAction An action to perform after the OK button has been
     * presses and robot's properties have been updated.  This action will
     * only be invoked if the user OK's the dialog; it will not be invoked
     * (and the robot properties will not be modified) if the user cancels
     * the dialog.  Also, you can safely pass in <tt>null</tt> for this
     * action if you don't need to do anything when the user hits OK. 
     * @return A non-modal JDialog which has been pack()ed, but not set visible.
     * You are free to setModal(true) on the dialog before displaying it if
     * you want it to be modal. 
     */
    private static JDialog makeRobotPropsDialog(final JFrame parent,
            final Project project, final LevelConfig level, final Robot robot, final ActionListener okAction) {
        final JDialog d = new JDialog(parent, "Switch Properties");

        final JTextField idField = new JTextField(robot.getId());
        final JTextField labelField = new JTextField(robot.getLabel());
        final JSpinner xPosition = new JSpinner(new SpinnerNumberModel(robot.getX(), 0.0, level.getWidth(), robot.getStepSize()));
        final JSpinner yPosition = new JSpinner(new SpinnerNumberModel(robot.getY(), 0.0, level.getHeight(), robot.getStepSize()));
        final JComboBox spritePathField = new JComboBox(new ResourcesComboBoxModel(project));
        if (robot.getSprite() != null) {
            spritePathField.setSelectedItem(robot.getSprite().getAttributes().get(Sprite.KEY_HREF));
        }
        final JSpinner evalsPerStep = new JSpinner(new SpinnerNumberModel(robot.getEvalsPerStep(), 1, null, 1));
        final JSpinner stepSize = new JSpinner(new SpinnerNumberModel(new Double(robot.getStepSize()), 0.01, null, 0.01));
        
        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    robot.setId(idField.getText());
                    robot.setLabel(labelField.getText());
                    Point2D pos = new Point2D.Double(
                            (Double) xPosition.getValue(),
                            (Double) yPosition.getValue());
                    robot.setPosition(pos);
                    robot.setSprite(SpriteManager.load(
                            project.getGameConfig().getResourceLoader(),
                            (String) spritePathField.getSelectedItem()));
                    robot.setEvalsPerStep((Integer) evalsPerStep.getValue());
                    robot.setStepSize(((Double) stepSize.getValue()).floatValue());
                    
                    if (okAction != null) {
                        okAction.actionPerformed(e);
                    }

                } catch (Exception ex) {
                    showException(parent, "Couldn't update Robot properties", ex);
                }
                
                d.setVisible(false);
                d.dispose();
            }
        });
        
        // set up the form
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel cp = new JPanel(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.insets = new Insets(4, 4, 4, 4);
        
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Robot ID:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(idField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Position:"), gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(xPosition, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(yPosition, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Label:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(labelField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Sprite Path:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(spritePathField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Evals per step:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(evalsPerStep, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Step size (squares):"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(stepSize, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        cp.add(okButton, gbc);

        cp.getActionMap().put("cancel", new DialogCancelAction(d));
        cp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        d.setContentPane(cp);
        d.getRootPane().setDefaultButton(okButton);
        d.pack();
        return d;
    }
    
    /**
     * Works very much like {@link #makeRobotPropsDialog(JFrame, Project, Robot, ActionListener)},
     * but is for editing switch properties.
     */
    private static JDialog makeSwitchPropsDialog(final JFrame parent,
            final Project project, final LevelConfig level, final Switch sw,
            final ActionListener okAction) {
        final JDialog d = new JDialog(parent, "Switch Properties");

        final JTextField idField = new JTextField(sw.getId());
        final JCheckBox enabledBox = new JCheckBox("Start enabled", sw.isEnabled());
        final JSpinner xPosition = new JSpinner(new SpinnerNumberModel(sw.getX(), 0, level.getWidth(), 1));
        final JSpinner yPosition = new JSpinner(new SpinnerNumberModel(sw.getY(), 0, level.getHeight(), 1));
        final JTextField labelField = new JTextField(sw.getLabel());
        final JComboBox spritePathField = new JComboBox(new ResourcesComboBoxModel(project));
        if (sw.getSprite() != null) {
            spritePathField.setSelectedItem(sw.getSprite().getAttributes().get(Sprite.KEY_HREF));
        }
        final JTextArea onEnterArea = new JTextArea(sw.getOnEnter(), 6, 15);

        final JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    sw.setId(idField.getText());
                    sw.setEnabled(enabledBox.isSelected());
                    Point pos = new Point();
                    pos.x = (Integer) xPosition.getValue();
                    pos.y = (Integer) yPosition.getValue();
                    sw.setPosition(pos);
                    sw.setLabel(labelField.getText());
                    sw.setSprite(SpriteManager.load(
                            project.getGameConfig().getResourceLoader(),
                            (String) spritePathField.getSelectedItem()));
                    sw.setOnEnter(onEnterArea.getText());
                    
                    if (okAction != null) {
                        okAction.actionPerformed(e);
                    }

                } catch (Exception ex) {
                    showException(parent, "Couldn't update Switch properties", ex);
                }
                d.setVisible(false);
                d.dispose();
            }
        });
        
        // set up the form
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel cp = new JPanel(new GridBagLayout());
        gbc.weighty = 0.0;
        gbc.insets = new Insets(4, 4, 4, 4);
        
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Switch ID:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(idField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel(""), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(enabledBox, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Position:"), gbc);

        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(xPosition, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(yPosition, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Label:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(labelField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("Sprite:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(spritePathField, gbc);

        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        cp.add(new JLabel("On Enter Script:"), gbc);

        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cp.add(new JScrollPane(onEnterArea), gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.NONE;
        cp.add(okButton, gbc);
        
        cp.getActionMap().put("cancel", new DialogCancelAction(d));
        cp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        cp.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        d.setContentPane(cp);
        d.getRootPane().setDefaultButton(okButton);
        d.pack();
        return d;
    }
    
    
    
    public EditorMain(Project project) {
        frame = new JFrame("Robot Level Editor");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                confirmExit();
            }
        });
        
        final GameConfig myGameConfig = project.getGameConfig();
        
        frame.getContentPane().setLayout(new BorderLayout(8, 8));
        ((JComponent) frame.getContentPane()).setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        
        JPanel levelChooserPanel = new JPanel(new BorderLayout());
        levelChooserListModel = new LevelChooserListModel(myGameConfig);
        levelChooser = new JList(levelChooserListModel);
        levelChooser.setCellRenderer(new LevelChooserListRenderer());
        levelChooser.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        levelChooser.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                LevelConfig level = (LevelConfig) levelChooser.getSelectedValue();
                setLevelToEdit(level);
            }
        });
        levelChooserPanel.add(new JLabel("Levels"), BorderLayout.NORTH);
        levelChooserPanel.add(new JScrollPane(levelChooser), BorderLayout.CENTER);
        JButton addLevelButton = new JButton(addLevelAction);
        levelChooserPanel.add(addLevelButton, BorderLayout.SOUTH);
        
        frame.add(levelChooserPanel, BorderLayout.WEST);
        
        levelEditPanel = new JPanel();
        levelEditPanel.add(new JLabel("To edit a level, select it from the list on the left-hand side."));
        frame.add(levelEditPanel, BorderLayout.CENTER);
        
        JPanel sensorTypesPanel = new JPanel(new BorderLayout());
        sensorTypeListModel = new SensorTypeListModel(myGameConfig);
        final JList sensorTypesList = new JList(sensorTypeListModel);
        sensorTypesPanel.add(new JLabel("Sensor Types"), BorderLayout.NORTH);
        sensorTypesPanel.add(new JScrollPane(
                    sensorTypesList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.CENTER);
        sensorTypesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Click");
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    GameConfig.SensorConfig sc = (SensorConfig) sensorTypesList.getSelectedValue();
                    System.out.println("Double Click (selectedValue="+sc+")");
                    if (sc != null) {
                        makeSensorPropsDialog(frame, getProject().getGameConfig(), sc).setVisible(true);
                    }
                }
            }
        });
        sensorTypesPanel.add(new JButton(addSensorTypeAction), BorderLayout.SOUTH);
        
        squareChooserListModel = new SquareChooserListModel(myGameConfig);
        final JList squareList = new JList(squareChooserListModel);
        squareList.setCellRenderer(new SquareChooserListRenderer());
        squareList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                System.out.println("Square List selection changed. squares="+myGameConfig.getSquareTypes());
                editor.setPaintingSquareType((SquareConfig) squareList.getSelectedValue());
            }
        });
        JPanel squareListPanel = new JPanel(new BorderLayout());
        squareListPanel.add(new JLabel("Square Types"), BorderLayout.NORTH);
        squareListPanel.add(
                new JScrollPane(squareList,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.CENTER);
        squareList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("Click");
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    GameConfig.SquareConfig sc = (SquareConfig) squareList.getSelectedValue();
                    System.out.println("Double Click (selectedValue="+sc+")");
                    if (sc != null) {
                        makeSquarePropsDialog(frame, getProject(), sc).setVisible(true);
                    }
                }
            }
        });
        squareListPanel.add(new JButton(addSquareTypeAction), BorderLayout.SOUTH);
        
        JSplitPane eastPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        eastPanel.setTopComponent(sensorTypesPanel);
        eastPanel.setBottomComponent(squareListPanel);
        
        frame.add(eastPanel, BorderLayout.EAST);
        
        setupMenu();
        
        setProject(project);
        
        frame.pack();
        frame.setVisible(true);
    }
    
    /**
     * Sets up all the menu bar crap and adds it to the frame.
     */
    private void setupMenu() {
        JMenuBar mb = new JMenuBar();
        JMenu m;
        mb.add (m = new JMenu("File"));
        m.add(new JMenuItem(loadProjectAction));
        m.add(new JMenuItem(saveLevelPackAction));
        m.add(new JMenuItem(exitAction));
        
        frame.setJMenuBar(mb);
    }
    
    /**
     * Invokes the confirmExit() method.
     */
    private Action exitAction = new AbstractAction("Exit") {
        public void actionPerformed(ActionEvent e) {
            confirmExit();
        }
    };

    /**
     * Presents an "are you sure?" dialog and exits the application if the user
     * responds affirmitavely.
     *
     */
    public void confirmExit() {
        int choice = JOptionPane.showConfirmDialog(frame, "Do you really want to quit?", "Quit the level editor", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    
    private void setProject(Project proj) {
        this.project = proj;
        sensorTypeListModel.setGame(proj.getGameConfig());
        squareChooserListModel.setGame(proj.getGameConfig());
        levelChooserListModel.setGame(proj.getGameConfig());
        levelChooser.setSelectedIndex(0);
    }
    
    private Project getProject() {
        return project;
    }

    private void setLevelToEdit(LevelConfig level) {
        if (levelEditPanel != null) {
            frame.remove(levelEditPanel);
        }
        levelEditPanel = new JPanel(new BorderLayout(8, 8));
        editor = new LevelEditor(project.getGameConfig(), level);
        JPanel floaterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        floaterPanel.add(editor);
        levelEditPanel.add(floaterPanel, BorderLayout.CENTER);
        
        levelEditPanel.add(makeLevelPropsPanel(level), BorderLayout.NORTH);
        
        frame.add(levelEditPanel, BorderLayout.CENTER);
        frame.validate();
    }
    
    private JPanel makeLevelPropsPanel(final LevelConfig level) {
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel p = new JPanel(new GridBagLayout());
        
        // all components in the layout will have 4px of space around them
        gbc.insets = new Insets(4, 4, 4, 4);
        
        gbc.weighty = 0.0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.LINE_END;
        p.add(new JLabel("Level Name:"), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        final JTextField levelNameField = new JTextField(level.getName());
        levelNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { update(); }
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            void update() { level.setName(levelNameField.getText()); }
        });
        p.add(levelNameField, gbc);
        
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        p.add(new JLabel("Robots"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        p.add(new JLabel("Switches"), gbc);
        
        final JList robotChooser = new JList(new RobotListModel(level));
        final JList switchChooser = new JList(new SwitchListModel(level));

        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        robotChooser.setCellRenderer(new RobotListRenderer());
        robotChooser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Robot robot = (Robot) robotChooser.getSelectedValue();
                    if (robot != null) {
                        JDialog d = makeRobotPropsDialog(frame, project, level, robot, null);
                        d.setVisible(true);
                    }
                }
            }
        });
        robotChooser.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Robot r = (Robot) robotChooser.getSelectedValue();
                if (r == null) {
                    editor.setSpotlightLocation(null);
                    ((Playfield) editor).setSpotlightRadius(0.0);
                } else {
                    switchChooser.clearSelection();
                    editor.setSpotlightLocation(r.getPosition());
                    editor.setSpotlightRadius(1.0);
                }
            }
        });
        p.add(new JScrollPane(robotChooser), gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        switchChooser.setCellRenderer(new SwitchListRenderer());
        switchChooser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Switch sw = (Switch) switchChooser.getSelectedValue();
                    if (sw != null) {
                        JDialog d = makeSwitchPropsDialog(frame, project, level, sw, null);
                        d.setVisible(true);
                    }
                }
            }
        });
        switchChooser.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                Switch s = (Switch) switchChooser.getSelectedValue();
                if (s == null) {
                    editor.setSpotlightLocation(null);
                    ((Playfield) editor).setSpotlightRadius(0.0);
                } else {
                    robotChooser.clearSelection();
                    Point p = s.getPosition();
                    editor.setSpotlightLocation(new Point2D.Double(p.x+0.5, p.y+0.5));
                    editor.setSpotlightRadius(1.0);
                }
            }
        });
        p.add(new JScrollPane(switchChooser), gbc);

        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        final JButton addRobotButton = new JButton("Add Robot");
        addRobotButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Robot robot = project.createRobot();
                ActionListener addRobot = new ActionListener() {
                    public void actionPerformed(ActionEvent e) { level.addRobot(robot); }
                };
                JDialog d = makeRobotPropsDialog(frame, project, level, robot, addRobot);
                d.setModal(true);
                d.setVisible(true);
            }
        });
        p.add(addRobotButton, gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        final JButton addSwitchButton = new JButton("Add Switch");
        addSwitchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Switch sw = project.createSwitch();
                ActionListener addSwitch = new ActionListener() {
                    public void actionPerformed(ActionEvent e) { level.addSwitch(sw); }
                };
                JDialog d = makeSwitchPropsDialog(frame, project, level, sw, addSwitch);
                d.setModal(true);
                d.setVisible(true);
            }
        });
        p.add(addSwitchButton, gbc);

        return p;
    }

    /**
     * Shows the given message and the exception's message and stack trace
     * in a modal dialog.
     */
    public static void showException(Component owner, String message, Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        pw.flush();
        pw.close();
        
        JTextArea ta = new JTextArea(message+"\n\n"+sw.getBuffer(), 15, 60);
        JOptionPane.showMessageDialog(owner, new JScrollPane(ta), "Error Report", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    if (!autoloadMostRecentProject()) {
                        presentWelcomeMenu();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Fatal error:\n\n"+e
                            +"\n\nMore information is available on the system or Java console.");
                    System.exit(0);
                }
            }
        });
    }

    private static boolean autoloadMostRecentProject() {
        if (recentProjects.get("0", null) == null) {
            return false;
        }
        File mostRecentProjectLocation = new File(recentProjects.get("0", null));
        if (mostRecentProjectLocation.isDirectory()) {
            try {
                Project project = Project.load(mostRecentProjectLocation);
                new EditorMain(project);
                return true;
            } catch (Exception ex) {
                System.err.println("autoloadMostRecentProject():");
                System.err.println("  Exception while opening most recent project from '"+
                        mostRecentProjectLocation.getPath()+"'. Giving up.");
                ex.printStackTrace();
            }
        } else {
            System.out.println("autoloadMostRecentProject():");
            System.out.println("  Most recent project location '"+
                        mostRecentProjectLocation.getPath()+"' isn't a directory. Giving up.");
        }
        return false;
    }

    protected static void presentWelcomeMenu() throws IOException {
        int choice = JOptionPane.showOptionDialog(
                null, 
                "Welcome to the Robot Editor.\n" +
                 "Do you want to open an existing project\n" +
                 "or start a new one?", "Robot Editor",
                JOptionPane.YES_NO_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE, null, 
                new String[] {"Quit", "Open Existing", "Create new"},
                "Create new");
        System.out.println("Choice: "+choice);
        
        Project proj = null;
        if (choice == 1) {
            // open existing
            proj = promptUserForProject();
        } else if (choice == 2) {
            // create new
            String projName = JOptionPane.showInputDialog("What will your project be called?");
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setDialogTitle("Where do you want to save your project?");
            int fcChoice = fc.showSaveDialog(null);
            if (fcChoice == JFileChooser.APPROVE_OPTION) {
                proj = Project.createNewProject(new File(fc.getSelectedFile(), projName));
            }
        }
        
        if (proj != null) {
            new EditorMain(proj);
        }
    }

}

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

public class Driver {
	
	// Declare class data
    public static String filename = "triplog.csv";
    public static ArrayList<TripPoint> trip;
    public static ArrayList<TripPoint> movingTrip;
    public static int animationTime;
    public static void main(String[] args) throws FileNotFoundException, IOException {
        JFrame frame;
        JPanel panel;
        JButton button;
        JCheckBox check;
        JComboBox<String> box;
        JMapViewer treeMap;
    	// Read file and call stop detection
        TripPoint.readFile(filename);
        TripPoint.h2StopDetection();
        trip = TripPoint.getTrip();
        movingTrip = TripPoint.getMovingTrip();
    	// Set up frame, include your name in the title
    	frame = new JFrame("Animated Trip - Jack Turk");
        frame.setMinimumSize(new Dimension(600, 400));
        // Set up Panel for input selections
        panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        // Play Button
        button = new JButton("Play");
    	c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.ipadx = 10;
        panel.add(button, c);
        // CheckBox to enable/disable stops
        check = new JCheckBox("Inclue Stops");
    	c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        panel.add(check, c);
        // ComboBox to pick animation time
        box = new JComboBox<>(new String[] {"Select", "15", "30", "60", "90"});
    	c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 0;
        panel.add(box, c);
        // Set up mapViewer
        treeMap = new JMapViewer();
        treeMap.setTileSource(new OsmTileSource.TransportMap());
        treeMap.setZoom(6);
        treeMap.setVisible(true);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(treeMap, c);
        // Add listeners for GUI components
        box.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e){
                animationTime = Integer.parseInt(box.getItemAt(box.getSelectedIndex()));
           }
        });
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Animate the appropriate trip list based on checkbox selection
                if (check.isSelected()) {
                    animateTrip(trip, animationTime, treeMap);
                } else {
                    animateTrip(movingTrip, animationTime, treeMap);
                }
                treeMap.removeAllMapPolygons();
            }
        });
         // Add a component listener to resize elements when the frame is resized
        frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                int frameWidth = frame.getContentPane().getWidth();
                int frameHeight = frame.getContentPane().getHeight();
                int topRowHeight = button.getHeight() + 10;
                int mapHeight = frameHeight - topRowHeight - 10;
                button.setPreferredSize(new Dimension(frameWidth / 3 - 10, topRowHeight));
                check.setPreferredSize(new Dimension(frameWidth / 3 - 10, topRowHeight));
                box.setPreferredSize(new Dimension(frameWidth / 3 - 10, topRowHeight));
                treeMap.setPreferredSize(new Dimension(frameWidth - 10, mapHeight));
            }
        });
        // Set the map center and zoom level
        Coordinate median = new Coordinate(trip.get(trip.size() / 2).getLat(), 
        trip.get(trip.size() / 2).getLon());
        treeMap.setDisplayPosition(median, 3);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
    }
    // Animate the trip based on selections from the GUI components
    public static void animateTrip(ArrayList<TripPoint> trip, int animationTime, JMapViewer map) {
        //Image image = Toolkit.getDefaultToolkit().getImage("arrow.png");
        ImageIcon icon = new ImageIcon("arrow.png");
        Image image = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        // Initialize trip in Coordinate form
        ArrayList<Coordinate> tripAsCoords = new ArrayList<Coordinate>();
        for(TripPoint p:trip){
            Coordinate c = new Coordinate(p.getLat(), p.getLon());
            tripAsCoords.add(c);
        }
        int waitTime = (int) Math.ceil(animationTime * 1000 / tripAsCoords.size());
        Timer timer = new Timer(waitTime, new ActionListener(){
            int i = 1;
            IconMarker arrow;
            public void actionPerformed(ActionEvent e){
                map.removeMapMarker(arrow);
                double bearing = 0;
                if(i < tripAsCoords.size() - 1){
                    double lon1 = Math.toRadians(tripAsCoords.get(i).getLon());
                    double lat1 = Math.toRadians(tripAsCoords.get(i).getLat());
                    double lon2 = Math.toRadians(tripAsCoords.get(i + 1).getLon());
                    double lat2 = Math.toRadians(tripAsCoords.get(i + 1).getLat());
                    
                    double y = Math.sin(lon2 - lon1) * Math.cos(lat2);
                    double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
                    
                    bearing = Math.toDegrees(Math.atan2(y, x));
                    if (bearing < 0) {
                    bearing += 360;
                    }
                }
                AffineTransform transform = new AffineTransform();
                transform.rotate(Math.toRadians(bearing), image.getWidth(null) / 2, image.getHeight(null) / 2);
                BufferedImage rotatedImage = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = rotatedImage.createGraphics();
                g.setTransform(transform);
                g.drawImage(image, 0, 0, null);
                g.dispose();
                arrow = new IconMarker(tripAsCoords.get(i), rotatedImage);
                map.addMapMarker(arrow);
                MapPolygonImpl connector = new MapPolygonImpl(tripAsCoords.get(i), tripAsCoords.get(i - 1), tripAsCoords.get(i - 1));
                map.addMapPolygon(connector);
                if(i < tripAsCoords.size() - 1){
                    i++;
                }
                else{
                    map.removeMapMarker(arrow);
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();
        timer.setRepeats(true);
    }
}

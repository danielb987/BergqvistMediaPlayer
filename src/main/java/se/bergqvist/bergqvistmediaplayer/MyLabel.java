package se.bergqvist.bergqvistmediaplayer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/**
 * A component similar to JLabel which wraps text.
 *
 * @author Daniel Bergqvist (C) 2026
 */
public class MyLabel extends JComponent implements MouseListener, FocusListener {

    private final int maxX;
    private final String text;
    private List<String> lines;
    private final Font font;
    private final Runnable onClick;
    private int height;
    private int ascent;
    private FontMetrics metrics;
    private boolean hasFocus;

    public MyLabel(int maxX, String text, Font font, Runnable onClick) {
        this.maxX = maxX;
        this.text = text;
        this.font = font;
        this.onClick = onClick;
        setFocusable(true);
        addMouseListener(this);
        addFocusListener(this);
//        System.out.format("Label: \"%s\"%n", text);
    }

    private List<String> splitString(Graphics g, String text, Font font, int maxX) {
        // https://www.cs.auckland.ac.nz/references/java/java1.5/tutorial/2d/text/measuringtext.html
        // get metrics from the graphics
        metrics = g.getFontMetrics(font);
        // get the height of a line of text in this font and render context
        height = metrics.getHeight();
        ascent = metrics.getAscent();

        List<String> theLines = new ArrayList<>();

        StringBuilder sb = new StringBuilder(text);

        while (metrics.stringWidth(sb.toString()) > maxX) {

            StringBuilder sbR = new StringBuilder(sb.substring(0, sb.length()-2));
            while (metrics.stringWidth(sbR.toString()) > maxX) {
                sbR.deleteCharAt(sbR.length()-1);
            }

            int split = sbR.length();
            if (sbR.lastIndexOf(" ") > 0) {
                split = sbR.lastIndexOf(" ");
            } else if (sbR.lastIndexOf(".") > 0) {
                split = sbR.lastIndexOf(".")+1;
            }

            theLines.add(sb.substring(0, split));
            sb.delete(0, split);
        }
        theLines.add(sb.toString());

        return theLines;
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(1,1);
    }

    @Override
    public Dimension getPreferredSize() {
        if (lines == null) {
            lines = splitString(getGraphics(), text, font, maxX);
        }

        Dimension d = super.getPreferredSize();

        d.width = maxX;
        d.height = metrics.getHeight() * lines.size();

//        if (d.width > maxX) {
//            d.width = maxX;
//        }
        return d;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g.create();
        if (hasFocus) {
            g2.setColor(Color.LIGHT_GRAY);
        } else {
            g2.setColor(Color.WHITE);
        }
        g2.fillRect(0, 0, maxX, lines.size() * height);
        g2.setColor(Color.BLACK);
        g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g2.setFont(font);
        for (int i=0; i < lines.size(); i++) {
            String line = lines.get(i);

            int w = metrics.stringWidth(line);
            int left = 2 + (maxX-w) / 2;

            g2.drawString(line, 2 + left, ascent + i*height);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        onClick.run();
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mousePressed(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseEntered(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void mouseExited(MouseEvent e) {
//        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void focusGained(FocusEvent e) {
        hasFocus = true;
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        hasFocus = false;
        repaint();
    }

}

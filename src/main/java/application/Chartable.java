package application;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class providing access to a real-time chart of measured data
 */
public class Chartable implements Closeable {
    public static final int WIDTH = 600;
    public static final int HEIGHT = 400;

    private static boolean disablePlotting = false;
    private static final List<JFrame> activeWindows = Collections.synchronizedList(new ArrayList<>());

    private boolean doNotPlotDelay = false;

    private double[] xAxis;
    private double[] yAxisGoodput;
    private double[] yAxisDelay;

    //private MySwingWorker mySwingWorker;
    private JFrame jframe;
    private SwingWrapper<XYChart> sw;
    private XYChart chart;

    private enum Lines {
        GOODPUT(0, "Goodput [Mbps]"),
        DELAY(1, "Delay [ms]");

        private final int axis;
        private final String title;

        Lines(int axis, String title) {
            this.axis = axis;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }

        public int getAxis() {
            return axis;
        }
    }

    public static void disablePlotting() {
        Chartable.disablePlotting = true;
    }

    public Chartable(int xAxisSize, String xAxisName, String title) {
        this(xAxisSize, xAxisName, title, false);
    }

    public Chartable(int xAxisSize, String xAxisName, String title, boolean doNotPlotDelay) {
        if (disablePlotting) {
            return;
        }
        this.doNotPlotDelay = doNotPlotDelay;

        xAxis = new double[xAxisSize];
        yAxisGoodput = new double[xAxisSize];
        yAxisDelay = new double[xAxisSize];
        // xAxis is [0:xAxisSize] inclusive both ends
        for (int i = 0; i < xAxisSize; i++) {
            xAxis[i] = i;
        }

        // Create Chart
        chart = new XYChartBuilder().width(WIDTH).height(HEIGHT).xAxisTitle(xAxisName).title(title).build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setXAxisTicksVisible(true);

        XYSeries goodputSeries = chart.addSeries(Lines.GOODPUT.toString(), xAxis, yAxisGoodput);
        goodputSeries.setYAxisGroup(Lines.GOODPUT.getAxis());
        chart.setYAxisGroupTitle(Lines.GOODPUT.getAxis(), Lines.GOODPUT.toString());

        if (!doNotPlotDelay) {
            XYSeries delaySeries = chart.addSeries(Lines.DELAY.toString(), xAxis, yAxisDelay);
            delaySeries.setYAxisGroup(Lines.DELAY.getAxis());
            chart.setYAxisGroupTitle(Lines.DELAY.getAxis(), Lines.DELAY.toString());
            //noinspection SuspiciousNameCombination
            chart.getStyler().setYAxisGroupPosition(Lines.DELAY.getAxis(), Styler.YAxisPosition.Right);
        }

        // Show it
        sw = new SwingWrapper<XYChart>(chart);

        synchronized (activeWindows) {
            jframe = sw.displayChart();
            jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            if (activeWindows.isEmpty()) {
                // if not other window exists, place it a 0,0
                jframe.setLocation(0, 0);
            } else {
                // if another window exists, place it next to it
                JFrame lastFrame = activeWindows.get(activeWindows.size() - 1);
                final Rectangle bounds = lastFrame.getBounds();

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                double screenWidth = screenSize.getWidth();
                double screenHeight = screenSize.getHeight();

                if (bounds.x + bounds.width + jframe.getWidth() <= screenWidth) {
                    // if window fits next to last window (in current screen)
                    jframe.setLocation(bounds.x + bounds.width, bounds.y);
                } else {
                    // if it does not fit next to the last window, place it in the next row
                    jframe.setLocation(0, bounds.y + bounds.height);
                }
            }
            activeWindows.add(jframe);
        }
    }

    public void plotData(int time, double goodput, double delay) {
        if (disablePlotting) {
            return;
        }
        if (doNotPlotDelay) {
            throw new IllegalStateException("Can't plot delay by instruction");
        }
        if (time >= xAxis.length) {
            return;
        }

        yAxisGoodput[time] = goodput;
        yAxisDelay[time] = delay;

        chart.updateXYSeries(Lines.GOODPUT.toString(), xAxis, yAxisGoodput, null);
        chart.updateXYSeries(Lines.DELAY.toString(), xAxis, yAxisDelay, null);
        sw.repaintChart();
    }

    public void plotData(int time, double goodput) {
        if (disablePlotting) {
            return;
        }

        if (time >= xAxis.length) {
            return;
        }

        yAxisGoodput[time] = goodput;

        chart.updateXYSeries(Lines.GOODPUT.toString(), xAxis, yAxisGoodput, null);
        sw.repaintChart();
    }

    @Override
    public void close() throws IOException {
        if (jframe != null) {
            activeWindows.remove(jframe);
            jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSING));
        }
    }
}

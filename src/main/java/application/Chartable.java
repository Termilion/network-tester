package application;

import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.IOException;

/**
 * Class providing access to a real-time chart of measured data
 */
public class Chartable implements Closeable {
    private boolean disablePlotting = false;

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

    public void disablePlotting() {
        this.disablePlotting = true;
    }

    public void initChart(int xAxisSize, String xAxisName, String title) {
        if (disablePlotting) {
            return;
        }

        xAxis = new double[xAxisSize];
        yAxisGoodput = new double[xAxisSize];
        yAxisDelay = new double[xAxisSize];
        // xAxis is [0:xAxisSize] inclusive both ends
        for (int i = 0; i < xAxisSize; i++) {
            xAxis[i] = i;
        }

        // Create Chart
        chart = new XYChartBuilder().xAxisTitle(xAxisName).title(title).build();
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setXAxisTicksVisible(true);

        XYSeries goodputSeries = chart.addSeries(Lines.GOODPUT.toString(), xAxis, yAxisGoodput);
        XYSeries delaySeries = chart.addSeries(Lines.DELAY.toString(), xAxis, yAxisDelay);

        goodputSeries.setYAxisGroup(Lines.GOODPUT.getAxis());
        delaySeries.setYAxisGroup(Lines.DELAY.getAxis());

        chart.setYAxisGroupTitle(Lines.GOODPUT.getAxis(), Lines.GOODPUT.toString());
        chart.setYAxisGroupTitle(Lines.DELAY.getAxis(), Lines.DELAY.toString());

        //noinspection SuspiciousNameCombination
        chart.getStyler().setYAxisGroupPosition(Lines.DELAY.getAxis(), Styler.YAxisPosition.Right);

        // Show it
        sw = new SwingWrapper<XYChart>(chart);
        jframe = sw.displayChart();
        jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void plotData(int time, double goodput, double delay) {
        if (disablePlotting) {
            return;
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
            jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSING));
        }
    }
}

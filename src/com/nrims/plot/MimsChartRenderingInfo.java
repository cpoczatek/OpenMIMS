package com.nrims.plot;

import java.awt.geom.Rectangle2D;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.util.ObjectUtilities;

/**
 * An extension of the <code>ChartRenderingInfo</code> class
 * that sets entities based on <code>MimsStandardEntityCollection</code>
 * instead of <code>StandardEntityCollection</code>.
 */
public class MimsChartRenderingInfo extends ChartRenderingInfo {

    /** Rendering info for the chart's plot (and subplots, if any). */
    private PlotRenderingInfo plotInfo;

    /**
     * Constructs a new instance. If an entity collection is supplied, it will
     * be populated with information about the entities in a chart.  If it is
     * <code>null</code>, no entity information (including tool tips) will
     * be collected.
     *
     * @param entities  an entity collection (<code>null</code> permitted).
     */
    public MimsChartRenderingInfo(MimsStandardEntityCollection entities) {
        setChartArea(new Rectangle2D.Double());
        this.plotInfo = new PlotRenderingInfo(this);
        setEntityCollection(entities);
    }

    /**
     * Clears the information recorded by this object.
     */
    public void clear() {
       super.clear();
       this.plotInfo = new PlotRenderingInfo(this);
    }

    /**
     * Returns the rendering info for the chart's plot.
     *
     * @return The rendering info for the plot.
     */
    public PlotRenderingInfo getPlotInfo() {
        return this.plotInfo;
    }

    /**
     * Tests this object for equality with an arbitrary object.
     *
     * @param obj  the object to test against (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ChartRenderingInfo)) {
            return false;
        }
        ChartRenderingInfo that = (ChartRenderingInfo) obj;
        if (!ObjectUtilities.equal(getChartArea(), that.getChartArea())) {
            return false;
        }
        if (!ObjectUtilities.equal(this.plotInfo, that.getPlotInfo())) {
            return false;
        }
        if (!ObjectUtilities.equal(getEntityCollection(), that.getEntityCollection())) {
            return false;
        }
        return true;
    }

}


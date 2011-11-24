package org.pulpcore.tools.png.heuristic;

public class VarianceHeuristic extends AdaptiveFilterHeuristic {
    
    public double getCompressability(byte filter, byte[] scanline) {

        int sum = Math.abs(filter);
        for (int i = 0; i < scanline.length; i++) {
            sum += Math.abs(scanline[i]);
        }

        double mean = (double)sum / (scanline.length + 1);
        double s = Math.abs(filter) - mean;
        double var = s * s;
        for (int i = 0; i < scanline.length; i++) {
            s = Math.abs(scanline[i]) - mean;
            var += s*s;
        }

        return var;// / (scanline.length + 1);
    }
}

package com.khronodragon.bluestone.util;

import java.awt.*;

public class ColorUtils {
    /**
     * Interpolate two colors, given a percentage.
     * @param fromColor the color that represents 0
     * @param toColor the color that represents 1
     * @param percent percent from "from color" to "to color", 0.0 - 1.0
     * @return the interpolated color
     */
    public static Color mixColors(Color fromColor, Color toColor, double percent) {
        double inverse_percent = 1.0 - percent;

        int red = (int) (toColor.getRed() * percent + fromColor.getRed() * inverse_percent);
        int green = (int) (toColor.getGreen() * percent + fromColor.getGreen() * inverse_percent);
        int blue = (int) (toColor.getBlue() * percent + fromColor.getBlue() * inverse_percent);

        return new Color(red, green, blue);
    }
}

package com.kdragon.bluestone.util;

import com.twelvemonkeys.image.ResampleOp;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class GraphicsUtils {
    /**
     * Interpolate two colors, given a percentage.
     * @param fromColor the color that represents 0
     * @param toColor the color that represents 1
     * @param percent percent from "from color" to "to color", 0.0 - 1.0
     * @return the interpolated color
     */
    public static Color interpolateColors(Color fromColor, Color toColor, double percent) {
        double inverse_percent = 1.0 - percent;

        int red = (int) (toColor.getRed() * percent + fromColor.getRed() * inverse_percent);
        int green = (int) (toColor.getGreen() * percent + fromColor.getGreen() * inverse_percent);
        int blue = (int) (toColor.getBlue() * percent + fromColor.getBlue() * inverse_percent);

        return new Color(red, green, blue);
    }

    /**
     * Resize a {@link BufferedImage} to the specified width and height.
     * @param img the {@link BufferedImage} to resize
     * @param newW the width to resize to
     * @param newH the height to resize to
     * @return the resized image
     */
    public static BufferedImage resizeImage(BufferedImage img, int newW, int newH) {
        return new ResampleOp(newW, newH, ResampleOp.FILTER_LANCZOS).filter(img, null);
    }
}

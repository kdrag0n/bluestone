package com.khronodragon.bluestone.util;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class GraphicsUtils {
    private static boolean opencvInitialized = false;

    private static void ensureOpencvInit() {
        if (!opencvInitialized) {
            OpenCV.loadShared();
            opencvInitialized = true;
        }
    }

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
        ensureOpencvInit();

        int curCVtype;
        switch (img.getType()) {
            case BufferedImage.TYPE_3BYTE_BGR:
                curCVtype = CvType.CV_8UC3;
                break;
            case BufferedImage.TYPE_BYTE_GRAY:
            case BufferedImage.TYPE_BYTE_BINARY:
                curCVtype = CvType.CV_8UC1;
                break;
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
                curCVtype = CvType.CV_32SC3;
                break;
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                curCVtype = CvType.CV_32SC4;
                break;
            case BufferedImage.TYPE_USHORT_GRAY:
                curCVtype = CvType.CV_16UC1;
                break;
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                curCVtype = CvType.CV_8UC4;
                break;
            default:
                throw new IllegalArgumentException("Unsupported BufferedImage type: " + img.getType());
        }
        System.out.println("t" + img.getType());

        Mat matImg = new Mat(img.getHeight(), img.getWidth(), curCVtype);
        byte[] pixels = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        matImg.put(0, 0, pixels);

        Mat newImg = new Mat(newH, newW, CvType.CV_8SC4);
        Imgproc.resize(matImg, newImg, new Size(newW, newH));
        newImg.get(0, 0, pixels);

        return img;
    }
}

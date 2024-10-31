package io.jonuuh.leaptrajectories;

import io.jonuuh.leaptrajectories.util.Color;

public class Config
{
    private static final Color lineColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Color boxColorHit = new Color(1.0F, 0.0F, 0.0F);
    private static final Color boxColorMiss = new Color(1.0F, 1.0F, 0.0F);
    private static boolean isArrowMode = true;
    private static boolean isLeaping = false;

    public static Color getLineColor()
    {
        return lineColor;
    }

    public static Color getBoxColorHit()
    {
        return boxColorHit;
    }

    public static Color getBoxColorMiss()
    {
        return boxColorMiss;
    }

    public static boolean isArrowMode()
    {
        return isArrowMode;
    }

    public static void setArrowMode(boolean isArrowMode)
    {
        Config.isArrowMode = isArrowMode;
    }

    public static void flipArrowMode()
    {
        isArrowMode = !isArrowMode;
    }

    public static boolean isLeaping()
    {
        return isLeaping;
    }

    public static void setLeaping(boolean isLeaping)
    {
        Config.isLeaping = isLeaping;
    }
}

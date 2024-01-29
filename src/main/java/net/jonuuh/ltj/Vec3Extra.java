package net.jonuuh.ltj;

import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

public class Vec3Extra extends Vec3
{
    public Vec3Extra(double x, double y, double z)
    {
        super(x, y, z);
    }

    public Vec3Extra(Vec3i vec)
    {
        super(vec);
    }

    /**
     * Return a new vector with the components of this vector divided by a number.
     *
     * @param num the num
     * @return the resulting vector
     */
    public Vec3Extra divide(double num)
    {
        return new Vec3Extra(this.xCoord / num, this.yCoord / num, this.zCoord / num);
    }

    /**
     * Return a new vector with the components of this vector multiplied by a number.
     *
     * @param num the num
     * @return the resulting vector
     */
    public Vec3Extra multiply(double num)
    {
        return new Vec3Extra(this.xCoord * num, this.yCoord * num, this.zCoord * num);
    }
}

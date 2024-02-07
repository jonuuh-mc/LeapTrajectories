package net.jonuuh.ltj.util;

public class Vec
{
    public float x;
    public float y;
    public float z;

    public Vec()
    {
        new Vec(0,0,0);
    }

    public Vec(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec(double x, double y, double z)
    {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
    }

    public Vec addVec(Vec vec)
    {
        return new Vec(this.x + vec.x, this.y + vec.y, this.z + vec.z);
    }

    public Vec multiply(float num)
    {
        return new Vec(this.x * num, this.y * num, this.z * num);
    }

    public float dot(Vec vec)
    {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }

    public float length()
    {
        return (float) Math.sqrt(this.dot(this));
    }

    public float distanceTo(Vec vec)
    {
        return (float) Math.sqrt(this.dot(new Vec(this.x - vec.x, this.y - vec.y, this.z - vec.z)));
    }

    public Vec cloneVec()
    {
        return new Vec(this.x, this.y, this.z);
    }

    @Override
    public String toString()
    {
        return "Vec(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
}

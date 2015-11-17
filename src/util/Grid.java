package util;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class Grid
{
	final float x,y,xStep,yStep;
	final int rows,cols;
	
	public Grid(float x, float y, float xStep, float yStep, int rows, int cols)
	{
		this.x = x;
		this.y = y;
		this.xStep = xStep;
		this.yStep = yStep;
		this.rows = rows;
		this.cols = cols;
	}
	
	public void forEach(BiConsumer<Float,Float> consumer)
	{
		for(int row = 0; row<rows; row++)
		{
			for(int col = 0; col<cols; col++)
			{
				consumer.accept(x+xStep*col, y+yStep*row);
			}
		}
	}
	
	public void forEachWithIndicies(QuadConsumer<Float,Float,Integer,Integer> consumer)
	{
		for(int row = 0; row<rows; row++)
		{
			for(int col = 0; col<cols; col++)
			{
				consumer.accept(x+xStep*col, y+yStep*row,col,row);
			}
		}
	}
	
	public boolean allMatch(BiPredicate<Integer,Integer> predicate)
	{
		for(int row = 0; row<rows; row++)
		{
			for(int col = 0; col<cols; col++)
			{
				if(!predicate.test(col,row))
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean anyMatch(BiPredicate<Integer,Integer> predicate)
	{
		for(int row = 0; row<rows; row++)
		{
			for(int col = 0; col<cols; col++)
			{
				if(predicate.test(col,row))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	public float getX(int x)
	{
		return this.x+xStep*x;
	}
	
	public float getY(int y)
	{
		return this.y+yStep*y;
	}
	
	public float getTotalWidth()
	{
		return getX(cols+1);
	}
	public float getTotalHeight()
	{
		return getY(rows+1);
	}
	

	public int getCols()
	{
		return cols;
	}
	
	public int getRows()
	{
		return rows;
	}
	
	@FunctionalInterface
	public static interface QuadConsumer<T1, T2, T3, T4>
	{
		public void accept(T1 a, T2 b, T3 c, T4 d);
	}
	
	public static class Coord
	{
		public final int x, y;
		public Coord(int x, int y)
		{
			this.x=x;
			this.y=y;
		}
		public boolean equals(int x, int y)
		{
			return x==this.x && y==this.y;
		}
		public boolean equals(Object o)
		{
			if(o instanceof Coord)
			{
				Coord c= (Coord) o;
				return x==c.x && y==c.y;
			}
			return false;
		}
		public String toString()
		{
			return String.format("(%d,%d)",x,y);
		}
	}
	
	public static class FloatCoord
	{
		public final float x, y;
		public FloatCoord(float x, float y)
		{
			this.x=x;
			this.y=y;
		}
		public boolean equals(float x, float y)
		{
			return x==this.x && y==this.y;
		}
		public Coord toCoord()
		{
			return new Coord((int)x,(int)y);
		}
	}

	public float getXStep()
	{
		return xStep;
	}
	
	public float getYStep()
	{
		return yStep;
	}
}

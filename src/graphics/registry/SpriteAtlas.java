package graphics.registry;

import graphics.Sprite;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class SpriteAtlas
{
	List<BufferedImage> images = new ArrayList<>();
	List<String> names = new ArrayList<>();
	String namespace = "";
	int pTex;
	List<Sprite> sprites = new ArrayList<>();
	HashMap<String,Sprite> spriteTable =new HashMap<>();
	
	public SpriteAtlas()
	{
		
	}
	
	public SpriteAtlas(File f)
	{
		addAllChildren(f);
	}
	
	public void addAllChildren(File f)
	{
		for(File image:f.listFiles((dir,name)->name.endsWith(".png")))
		{
			addImage(image);
		}
	}
	
	public RegisteredFont addFont(String family, int type, int size)
	{
		return addFont(new Font(family,type,size));
	}
	
	public RegisteredFont addFont(File file,int type, float size)
	{
		try(FileInputStream in = new FileInputStream(file))
		{
			Font f = Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
			return addFont(f);
		}
		catch (IOException | FontFormatException e)
		{
			System.err.println("Font "+file.getPath()+" could not be loaded");
			throw new RuntimeException();
		}
	}
	
	public RegisteredFont addFont(Font f)
	{
		Canvas dummy = new Canvas();
		FontMetrics metrics = dummy.getFontMetrics(f);
		for(int c = 0; c<=256; c++)
		{
			addChar(f,metrics,f.getFamily(),(char)c);
		}
		return new RegisteredFont(f.getFamily(),metrics,this);
	}
	public void addChar(Font f, FontMetrics m, String name, char c)
	{
		if(m.charWidth(c)<=0)
		{
			return;
		}
		BufferedImage temp = new BufferedImage(m.charWidth(c)+4,m.getAscent()+m.getDescent()+4,BufferedImage.TYPE_INT_ARGB);
		Graphics g = temp.getGraphics();
		g.setFont(f);
		g.setColor(Color.black);
		for(int dx = -1; dx<=1; dx++)
		{
			for(int dy = -1; dy<=1; dy++)
			{
				if(dx == 0 && dy ==0)
				{
					continue;
				}
				g.drawString(String.valueOf(c), 2+dx, 2+dy+m.getAscent());
			}
		}
		g.setColor(Color.white);
		g.drawString(String.valueOf(c), 2, 2+m.getAscent());
		addImage(temp,name+"$o/"+c);
		
		temp = new BufferedImage(m.charWidth(c),m.getAscent()+m.getDescent(),BufferedImage.TYPE_INT_ARGB);
		g = temp.getGraphics();
		g.setColor(Color.white);
		g.setFont(f);
		g.drawString(String.valueOf(c), 0, m.getAscent());
		addImage(temp,name+"/"+c);
		
	}
	
	public void addImage(File f)
	{
		try
		{
			addImage(ImageIO.read(f),f.getPath().replace(File.separatorChar, '/'));
		}
		catch(IOException e)
		{
			System.err.println(f.getPath() +" could not be loaded");
		}
	}
	
	public void addImage(BufferedImage i, String name)
	{
		images.add(i);
		names.add(name);
	}
	
	public void build()
	{
		List<Rectangle> rects = new ArrayList<>();
		for(int i = 0; i<images.size(); i++)
		{
			rects.add(new Rectangle(images.get(i),names.get(i)));
		}
		Collections.sort(rects, (o1,o2)->-Integer.compare(o1.height, o2.height));//sort by height, greatest to least
		
		int maxWidth = rects.stream().reduce(0, (i,r)->Math.max(i,r.width), Math::max);
		
		int width = -(Integer.MIN_VALUE/2);//max power of two an integer can represent
		int height = (int)Math.pow(2,Math.ceil(Math.log(rects.get(0).height)/Math.log(2)));
		int bestMaxDim = Integer.MAX_VALUE;
		Packing bestCand = null;
		while(width>maxWidth)
		{
			Packing cand = new Packing(width,height);
			boolean success = true;
			for(Rectangle r:rects)
			{
				success = cand.place(r);
				if(!success)
				{
					break;
				}
			}
			if(success)
			{
				cand.trimToPowerOfTwo();
				width = cand.getWidth();
				if(bestCand==null || Math.max(width, height)<=bestMaxDim)
				{
					bestCand=cand;
					bestMaxDim = Math.max(width, height);
				}
				width/=2;
			}
			else//!success
			{
				height*=2;
			}
			while(Math.max(width, height)>bestMaxDim)
			{
				width/=2;
			}
		}
		width = bestCand.getWidth();
		height = bestCand.getHeight();
		
		ColorModel glColor=new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8,8,8,8}, true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
		WritableRaster raster=Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4, null);
		BufferedImage texImage = new BufferedImage(glColor,raster,true,new Hashtable<>());
		bestCand.render(texImage.getGraphics());
		try
		{
			ImageIO.write(texImage,"PNG",new File("Atlas.png"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
		byte[] data=((DataBufferByte)(texImage.getRaster().getDataBuffer())).getData();
		ByteBuffer imageBuffer=BufferUtils.createByteBuffer(data.length);
		imageBuffer.put(data);
		imageBuffer.flip();
		
		pTex=GL11.glGenTextures();
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, pTex); //I'm sorta assuming this is the only texture atlas.
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,imageBuffer);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		
		for(Rectangle r:bestCand.rects)
		{
			float x = (float)r.x/width;
			float y = (float)r.y/height;
			float swidth = (float)r.width/width;
			float sheight = (float)r.height/height;
			Sprite s = new Sprite(pTex,x,y,swidth,sheight,r.width,r.height,r.im);
			sprites.add(s);
			spriteTable.put(r.name, s);
		}
		
		//System.out.println(spriteTable.keySet().stream().sorted().reduce((a,b)->a+"\n"+b).orElse(""));
	}
	
	public void bind()
	{
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, pTex);
	}
	
	public void setNamespace(String namespace)
	{
		this.namespace = namespace;
	}
	
	public void resetNamespace()
	{
		this.namespace = "";
	}
	
	public Sprite getSprite(String name)
	{
		return spriteTable.get(namespace + name);
	}
	
	public Sprite[] getAnimation(String name, int frames)
	{
		Sprite[] result = new Sprite[frames];
		Sprite parent = getSprite(name);
		if(parent == null)
		{
			return null;
		}
		float x0 = parent.x;
		float y = parent.y;
		float sheight = parent.height;
		float swidth = parent.width/frames;
		int imWidth = parent.imWidth/frames;
		int imHeight = parent.imHeight;
		for(int i = 0; i<frames; i++)
		{
			BufferedImage subImage = new BufferedImage(imWidth,imHeight,BufferedImage.TYPE_INT_ARGB);
			subImage.getGraphics().drawImage(parent.image, -imWidth*i, 0, null);
			result[i] = new Sprite(pTex,x0+i*swidth,y,swidth,sheight,imWidth,imHeight,subImage);
		}
		return result;
	}
	
	public List<Sprite[]> getAnimations(String prefix, int frames)
	{
		List<Sprite[]> result = new ArrayList<>();
		Sprite current[] = null;
		for(
				int i = 0; 
				(current = getAnimation(String.format(prefix, i),frames))!=null; 
				i++
			)//magic for/while loops of magic
		{
			result.add(current);
		}
		return result;
	}
	
	public List<Sprite> getSprites(String prefix)
	{
		List<Sprite> result = new ArrayList<>();
		Sprite current = null;
		for(
				int i = 0; 
				(current = getSprite(String.format(prefix, i)))!=null; 
				i++
			)//magic for/while loops of magic
		{
			result.add(current);
		}
		return result;
	}
	
	public Sprite getSpriteGlobal(String name)
	{
		return spriteTable.get(name);
	}
	
	static class Rectangle
	{
		int x,y,width,height;
		BufferedImage im;
		String name;
		public Rectangle(int x, int y, int width, int height)
		{
			this.x=x;
			this.y=y;
			this.width=width;
			this.height=height;
		}
		
		public Rectangle(BufferedImage im, String name)
		{
			this.x=0;
			this.y=0;
			this.width=im.getWidth();
			this.height=im.getHeight();
			this.im=im;
			this.name=name;
		}
		
		public Rectangle clone()
		{
			Rectangle toReturn = new Rectangle(x,y,width,height);
			toReturn.im=im;
			toReturn.name=name;
			return toReturn;
		}
		
		public void render(Graphics g)
		{
			g.drawImage(im, x, y, null);
		}
	}
	
	static class Packing
	{
		int cols = 1;
		int rows = 1;
		int maxWidth,maxHeight;
		List<Integer> rowHeights = new ArrayList<>();
		List<Integer> colWidths = new ArrayList<>();
		List<Rectangle> rects = new ArrayList<>();
		boolean[][] filled = new boolean[1][1];
		public Packing(int maxWidth, int maxHeight)
		{
			this.maxWidth=maxWidth;
			this.maxHeight=maxHeight;
			colWidths.add(maxWidth);
			rowHeights.add(maxHeight);
		}
		
		public boolean place(Rectangle r)
		{
			int x= 0;
			for(int cellCol = 0; cellCol<cols; cellCol++)
			{
				int y= 0;
				for(int cellRow = 0; cellRow<rows; cellRow++)
				{
					boolean fits = true;
					
					int rowsNeeded = 0;
					int heightLeft = r.height;
					while(heightLeft>0 && fits)
					{
						try
						{
							heightLeft-=rowHeights.get(cellRow+rowsNeeded);
						}
						catch(IndexOutOfBoundsException e)
						{
							fits = false;
						}
						rowsNeeded++;
					}
					
					int colsNeeded = 0;
					int widthLeft = r.width;
					while(widthLeft>0 && fits)
					{
						try
						{
							widthLeft-=colWidths.get(cellCol+colsNeeded);
						}
						catch(IndexOutOfBoundsException e)
						{
							fits = false;
						}
						colsNeeded++;
					}
					
					if(!fits)
					{
						continue;
					}
					
					for(int row = 0; row<rowsNeeded; row++)
					{
						for(int col =0; col<colsNeeded; col++)
						{
							if(filled[cellRow+row][cellCol+col])
							{
								fits = false;
							}
						}
					}
					
					if(fits)
					{
						heightLeft = r.height;
						for(int row = cellRow; row<cellRow+rowsNeeded; row++)
						{
							if(heightLeft<rowHeights.get(row))
							{
								splitRow(row,heightLeft);
							}
							heightLeft-=rowHeights.get(row);
							widthLeft = r.width;
							for(int col = cellCol; col<cellCol+colsNeeded; col++)
							{
								if(widthLeft<colWidths.get(col))
								{
									splitCol(col,widthLeft);
								}
								widthLeft -= colWidths.get(col);
								filled[row][col]=true;
							}
						}
						Rectangle newRect = r.clone();
						newRect.x=x;
						newRect.y=y;
						rects.add(newRect);
						return true;
					}
					y+=rowHeights.get(cellRow);
				}
				x+=colWidths.get(cellCol);
			}
			return false;
		}
		
		public void render(Graphics g)
		{
			for(Rectangle r: rects)
			{
				r.render(g);
			}
		}
		
		public void trimCols()
		{
			for(int col = cols-1; col>=0; col--)
			{
				for(int row=0; row<rows; row++)
				{
					if(filled[row][col])
					{
						return;
					}
				}
				cols--;
				colWidths.remove(col);
			}
		}
		
		public void trimToPowerOfTwo()
		{
			int width = getWidth();
			int canRemove = 0;
			countRemovableSpace:
			{
				for(int col = cols-1; col>=0; col--)
				{
					for(int row=0; row<rows; row++)
					{
						if(filled[row][col])
						{
							break countRemovableSpace;
						}
					}
					canRemove += colWidths.get(col);
				}
			}
			for(int i = 1; i<=width; i*=2)
			{
				if(i >= (width-canRemove) && i<width)
				{
					int needToRemove = width-i;
					int col = cols-1;
					while(needToRemove>0)
					{
						if(needToRemove>= colWidths.get(col))
						{
							cols--;
							needToRemove-=colWidths.remove(col);
						}
						else
						{
							colWidths.set(col, colWidths.get(col)-needToRemove);
							needToRemove = 0;
						}
						col--;
					}
					return;
				}
			}
		}
		
		public int getWidth()
		{
			int width=0;
			for(int w:colWidths)
			{
				width+=w;
			}
			return width;
		}
		
		public int getHeight()
		{
			return maxHeight;
		}
		
		public void splitRow(int row, int height)
		{
			if(height == 0 || height == rowHeights.get(row))
			{
				return;
			}
			int oldHeight = rowHeights.get(row);
			rowHeights.set(row, height);
			rowHeights.add(row+1, oldHeight-height);
			
			boolean[][] newFilled = new boolean[rows+1][cols];
			for(int inRow = 0,outRow=0; inRow<rows; inRow++,outRow++)
			{
				for(int col = 0; col<cols; col++)
				{
					newFilled[outRow][col] = filled[inRow][col];
				}
				if(inRow==row)
				{
					outRow++;
					for(int col = 0; col<cols; col++)
					{
						newFilled[outRow][col] = filled[inRow][col];
					}
				}
			}
			filled = newFilled;
			rows++;
		}
		public void splitCol(int col, int width)
		{
			if(width == 0 || width == colWidths.get(col))
			{
				return;
			}
			int oldWidth = colWidths.get(col);
			colWidths.set(col, width);
			colWidths.add(col+1, oldWidth-width);
			
			boolean[][] newFilled = new boolean[rows][cols+1];
			for(int inCol = 0,outCol=0; inCol<cols; inCol++,outCol++)
			{
				for(int row = 0; row<rows; row++)
				{
					newFilled[row][outCol] = filled[row][inCol];
				}
				if(inCol==col)
				{
					outCol++;
					for(int row = 0; row<rows; row++)
					{
						newFilled[row][outCol] = filled[row][inCol];
					}
				}
			}
			filled = newFilled;
			cols++;
		}
		
		public String toString()
		{
			StringBuilder out = new StringBuilder();
			
			out.append('\t');
			for(int width:colWidths)
			{
				out.append(width);
				out.append('\t');
			}
			out.append('\n');
			int row = 0;
			for(int height:rowHeights)
			{
				out.append(height);
				out.append('\t');
				for(int col=0; col<cols; col++)
				{
					if(filled[row][col])
					{
						out.append('#');
					}
					else
					{
						out.append(' ');
					}
					out.append('\t');
				}
				row++;
				out.append('\n');
			}
			
			return out.toString();
		}
	}
}

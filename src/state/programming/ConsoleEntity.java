package state.programming;

import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;

import math.Matrix;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import computer.ExecutableHolder;
import computer.InteractiveExecutable;
import computer.RenderedExecutable;
import state.ui.ClickableArea;
import util.Color;
import graphics.Context;
import graphics.Sprite;
import graphics.entity.Entity;
import graphics.registry.RegisteredFont;
import graphics.registry.UtilSprites;

public class ConsoleEntity extends Entity
{
	AppendOnlyBuffer appendOnly;
	StringBuffer editable = new StringBuffer();
	RegisteredFont font;
	List<String> commandBuffer = new ArrayList<>();
	int commandIndex = 0;
	
	String prompt = "command@fake-pc$>";
	int cursor;
	int commandStart;
	
	float width,height;
	int charWidth, charHeight, rows, cols;
	
	int startLine = 0;
	int blinkTime = 0;
	int blinkPeriod = 500;
	
	boolean insert = true;
	Entity bg;
	
	boolean runningInteractiveProgram = false;
	boolean runningRenderedProgram = false;
	InteractiveExecutable interactiveProgram;
	RenderedExecutable renderedProgram;
	CommandParser parser;
	
	public ConsoleEntity(float x, float y, float z, float width, float height)
	{
		super(x, y, z, null);
		
		this.addClickableArea(new ClickableArea(x, y, width, height)
		{
			public void onLeftClick(float x, float y)
			{
				if(runningRenderedProgram)
				{
					renderedProgram.mouseClicked
					(
							(int)(x/charWidth),
							(int)(y/charHeight)-appendOnly.height()+startLine+1
					);
				}
			}
			public void onMouseMove(float x, float y)
			{
				if(runningRenderedProgram)
				{
					renderedProgram.mouseMoved
					(
							(int)(x/charWidth),
							(int)(y/charHeight)-appendOnly.height()+startLine+1
					);
				}
			}
			public void onRelease()
			{
				if(runningRenderedProgram)
				{
					renderedProgram.mouseReleased();
				}
			}
		});
		
		font = RegisteredFont.defaultFont;
		this.width = width;
		this.height = height;
		FontMetrics metrics = font.metrics;
		charWidth = metrics.charWidth(' ');
		charHeight = metrics.getHeight();
		rows = (int) (height/charHeight);
		cols = (int) (width/charWidth);
		
		bg = new Entity(-charWidth,-charHeight,0,UtilSprites.white);
		bg.setScale((cols+2)*charWidth, (rows+2)*charHeight);
		bg.setColor(Color.black);
		
		appendOnly = new AppendOnlyBuffer(cols);
		parser = new CommandParser(appendOnly);
		
		appendOnly.append("Login fake@fake-pc\n\n");
		newPrompt();
	}
	public void act(int dt)
	{
		blinkTime = (blinkTime+dt)%blinkPeriod;
		if(runningInteractiveProgram)
		{
			if(interactiveProgram.isRunning())
			{
				interactiveProgram.act(dt);
			}
			else
			{
				runningInteractiveProgram = false;
				interactiveProgram = null;
				newPrompt();
			}
		}
		else if(runningRenderedProgram)
		{
			if(renderedProgram.isRunning())
			{
				renderedProgram.act(dt);
			}
			else
			{
				runningRenderedProgram = false;
				renderedProgram = null;
				newPrompt();
			}
		}
		super.act(dt);
	}
	
	public String getPrompt()
	{
		if(runningInteractiveProgram)
		{
			return interactiveProgram.getPrompt();
		}
		return prompt;
	}
	
	public char getCursorChar()
	{
		if(insert)
		{
			return '_';
		}
		else
		{
			return 22;
		}
	}
	private void renderCursor(int x, int y, Context c)
	{
		if(blinkTime<blinkPeriod/2)
		{
			c.pushTransform();
			c.appendTransform(Matrix.translation(x,y,0));
			font.getSprite(getCursorChar()).render(c);
			c.popTransform();
		}
	}
	
	public void renderBase(Context c)
	{
		bg.render(c);
		//render the static section
		renderStaticSection(c);
		//render the mutable section
		if(runningRenderedProgram)
		{
			renderedProgram.render(c, -startLine+appendOnly.height()-1);
		}
		else
		{
			renderMutableSection(c);
		}
	}
	
	public void renderStaticSection(Context c)
	{
		int row = 0;
		int x = 0, y = 0;
		if(startLine<0)
		{
			y = -startLine*charHeight;
			row = -startLine;
		}
		else
		{
			y = 0;
			row = 0;
		}
		for(int i = appendOnly.getLineStart(startLine); i<appendOnly.length() && row<rows; i++)
		{
			char character = appendOnly.getCharAt(i);
			if(character!='\n')
			{
				Sprite s = font.getSprite(character);
				if(s!=null)
				{
					c.pushTransform();
					c.appendTransform(Matrix.translation(x,y,0));
					s.render(c);
					c.popTransform();
				}
				x+=charWidth;
			}
			else
			{
				y += charHeight;
				row++;
				x = 0;
			}
		}
	}
	public void renderMutableSection(Context c)
	{
		int row = -startLine+appendOnly.height()-1;
		int x=0;
		int y = row*charHeight;
		
		int col = 0;
		for(int i = 0; i<editable.length() && row<rows; i++)
		{
			char character = editable.charAt(i);
			if(character!='\n')
			{
				if(row>=0)
				{
					Sprite s = font.getSprite(character);
					if(i==cursor)
					{
						renderCursor(x,y,c);
					}
					if(s!=null)
					{
						GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT);
						
						GL14.glBlendFuncSeparate(GL11.GL_ONE,GL11.GL_DST_COLOR,GL11.GL_ZERO,GL11.GL_ZERO);
						GL20.glBlendEquationSeparate(GL14.GL_FUNC_SUBTRACT,GL14.GL_FUNC_ADD);
						c.pushTransform();
						c.appendTransform(Matrix.translation(x,y,0));
						s.render(c);
						c.popTransform();
						GL11.glPopAttrib();
					}
				}
				x+=charWidth;
				col++;
				if(col >= cols)
				{
					y+= charHeight;
					row++;
					x = 0;
					col = 0;
				}
			}
			else
			{
				y += charHeight;
				row++;
				x = 0;
				col = 0;
			}
		}
		if(cursor==editable.length() && row<rows && row>=0)
		{
			renderCursor(x,y,c);
		}
	}
	
	public float getUnscaledWidth()
	{
		return width;
	}
	public float getUnscaledHeight()
	{
		return height;
	}
	public int bufferHeight()
	{
		return appendOnly.height()+commandHeight();
	}
	public int commandHeight()
	{
		return (int) (editable.length()/cols)+1;
	}
	public String getCommand()
	{
		return editable.substring(commandStart);
	}
	public void setCommand(String command)
	{
		editable.delete(commandStart, editable.length());
		editable.append(command);
		cursor = editable.length();
		updateScroll();
	}
	
	private void updateScroll()
	{
		if(bufferHeight()-startLine>rows)
		{
			startLine = bufferHeight()-rows;
		}
	}
	public void makeVisible(int line)
	{
		line += appendOnly.height();
		if(line-1 < startLine)
		{
			startLine = line-1;
		}
		if(line > startLine+rows)
		{
			startLine = line - rows;
		}
	}
	
	private void deletePrompt()
	{
		editable.delete(0, editable.length());
		cursor = 0;
		commandStart = 0;
	}
	
	private void archivePrompt()
	{
		appendOnly.append(editable.toString());
		appendOnly.append('\n');
		savePrompt();
		deletePrompt();
	}
	
	private void savePrompt()
	{
		commandBuffer.add(0,getCommand());
		commandIndex = 0;
	}
	
	private void newPrompt()
	{
		deletePrompt();
		editable.append(getPrompt());
		cursor = editable.length();
		commandStart = cursor;
		updateScroll();
	}

	private void enterCommand()
	{
		if(runningInteractiveProgram)
		{
			savePrompt();
			interactiveProgram.acceptCommand(getCommand());
			newPrompt();
		}
		else if(runningRenderedProgram)
		{
			//this should never happen?
		}
		else
		{
			String command = getCommand();
			archivePrompt();
			String[] tokens = parser.parse(command);
			ExecutableHolder holder = parser.execute(tokens);
			if(holder == null)
			{
				appendOnly.append("System cannot find command ");
				appendOnly.appendln(command);
				appendOnly.appendln();
				newPrompt();
			}
			else if(holder.isInteractive())
			{
				runningInteractiveProgram = true;
				interactiveProgram = holder.getInteractiveExec();
				newPrompt();
			}
			else if(holder.isRendered())
			{
				runningRenderedProgram = true;
				renderedProgram = holder.getRenderedExec();
			}
			if(holder!=null)
			{
				holder.init(tokens, appendOnly, editable, font, cols, rows, this::makeVisible);
				if(!holder.isInteractive() && !holder.isRendered())
				{
					newPrompt();
				}
			}
		}
		
	}
	
	public void insertCharAtCursor(char c)
	{
		if(c=='\r' || c =='\n')
		{
			enterCommand();
		}
		else
		{
			editable.insert(cursor, c);
			cursor++;
			updateScroll();
		}
	}
	public void overwriteCharAtCursor(char c)
	{
		if(c=='\r' || c =='\n')
		{
			enterCommand();
		}
		else if(cursor == editable.length())
		{
			editable.insert(cursor, c);
			cursor++;
			updateScroll();
		}
		else
		{
			editable.setCharAt(cursor, c);
			cursor++;
			updateScroll();
		}
	}
	
	public void paste(String s)
	{
		s=s.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
		if(runningRenderedProgram)
		{
			renderedProgram.acceptPaste(s);
		}
		else
		{
			for(int i = 0; i<s.length(); i++)
			{
				char c = s.charAt(i);
				insertCharAtCursor(c);
			}
		}
	}
	public void backspaceAtCursor()
	{
		if(cursor>commandStart)
		{
			editable.deleteCharAt(cursor-1);
			cursor--;
		}
	}
	public void deleteAtCursor()
	{
		if(cursor<editable.length())
		{
			editable.deleteCharAt(cursor);
		}
	}

	public void keyPressed(int key, int modFlags)
	{
		if(key == GLFW.GLFW_KEY_PAGE_UP)
		{
			startLine--;
		}
		else if(key == GLFW.GLFW_KEY_PAGE_DOWN)
		{
			startLine++;
		}
		
		if(runningRenderedProgram)
		{
			if(key == GLFW.GLFW_KEY_END)
			{
				appendOnly.append(editable.toString());
				appendOnly.append('\n');
				renderedProgram.stop();
				runningRenderedProgram = false;
				renderedProgram = null;
				newPrompt();
			}
			else
			{
				renderedProgram.keyPressed(key, modFlags);
			}
			return;
		}
		
		if(key == GLFW.GLFW_KEY_ENTER)
		{
			enterCommand();
		}
		else if(key == GLFW.GLFW_KEY_LEFT)
		{
			if(cursor>commandStart)
			{
				cursor--;
			}
		}
		else if(key == GLFW.GLFW_KEY_RIGHT)
		{
			if(cursor<editable.length())
			{
				cursor++;
			}
		}
		else if(key == GLFW.GLFW_KEY_BACKSPACE)
		{
			backspaceAtCursor();
		}
		else if(key == GLFW.GLFW_KEY_DELETE)
		{
			deleteAtCursor();
		}
		else if(key == GLFW.GLFW_KEY_INSERT)
		{
			insert = !insert;
		}
		else if(key == GLFW.GLFW_KEY_UP)
		{
			if(commandIndex>=0 && commandIndex<commandBuffer.size())
			{
				setCommand(commandBuffer.get(commandIndex));
				commandIndex++;
			}
		}
		else if(key == GLFW.GLFW_KEY_DOWN)
		{
			if(commandIndex-2>=0 && commandIndex-2<commandBuffer.size())
			{
				setCommand(commandBuffer.get(commandIndex-2));
				commandIndex--;
			}
		}
		else if(key == GLFW.GLFW_KEY_END)
		{
			if(runningInteractiveProgram)
			{
				interactiveProgram.stop();
				runningInteractiveProgram = false;
				interactiveProgram = null;
				newPrompt();
			}
		}
	}

	public void charTyped(char c)
	{
		if(runningRenderedProgram)
		{
			renderedProgram.charTyped(c);
			return;
		}
		if(insert)
		{
			insertCharAtCursor(c);
		}
		else
		{
			overwriteCharAtCursor(c);
		}
	}
}
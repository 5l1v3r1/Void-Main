package state.programming;

import math.Matrix;

import org.lwjgl.glfw.GLFW;

import computer.system.Computer;
import entry.GlobalInput;
import entry.GlobalState;
import graphics.Context;
import graphics.registry.SpriteAtlas;
import state.GameState;
import state.workbench.Camera;
import static state.programming.Modifiers.*;

public class ProgrammingState extends GameState
{
	ConsoleEntity console;
	Camera camera;
	boolean mouseMoveThisFrame = false;
	long ibeamCursor;
	long defaultCursor;
	Computer computer;
	
	public ProgrammingState(GlobalInput input, long window)
	{
		super(input, window);
		this.computer = GlobalState.laptop;
	}
	
	public void init(SpriteAtlas sprites)
	{
		camera = new Camera(screenWidth()/2,screenHeight()/2,screenWidth(),screenHeight(),1);
		console = new ConsoleEntity(100,100,0,1800,800,computer);
		addUI(console);
		ibeamCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_IBEAM_CURSOR);
		defaultCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
	}
	
	public int getModFlags()
	{
		return (isShiftDown()?SHIFT_FLAG:0) | 
			   (isControlDown()?CONTROL_FLAG:0) | 
			   (isAltDown()?ALT_FLAG:0);
	}
	
	public void keyPressed(int key)
	{
		if(key == GLFW.GLFW_KEY_ESCAPE)
		{
			//TODO
			changeTo(GlobalState.currentWorkbench);
		}
		console.keyPressed(key, getModFlags());
		if(isControlDown() && key == GLFW.GLFW_KEY_V)
		{
			console.paste(getClipboardString().replaceAll("\r\n", "\n"));
		}
	}
	
	public void charTyped(char c)
	{
		console.charTyped(c);
	}
	public void mousePressed(int button)
	{
		if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			console.handleClick(getMouseX(), getMouseY(), button);
		}
	}
	public void mouseMoved(float x, float y)
	{
		console.handleMove(x, y);
		mouseMoveThisFrame = true;
	}
	public void afterInput(int dt)
	{
		if(!mouseMoveThisFrame)
		{
			console.handleMove(getMouseX(), getMouseY());
		}
		mouseMoveThisFrame = false;
		float x = getMouseX();
		float y = getMouseY();
		if(x>=console.getX() && x<console.getX()+console.getWidth() && y>=console.getY() && y<console.getY()+console.getHeight())
		{
			setCursor(ibeamCursor);
		}
		else
		{
			setCursor(defaultCursor);
		}
	}
	public void mouseReleased(int button)
	{
		if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			console.handleRelease();
		}
	}
	public void scrollMoved(float dx, float dy)
	{
		console.scroll((int)Math.round(dy),getModFlags());
	}
	
	
	boolean isShiftDown()
	{
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_SHIFT)||isKeyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}
	boolean isControlDown()
	{
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_CONTROL);
	}
	boolean isAltDown()
	{
		return isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_ALT);
	}
	
	public void keyRepeated(int key)
	{
		keyPressed(key);
	}

	public void renderAll(Context context)
	{
		context.setView(camera.getView());
		context.setModel(Matrix.identity(4));
		context.resetColor();
		renderUI(context);
	}
	public void cleanup()
	{
		setCursor(defaultCursor);
	}
}

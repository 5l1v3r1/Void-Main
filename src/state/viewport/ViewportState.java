package state.viewport;

import javax.sound.sampled.Clip;

import org.lwjgl.glfw.GLFW;

import math.Matrix;
import entry.GlobalInput;
import entry.GlobalState;
import game.map.Map;
import game.map.Unit;
import game.map.UnitController;
import game.map.UnitTypes;
import game.session.levelgen.MapTypes;
import game.session.levelgen.roomgen.RoomGen;
import graphics.Context;
import graphics.entity.particles.Particle;
import graphics.registry.SpriteAtlas;
import graphics.registry.UtilSprites;
import state.GameState;
import state.workbench.Camera;
import state.workbench.game.ExportState;
import util.Color;

public class ViewportState extends GameState
{
	Camera camera = new Camera(screenWidth()/2,screenHeight()/2,screenWidth(),screenHeight(),1);
	
	Unit robot;
	ExportState robotConfig;
	UnitController robotController;
	LightSource robotLight;
	
	MapEntity me;
	public Map map;
	float scale = 4f;
	Clip bgMusic;
	LightSystem lightSystem = new LightSystem(.1f);
	
	public ViewportState(GlobalInput input, long window)
	{
		super(input, window);
	}
	
	public void setRobot(ExportState robotConfig)
	{
		if(map == null)
		{
			genMap();
		}
		if(robot!=null)
		{
			robot.setTile(null);
			lightSystem.removeLight(robotLight);
		}
		
		this.robotConfig = robotConfig;
		robot = new Unit(UnitTypes.robot.get());
		robotController = new UnitController(robotConfig,robot,map);
		map.setUnitAtStart(robot);
		me.setCenter(robot, 5);
		robotLight = new LightSource(robot,5);
		lightSystem.addLight(robotLight);
	}
	
	public void setMap(Map map)
	{
		robot = null;
		robotConfig = null;
		robotController = null;
		robotLight = null;
		lightSystem = new LightSystem(.1f);
		
		if(me != null)
		{
			remove(me);
		}
		
		me = RoomGen.rebuild(map, scale, camera, lightSystem);
		add(me);
		this.map = map;
		
		robot = map.getUnit(UnitTypes.ROBOT);
		if(robot!=null)
		{
			robotController = robot.getController();
			robotConfig = robotController.getConfig();
			me.setCenter(robot, 5);
			robotLight = new LightSource(robot,5);
			lightSystem.addLight(robotLight);
		}
	}
	
	public void genMap()
	{
		robot = null;
		robotConfig = null;
		robotController = null;
		robotLight = null;
		lightSystem = new LightSystem(.1f);
		
		if(me != null)
		{
			remove(me);
		}
		
		RoomGen generator = new RoomGen(6,100,MapTypes.surface);
		map = generator.getMap();
		
		generator.populate(()->new Unit(UnitTypes.mushrooms.get()), .1);
		generator.populate(()->new Unit(UnitTypes.spores.get()), .2);
		
		me = generator.build(scale, camera, lightSystem);
		add(me);
	}
	
	public void keyPressed(int key)
	{
		if(key == GLFW.GLFW_KEY_ESCAPE)
		{
			systemExit();
		}
		
		if(key == GLFW.GLFW_KEY_S && isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL))
		{
			GlobalState.save("session.jso");
		}
		
		if(key == GLFW.GLFW_KEY_R && isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL))
		{
			if(robot!=null)
			{
				robot.setTile(null);
				lightSystem.removeLight(robotLight);
			}
			ExportState config = robotConfig;
			robot = null;
			robotConfig = null;
			robotController = null;
			robotLight = null;
			
			GlobalState.currentWorkbench.load(config);
			this.changeTo(GlobalState.currentWorkbench);
		}
	}
	
	public void afterInput(int dt)
	{
		if(isKeyPressed(GLFW.GLFW_KEY_UP))
		{
			GlobalState.radio.broadcast(0,GlobalState.laptop);
		}
		else
		{
			GlobalState.radio.stopBroadcast(0,GlobalState.laptop);
		}
		
		if(isKeyPressed(GLFW.GLFW_KEY_DOWN))
		{
			GlobalState.radio.broadcast(1,GlobalState.laptop);
		}
		else
		{
			GlobalState.radio.stopBroadcast(1,GlobalState.laptop);
		}
		
		if(isKeyPressed(GLFW.GLFW_KEY_LEFT))
		{
			GlobalState.radio.broadcast(2,GlobalState.laptop);
		}
		else
		{
			GlobalState.radio.stopBroadcast(2,GlobalState.laptop);
		}
		
		if(isKeyPressed(GLFW.GLFW_KEY_RIGHT))
		{
			GlobalState.radio.broadcast(3,GlobalState.laptop);
		}
		else
		{
			GlobalState.radio.stopBroadcast(3,GlobalState.laptop);
		}
		
		if(robot!= null && robot.isMoving())
		{
			float x=0,y=0,width=0,height=0,dx=0,dy=0;
			switch(robot.getDirection())
			{
			case Unit.NORTH:
			case Unit.SOUTH:
				x = 1;
				y = 4;
				width = 2;
				height = 12;
				dx = 11;
				break;
			case Unit.WEST:
				x = 4;
				y = 15;
				width = 9;
				height = 1;
				dy = -7;
				break;
			case Unit.EAST:
				x = 2;
				y = 15;
				width = 9;
				height = 1;
				dy = -7;
				break;
			}
			Particle toAdd = new Particle(
					(float) (Math.random()*width)+x-.5f+robot.getxOffset(), 
					(float) (Math.random()*height)+y-.5f+robot.getyOffset(), 
					0, 
					UtilSprites.white, 500, robot.getTile().getParticles())
			{

				public void update(int dt, int lifeTime, float x, float y,float z)
				{
					this.setGroupAlpha((float) (this.getGroupAlpha()*Math.pow(.994, dt)));
				}
			};
			Particle toAdd2 = new Particle(
					(float) (Math.random()*width)+x-.5f+robot.getxOffset()+dx, 
					(float) (Math.random()*height)+y-.5f+robot.getyOffset()+dy, 
					0, 
					UtilSprites.white, 500, robot.getTile().getParticles())
			{

				public void update(int dt, int lifeTime, float x, float y,float z)
				{
					this.setGroupAlpha((float) (this.getGroupAlpha()*Math.pow(.994, dt)));
				}
			};
			
			toAdd.setColor(new Color(0f,0f,0f,.8f));
			toAdd2.setColor(new Color(0f,0f,0f,.8f));
			
			robot.getTile().getParticles().addParticle(toAdd);
			robot.getTile().getParticles().addParticle(toAdd2);
		}
		if(robot!=null && robot.getGoal() != null && robot.getGoal().getUnit()!=null)
		{
			robot.getGoal().destroyUnit(0, 0);
		}
	}
	
	public void afterUpdate(int dt)
	{
		if(robot!=null)
		{
			camera.x = ((robot.getX()+.5f)*16+robot.getxOffset())*scale;
			camera.y = ((robot.getY()+.5f)*16+robot.getyOffset())*scale;
		}
		lightSystem.act(dt);
	}
	
	public void disable()
	{
		if(bgMusic!=null)
		{
			bgMusic.stop();
		}
	}
	public void enable()
	{
		
	}
	
	public void init(SpriteAtlas sprites)
	{
		try
		{
//			File file = new File("res/mus/Overworld_Theme.wav");
//			AudioInputStream sound = AudioSystem.getAudioInputStream(file);
//			bgMusic = AudioSystem.getClip();
//			bgMusic.open(sound);
//			bgMusic.setFramePosition(0);
//			bgMusic.loop(Clip.LOOP_CONTINUOUSLY);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public void renderAll(Context context)
	{
		context.resetColor();
		context.setView(camera.getView());
		context.setModel(Matrix.identity(4));
		render(context);
		context.setView(Matrix.identity(4));
		renderUI(context);
	}

	public Map getMap()
	{
		return map;
	}

}

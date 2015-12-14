package state.workbench.graphics;

import math.Matrix;

import org.lwjgl.glfw.GLFW;

import state.ui.ClickableArea;
import state.workbench.game.WiringMode;
import util.Color;
import game.item.Pin;
import graphics.Context;
import graphics.Sprite;
import graphics.entity.AnimatedEntity;
import graphics.entity.Entity;
import graphics.entity.TextEntity;

public class PinHighlight extends AnimatedEntity
{
	float htz = .5f;
	float center=.05f;
	float amplitude=.05f;
	float wireAlpha=1;
	float previewAlpha=.4f;
	int time;
	ClickableArea area;
	Entity preview;
	Entity previewFade;
	Entity mask;
	Entity wire;
	Entity wireFade;
	Entity tooltipEntity;
	String tooltip = "";
	Pin pin;
	WiringMode mode;
	
	public PinHighlight(float x, float y, Sprite highlight, Sprite wireEnd, Sprite wireFade, Sprite pinMask, WiringMode mode, Pin pin)
	{
		this(x,y,highlight,wireEnd,wireFade,pinMask,pin,mode,true);
	}
	
	public PinHighlight(float x, float y, Sprite highlight, Sprite wireEnd, Sprite wireFade, Sprite pinMask, Pin pin, WiringMode mode, boolean interactable)
	{
		super(x,y,y,highlight);
		this.pin = pin;
		this.mode = mode;
		setColor(new Color(1,1,1,center));
		if(interactable)
		{
			pin.highlight = this;
			area = new ClickableArea(0,0,3,3)
			{
				public void onClick(float x, float y, int button)
				{
					if(pin.getAttatched()==null)
					{
						mode.bind(pin);
					}
					else
					{
						if(button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
						{
							mode.unbind(pin);
						}
						else if(button == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
						{
							mode.unbind(pin.getAttatched());
						}
					}
				}
			};
			addClickableArea(area);
			{
				preview = new Entity(0,-wireEnd.imHeight+6,0,wireEnd);
				preview.setColor(new Color(1,1,1,previewAlpha));
				previewFade = new WireFadeEntity(1,2-wireFade.imHeight,0,wireFade);
				preview.addChild(previewFade);
				addChild(preview);
			}
			
			{
				wire = new Entity(0,-wireEnd.imHeight+6,0,wireEnd);
				wire.setColor(new Color(1,1,1,wireAlpha));
				this.wireFade = new WireFadeEntity(1,2-wireFade.imHeight,0,wireFade);
				wire.addChild(this.wireFade);
				addChild(wire);
				
				if(pin != null && pin.getAttatched() != null)
				{
					this.wireFade.setColor(pin.getAttatched().getColor());
				}
				else
				{
					wire.setVisible(false);
				}
			}
			mask = new Entity(0,0,1,pinMask);
			addChild(mask);
			mask.setVisible(false);
			preview.setVisible(false);
		}
	}

	public void setTooltip(String tooltip)
	{
		this.tooltip = tooltip;
		//TIME FOR A DIRTY HACK!!!!
		if(pin.getParent().getType().getWorkbenchHeight()==1)
		{
			this.tooltipEntity = new TextEntity(2.5f,3,100,tooltip);
		}
		else
		{
			this.tooltipEntity = new TextEntity(3,3,100,tooltip);
		}
		//THAT'S BAD
		tooltipEntity.setRotation(Matrix.rotationd(90));
		addChild(tooltipEntity);
	}
	
	public void animate(int dt)
	{
		time+=dt;
		
		if(pin != null && pin.getAttatched() != null)
		{
			wire.setVisible(true);
			mask.setVisible(true);
			preview.setVisible(false);
			wireFade.setColor(pin.getAttatched().getColor());
			if(!pin.getAttatched().isAttatchedOnBothSides())
			{
				wire.setColor(new Color(1,1,1,.6f));
			}
			else if(area.ownsMouse() || pin.getAttatched().isAttatchedOnBothSides() && pin.getAttatched().getOtherEnd(pin).highlight.area.ownsMouse())
			{
				wire.setColor(new Color(2,2,2,1));
			}
			else
			{
				wire.setColor(Color.white);
			}
			return;
		}
		else if(wire!=null)
		{
			wire.setVisible(false);
			mask.setVisible(false);
		}
		
		if(area!=null && area.ownsMouse())
		{
			preview.setVisible(true);
			previewFade.setColor(mode.getCurrent().getColor());
			mask.setVisible(true);
		}
		else
		{
			if(preview!=null) 
			{
				preview.setVisible(false);
				mask.setVisible(false);
			}
			float col = (float) (Math.sin(time*htz*Math.PI*2/1000));
			col = col*amplitude+center;
			setColor(new Color(1,1,1,col));
		}
	}
	
	
	public static class WireFadeEntity extends Entity
	{

		public WireFadeEntity(float x, float y, float z, Sprite base)
		{
			super(x, y, z, base);
		}
		
		public void renderBase(Context c)
		{
			c.setFlag(0, true);
			super.renderBase(c);
			c.setFlag(0, false);
		}
		
	}
}

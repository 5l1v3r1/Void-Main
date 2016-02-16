package computer.system;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;

import program.Environment;
import util.FileLoader;

public class Computer implements Serializable
{
	private static final long serialVersionUID = 8175286991987632824L;
	
	private String name;
	String rootPath;
	Environment connected = null;
	
	public Computer(String name)
	{
		init(name);
	}
	private void init(String name)
	{
		this.name = name;
		File root = new File(name);
		if(!root.exists())
		{
			root.mkdir();
		}
		else if(!root.isDirectory())
		{
			init(name+"$");
			return;
		}
		rootPath = root.getAbsolutePath().replace(File.separatorChar, '/');
	}
	private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException
	{
		in.defaultReadObject();
		init(name);
		System.out.println(rootPath);
	}
	public void upload(String source)
	{
		if(connected!=null)
		{
			connected.uplode(source);
		}
	}
	public void touch(String filePath)
	{
		filePath = filePath.trim();
		if(filePath.equals("/") || filePath.equals(""))
		{
			return;
		}
		if(filePath.startsWith("/"))
		{
			filePath = filePath.substring(1);
		}
		File file = new File(rootPath+"/"+filePath);
		if(!file.getAbsolutePath().startsWith(rootPath))
		{
			return;
		}
		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	public boolean exists(String filePath)
	{
		filePath = filePath.trim();
		if(filePath.equals("/") || filePath.equals(""))
		{
			return false;
		}
		if(filePath.startsWith("/"))
		{
			filePath = filePath.substring(1);
		}
		File file = new File(rootPath+"/"+filePath);
		if(!file.getAbsolutePath().replace(File.separatorChar, '/').startsWith(rootPath))
		{
			return false;
		}
		return file.exists();
	}
	public void write(String filePath, String contents)
	{
		filePath = filePath.trim();
		if(filePath.equals("/") || filePath.equals(""))
		{
			return;
		}
		if(filePath.startsWith("/"))
		{
			filePath = filePath.substring(1);
		}
		File file = new File(rootPath+"/"+filePath);
		if(!file.getAbsolutePath().replace(File.separatorChar, '/').startsWith(rootPath))
		{
			return;
		}
		try(BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
		{
			out.write(contents);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public String read(String filePath)
	{
		filePath = filePath.trim();
		if(filePath.equals("/") || filePath.equals(""))
		{
			return "";
		}
		if(filePath.startsWith("/"))
		{
			filePath = filePath.substring(1);
		}
		try
		{
			return FileLoader.getFileContents(rootPath+"/"+filePath);
		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			return "";
		}
	}
	public String getName()
	{
		return name;
	}
	public String getRootPath()
	{
		return rootPath;
	}
	public boolean hasConnection()
	{
		return connected != null;
	}
	public void setConnected(Environment environment)
	{
		this.connected = environment;
	}
	public Environment getConnected()
	{
		return this.connected;
	}
}

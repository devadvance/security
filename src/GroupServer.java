/* Group server. Server loads the users from UserList.bin.
 * If user list does not exists, it creates a new list and makes the user the server administrator.
 * On exit, the server saves the user list to file. 
 */

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.*;


public class GroupServer extends Server {

	public static final int SERVER_PORT = 8765;
	public UserList userList;
	public GroupList groupList;
    
	public GroupServer() {
		super(SERVER_PORT, "ALPHA");
	}
	
	public GroupServer(int _port) {
		super(_port, "ALPHA");
	}
	
	public void start() {
		// Overwrote server.start() because if no user file exists, initial admin account needs to be created
		
		String userFile = "UserList.bin";
		String groupFile = "GroupList.bin";
		Scanner console = new Scanner(System.in);
		ObjectInputStream userStream;
		ObjectInputStream groupStream;
		
		//This runs a thread that saves the lists on program exit
		Runtime runtime = Runtime.getRuntime();
		runtime.addShutdownHook(new ShutDownListener(this));
		
		//Open user file to get user list
		try
		{
			FileInputStream fis = new FileInputStream(userFile);
			userStream = new ObjectInputStream(fis);
			userList = (UserList)userStream.readObject();
		}
		catch(FileNotFoundException e)
		{
			System.out.println("UserList File Does Not Exist. Creating UserList...");
			System.out.println("No users currently exist. Your account will be the administrator.");
			System.out.print("Enter your username: ");
			String username = console.next();
			
			//Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
			userList = new UserList();
			userList.addUser(username);
			userList.addGroup(username, "ADMIN");
			userList.addOwnership(username, "ADMIN");
			

			groupList = new GroupList();
			groupList.addGroup("ADMIN");
			groupList.addOwner("ADMIN", username);
			groupList.addMember("ADMIN", username);

			ObjectOutputStream outStream;
			try
			{
				outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
				outStream.writeObject(groupList);
				outStream.close();
			}
			catch(Exception e1)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}


		}
		catch(IOException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Error reading from UserList file");
			System.exit(-1);
		}
				//Open group file to get group list
		try
		{
			FileInputStream fis = new FileInputStream(groupFile);
			groupStream = new ObjectInputStream(fis);
			groupList = (GroupList)groupStream.readObject();
		}
		catch(FileNotFoundException e)
		{
			//Don't need this, will be created if the userlist was not.
			//Create a new list, add current user to the ADMIN group. They now own the ADMIN group.
			System.out.println("Error in finding GroupList file. Delete the userlist to maintain stability!");
			System.exit(-1);
		}
		catch(IOException e)
		{
			System.out.println("Error reading from GroupList file");
			System.exit(-1);
		}
		catch(ClassNotFoundException e)
		{
			System.out.println("Error reading from GroupList file");
			System.exit(-1);
		}
		
		//Autosave Daemon. Saves lists every 5 minutes
		AutoSave aSave = new AutoSave(this);
		aSave.setDaemon(true);
		aSave.start();
		
		//This block listens for connections and creates threads on new connections
		try
		{
			System.out.println("Good to go! Listening for new connections...");
			final ServerSocket serverSock = new ServerSocket(port);
			
			Socket sock = null;
			GroupThread thread = null;
			
			while(true)
			{
				sock = serverSock.accept();
				thread = new GroupThread(sock, this);
				thread.start();
			}
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}

	}
	
}

//This thread saves the user list
class ShutDownListener extends Thread
{
	public GroupServer my_gs;
	
	public ShutDownListener (GroupServer _gs) {
		my_gs = _gs;
	}
	
	public void run()
	{
		System.out.println("Shutting down server");
		ObjectOutputStream outStream;
		try
		{
			outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
			outStream.writeObject(my_gs.userList);
			outStream.close();
			outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
			outStream.writeObject(my_gs.groupList);
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}
}

class AutoSave extends Thread
{
	public GroupServer my_gs;
	
	public AutoSave (GroupServer _gs) {
		my_gs = _gs;
	}
	
	public void run()
	{
		do
		{
			try
			{
				Thread.sleep(300000); //Save group and user lists every 5 minutes
				System.out.println("Autosave group and user lists...");
				ObjectOutputStream outStream;
				try
				{
					outStream = new ObjectOutputStream(new FileOutputStream("UserList.bin"));
					outStream.writeObject(my_gs.userList);
					outStream.close();
					outStream = new ObjectOutputStream(new FileOutputStream("GroupList.bin"));
					outStream.writeObject(my_gs.groupList);
				}
				catch(Exception e)
				{
					System.err.println("Error: " + e.getMessage());
					e.printStackTrace(System.err);
				}
			}
			catch(Exception e)
			{
				System.out.println("Autosave Interrupted");
			}
	    }while(true);
	}
}

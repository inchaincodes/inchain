package org.inchain.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerTest   
{  
    private ServerSocket ss;  
    private Socket socket;  
    private BufferedReader in;  
    private PrintWriter out;  
  
    public ServerTest()   
    {  
        try   
        {  
            ss = new ServerSocket(10000);  
              
            System.out.println("The server is waiting your input...");  
              
            while(true)   
            {  
                socket = ss.accept();  
                  
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));  
                out = new PrintWriter(socket.getOutputStream(), true);  
                String line = in.readLine();  
                  
                System.out.println("you input is : " + line);  
                  
                //out.println("you input is :" + line);  
                  
                out.close();  
                in.close();  
                socket.close();  
                  
                if(line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit"))  
                    break;  
            }  
              
            ss.close();  
              
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  

}  
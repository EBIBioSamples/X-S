package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biosd.xs.init.TaskInfo;
import uk.ac.ebi.biosd.xs.starter.StarterLog;
import uk.ac.ebi.biosd.xs.starter.TaskStarter;
import uk.ac.ebi.biosd.xs.task.TaskManager;
import uk.ac.ebi.biosd.xs.util.ServletRequestParamPool;

public class ControlServlet extends HttpServlet
{
 static final String CommandParameter = "command";


 static final String CommandForceTask = "forceTask";
 static final String CommandInterruptTask = "interruptTask";

 static final String TaskParameter = "task";

 private static final long serialVersionUID = 1L;

 private final Logger log = LoggerFactory.getLogger(ControlServlet.class);
 
 
 @Override
 protected void doGet(HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
 {
  String command = req.getParameter(CommandParameter);
  
  if( command == null )
  {
   sendMessage("Parameter '"+CommandParameter+"' is missed", resp.getOutputStream(), "red");
   return;
  }
  
  
  StarterLog stLog = new StarterLog()
  {
   
   @Override
   public void sendInfoMsg(String msg)
   {
     try
     {
      sendMessageNoExp(msg, resp.getOutputStream(), "black");
     }
     catch(IOException e)
     {
      log.error("IO error: "+e.getMessage());
     }
   }
   
   @Override
   public void sendErrorMsg(String msg)
   {
     try
     {
      sendMessageNoExp(msg, resp.getOutputStream(), "red");
     }
     catch(IOException e)
     {
      log.error("IO error: "+e.getMessage());
     }
   }
  };
  
  if( CommandForceTask.equalsIgnoreCase(command) )
  {
   String tNm = req.getParameter(TaskParameter);
   
   if( tNm == null )
   {
    sendMessageNoExp("'"+TaskParameter+"' should be defined for this command", resp.getOutputStream(), "red");
    return;
   }
   
   TaskStarter.start(tNm, new ServletRequestParamPool(req), "", stLog);
  }
  else if( CommandInterruptTask.equalsIgnoreCase(command) )
  {
   String tNm = req.getParameter(TaskParameter);
   
   if( tNm == null )
   {
    sendMessageNoExp("'"+TaskParameter+"' should be defined for this command", resp.getOutputStream(), "red");
    return;
   }
   
   TaskInfo ti = TaskManager.getDefaultInstance().getTask(tNm);
   
   if( ti == null )
    sendMessageNoExp("Task '"+tNm+"' doesn't exist", resp.getOutputStream(), "red");
   else if( ti.getTask().interrupt() )
    sendMessageNoExp("Task '"+tNm+"' was interrupted", resp.getOutputStream(), "black");
   else
    sendMessageNoExp("Task '"+tNm+"' is idle", resp.getOutputStream(), "black");

  }
 }

 
 private void sendMessageNoExp(String msg, ServletOutputStream out, String color)
 {
  try
  {
   sendMessage(msg, out, color);
  }
  catch(IOException e)
  {
   log.error("Can't send info message to the client. "+e.getMessage());
  }

 }

 private void sendMessage(String msg, ServletOutputStream out, String color) throws IOException
 {
  out.print("<html><body><span");
  
  if( color != null )
   out.print(" style=\"color: "+color+"\"");
  
  out.print(">\n");
  out.print(msg);
  out.print("\n</span></body></html>");
  
 }
 
}

package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ControlServlet extends HttpServlet
{
 static final String CommandParameter = "command";
 static final String LimitParameter = "limit";
 static final String CommandForceEBEye = "forceEBEye";

 private static final long serialVersionUID = 1L;

 
 @Override
 protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
 {
  String command = req.getParameter(CommandParameter);
  
  if( command == null )
  {
   sendMessage("Parameter '"+CommandParameter+"' is missed", resp.getOutputStream(), "red");
   return;
  }
  
  if( CommandForceEBEye.equalsIgnoreCase(command) )
  {
   EBeyeExport exp = EBeyeExport.getInstance();
   
   if( exp == null )
   {
    sendMessage("EBeyeExport was not initialized. See logs", resp.getOutputStream(), "red");
    return;
   }
   
   int limit = -1;
   
   String limStr = req.getParameter(LimitParameter);
   
   if( limStr != null )
   {
    try
    {
     limit = Integer.parseInt(limStr);
    }
    catch(Exception e)
    {
     sendMessage("Invalid value of '"+LimitParameter+"' parameter", resp.getOutputStream(), "red");
     return;
    }
   }
   
   if( ! exp.export(limit) )
   {
    sendMessage("EBEye export is busy", resp.getOutputStream(), "orange");
    return;
   }
   
   sendMessage("EBEye export finished", resp.getOutputStream(), "black");

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

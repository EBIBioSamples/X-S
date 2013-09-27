package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.xs.service.ebeye.EBeyeExport;

public class ControlServlet extends HttpServlet
{
 static final String CommandParameter = "command";
 static final String LimitParameter = "limit";
 static final String GenGroupParameter = "generateGroups";
 static final String GenSamplesParameter = "generateSamples";
 static final String ExportPrivateParameter = "exportPrivate";
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
   
   String genGrpStr =  req.getParameter(GenGroupParameter);
   
   boolean genGrp = genGrpStr == null? true : "1".equals(genGrpStr) || "yes".equalsIgnoreCase(genGrpStr) || "on".equalsIgnoreCase(genGrpStr) || "true".equalsIgnoreCase(genGrpStr);
   
   String genSmpStr =  req.getParameter(GenSamplesParameter);
   
   boolean genSmp = genSmpStr == null? true : "1".equals(genSmpStr) || "yes".equalsIgnoreCase(genSmpStr) || "on".equalsIgnoreCase(genSmpStr) || "true".equalsIgnoreCase(genSmpStr);

   String expPrvStr =  req.getParameter(ExportPrivateParameter);
   
   boolean expPrv = expPrvStr == null? false : "1".equals(expPrvStr) || "yes".equalsIgnoreCase(expPrvStr) || "on".equalsIgnoreCase(expPrvStr) || "true".equalsIgnoreCase(expPrvStr);

   
   if( ! exp.export(limit, genSmp, genGrp, ! expPrv) )
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

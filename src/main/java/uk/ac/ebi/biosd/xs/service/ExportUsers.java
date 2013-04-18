package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.fg.biosd.model.access_control.User;

public class ExportUsers extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String ProfileParameter = "server";

 
 @Override
 protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  String prof = request.getParameter(ProfileParameter);
  
  EntityManagerFactory emf = null;
  
  if( prof == null )
  {
   emf = EMFManager.getDefaultFactory();
   prof = "<default>";
  }
  else
   emf = EMFManager.getFactory(prof);
  
  if( emf == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>Can't find profile: "+prof+"</span></body></html>");
   return;
  }
  
  response.setContentType("text/xml");

  EntityManager em = emf.createEntityManager();
  
  EntityTransaction ts = em.getTransaction ();
  
  ts.begin ();
  
  @SuppressWarnings("unchecked")
  List<User> result = em.createQuery("select u from "+User.class.getCanonicalName()+" u").getResultList();
 
  PrintWriter out = response.getWriter();
  
  out.println("<Users>");
  
  for( User u : result )
  {
   out.print("<User id=\"");
   AbstractXMLFormatter.xmlEscaped(u.getAcc(), out);
   out.println("\">");
   
   out.print("<Name>");
   AbstractXMLFormatter.xmlEscaped(u.getName(), out);
   out.println("</Name>");

   out.print("<Password>");
   AbstractXMLFormatter.xmlEscaped(u.getHashPassword(), out);
   out.println("</Password>");
   
   if( u.getEmail() != null )
   {
    out.print("<Email>");
    AbstractXMLFormatter.xmlEscaped(u.getEmail(), out);
    out.println("</Email>");
   }
   
   out.print("</User>");


  }
  
  out.println("</Users>");

  ts.commit();
  
  em.close();
 }
}

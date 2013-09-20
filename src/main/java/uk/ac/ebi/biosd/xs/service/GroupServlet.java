package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter;
import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;


public class GroupServlet extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String DefaultSchema = SchemaManager.STXML;

 static final String SchemaParameter = "schema";
 static final String ProfileParameter = "server";
 static final String IdParameter = "id";
 
 public GroupServlet()
 {
  super();
  // TODO Auto-generated constructor stub
 }

 /**
  * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  AbstractXMLFormatter formatter=null;
  


  String group = request.getParameter(IdParameter);
  
  if( group == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>No group ID provided</span></body></html>");
   return;
  }
  
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
  
  
  String sch = request.getParameter(SchemaParameter);
  
  if( sch == null )
   sch = DefaultSchema;

//  @Override
//  public void exportSample(BioSample smp, Appendable out) throws IOException
//  {
//   exportSample(smp, out, true, true, null, null);
//  }
//
//  @Override
//  public void exportGroup(BioSampleGroup ao, Appendable out) throws IOException
//  {
//   exportGroup(ao, out, true, Samples.LIST, false );
//  }
  
  formatter = SchemaManager.getFormatter(sch, true, false, false, SamplesFormat.LIST);
  
  if( formatter == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>Invalid schema: '"+sch+"'</span></body></html>");
   return;
  }
  
  EntityManager em = emf.createEntityManager();
  
  EntityTransaction ts = em.getTransaction ();
  
  ts.begin ();
  
  AccessibleDAO<BioSampleGroup> smpDAO = new AccessibleDAO<>(BioSampleGroup.class, em);
  
  BioSampleGroup smp = smpDAO.find(group);

  if( smp == null )
  {
   response.setStatus(HttpServletResponse.SC_NOT_FOUND);
   response.getWriter().append("<html><body><span color='red'>Group with ID: "+group+" not found</span></body></html>");
   return;
  }
  
  response.setContentType("text/xml; charset=UTF-8");
  
  formatter.exportGroup(smp, response.getWriter());
  
  ts.commit();
  
  em.close();
  
 }

 /**
  * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  // TODO Auto-generated method stub
 }

}

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
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;



public class SampleServlet extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String DefaultSchema = SchemaManager.STXML;

 static final String SchemaParameter = "schema";
 static final String ProfileParameter = "server";
 static final String IdParameter = "id";
 
 public SampleServlet()
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
  


  String sample = request.getParameter(IdParameter);
  
  if( sample == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>No sample ID provided</span></body></html>");
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

  formatter = SchemaManager.getFormatter(sch, true, true, false, SamplesFormat.NONE);
  
  if( formatter == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>Invalid schema: '"+sch+"'</span></body></html>");
   return;
  }
  
  EntityManager em = emf.createEntityManager();
  
  EntityTransaction ts = em.getTransaction ();
  
  ts.begin ();
  
  AccessibleDAO<BioSample> smpDAO = new AccessibleDAO<>(BioSample.class, em);
  
  BioSample smp = smpDAO.find(sample);

  if( smp == null )
  {
   response.setStatus(HttpServletResponse.SC_NOT_FOUND);
   response.getWriter().append("<html><body><span color='red'>Sample with ID: "+sample+" not found</span></body></html>");
   return;
  }
  
  response.setContentType("text/xml");
  
  formatter.exportSample(smp, response.getWriter());
  
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

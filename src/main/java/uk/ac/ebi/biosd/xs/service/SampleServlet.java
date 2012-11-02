package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.export.STM2XMLconverter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.dao.hibernate.toplevel.AccessibleDAO;



@WebServlet("/SampleServlet")
public class SampleServlet extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String ProfileParameter = "profile";
 static final String SampleParameter = "sample";
 
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
  String sample = request.getParameter(SampleParameter);
  
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
  
  STM2XMLconverter.exportSample(smp, response.getWriter());
  
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

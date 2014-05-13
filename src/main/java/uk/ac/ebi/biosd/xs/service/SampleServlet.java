package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.AuxInfo;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.biosd.xs.util.ParamPool;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;



public class SampleServlet extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String DefaultSchema = SchemaManager.STXML;
 static final boolean      DefaultShowNS                = false;

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
 protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  XMLFormatter formatter=null;
  


  String sample = request.getParameter(IdParameter);
  
  if( sample == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>No sample ID provided</span></body></html>");
   return;
  }
  
  
  RequestConfig reqCfg = new RequestConfig();
  
  reqCfg.loadParameters(new ParamPool()
  {
   @Override
   public Enumeration<String> getNames()
   {
    return request.getParameterNames();
   }
   
   @Override
   public String getParameter(String name)
   {
    return request.getParameter(name);
   }
  }, "");
  
  
  SamplesFormat samplesFormat = SamplesFormat.EMBED;
  
  String pv = reqCfg.getOutput(null);
  
  if( pv != null )
  {
   try
   {
    samplesFormat = SamplesFormat.valueOf(pv);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().append("<html><body><span color='red'>Invalid "+RequestConfig.SamplesParameter+" parameter value. Sould be one of: "+Arrays.asList(SamplesFormat.values())+"</span></body></html>");
    return;
   }
  }
  
  String prof = reqCfg.getServer(null);
  
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
  
  
  EntityManagerFactory myEqEmf = null;
  
  String str = reqCfg.getMyEq(null);
  
  if( str != null )
   myEqEmf = EMFManager.getMyEqFactory( str );
  
  
  response.setContentType("text/xml; charset=UTF-8");
  Appendable out = response.getWriter();
  
  
  String sch = reqCfg.getSchema(DefaultSchema);
  
  formatter = SchemaManager.getFormatter(sch, true, false, samplesFormat, false, new Date());
  
  if( formatter == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>Invalid schema: '"+sch+"'</span></body></html>");
   return;
  }
  
  
  boolean showNS =reqCfg.getShowNamespace( DefaultShowNS );
  
  EntityManager em = emf.createEntityManager();
  
//  Query listQuery = em.createQuery("SELECT a FROM " + BioSample.class.getCanonicalName () + " a WHERE a.acc = ?1");
  
  EntityTransaction ts = em.getTransaction ();
  
  ts.begin ();
  
  AuxInfo auxInf = null;
  
  try
  {
   
   if( myEqEmf != null )
    auxInf = new AuxInfoImpl(myEqEmf);
   
   AccessibleDAO<BioSample> smpDAO = new AccessibleDAO<>(BioSample.class, em);
   
   BioSample smp = smpDAO.find(sample);
   
   if( smp == null )
   {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.getWriter().append("<html><body><span color='red'>Sample with ID: "+sample+" not found</span></body></html>");
    return;
   }
   
   
   formatter.exportSample(smp, auxInf, out, showNS);
  }
  finally
  {
   ts.commit();
   
   em.close();
   
   if(auxInf != null)
    auxInf.destroy();
  }
  
  
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

package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.biosd.xs.service.RequestConfig.ParamPool;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;


public class GroupServlet extends HttpServlet
{
 private static final long serialVersionUID = 1L;

 static final String        DefaultSchema        = SchemaManager.STXML;
 static final boolean       DefaultShowNS        = false;
 static final SamplesFormat DefaultSamplesFormat = SamplesFormat.EMBED;

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
 protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  
  XMLFormatter formatter=null;
  


  String id = request.getParameter(IdParameter);
  
  if( id == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>No sample ID provided</span></body></html>");
   return;
  }
  
  
  RequestConfig reqCfg = new RequestConfig();
  
  reqCfg.loadParameters(new ParamPool()
  {
   
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
  
 
  response.setContentType("text/xml; charset=UTF-8");
  Appendable out = response.getWriter();
  
  
  String sch = reqCfg.getSchema(DefaultSchema);
  
  formatter = SchemaManager.getFormatter(sch, false, false, samplesFormat,false);
  
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
  
  try
  {
   
   AccessibleDAO<BioSampleGroup> smpDAO = new AccessibleDAO<>(BioSampleGroup.class, em);
   
   BioSampleGroup grp = smpDAO.find(id);
   
   if( grp == null )
   {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    response.getWriter().append("<html><body><span color='red'>Group with ID: "+id+" not found</span></body></html>");
    return;
   }
   
   
   formatter.exportGroup(grp, out, showNS);
  }
  finally
  {
   ts.commit();
   
   em.close();
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

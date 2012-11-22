package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.export.STM2XMLconverter;
import uk.ac.ebi.biosd.export.STM2XMLconverter.Samples;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class ExportAll extends HttpServlet
{
 static final String ProfileParameter = "server";
 static final String LimitParameter = "limit";
 static final String SamplesParameter = "samples";
 static final String SinceParameter = "since";
 static final String AttributesParameter = "showAttributes";

 private static final long serialVersionUID = 1L;
 
 private static final int blockSize = 1000;

 /**
  * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  long limit=-1;
  
  String limP =  request.getParameter(LimitParameter);
  
  if( limP != null )
  {
   try
   {
    limit = Long.parseLong(limP);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().append("<html><body><span color='red'>Invalid "+LimitParameter+" parameter value. Sould be an integer value</span></body></html>");
    return;
   }
  }
  
  if( limit <=0 )
   limit=Long.MAX_VALUE;
  
  long since=-1;
  
  String sinsP =  request.getParameter(SinceParameter);
  
  if( sinsP != null )
  {
   try
   {
    since = Long.parseLong(sinsP);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().append("<html><body><span color='red'>Invalid "+SinceParameter+" parameter value. Sould be an integer value</span></body></html>");
    return;
   }
  }
  
  
  Samples samples = Samples.EMBED;
  
  String smp = request.getParameter(SamplesParameter);
  
  if( smp != null )
  {
   try
   {
    samples = Samples.valueOf(smp);
   }
   catch(Exception e)
   {
    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    response.getWriter().append("<html><body><span color='red'>Invalid "+SamplesParameter+" parameter value. Sould be one of: "+Arrays.asList(Samples.values())+"</span></body></html>");
    return;
   }
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
  
  long startID = Long.MIN_VALUE;
  long count = 0;
  
  response.setContentType("text/xml");
  Appendable out = response.getWriter();
  
  out.append("<BioSamples xmlns=\""+STM2XMLconverter.nameSpace+"\" timestamp=\""+(new java.util.Date().getTime())+"\">\n");
  
  Query listQuery = null;
  
  if( since < 0 )
   listQuery = em.createQuery("SELECT a FROM " + BioSampleGroup.class.getCanonicalName () + " a WHERE a.id >=?1 ORDER BY a.id");
  else
  {
   listQuery = em.createQuery("SELECT grp FROM " + BioSampleGroup.class.getCanonicalName () + " grp JOIN grp.MSIs msi WHERE grp.id >=?1 and msi.updateDate > ?2  ORDER BY grp.id");
  
   listQuery.setParameter(2, new Date(since));
  }
  
  listQuery.setMaxResults ( blockSize );  
  
  try
  {

   blockLoop: while(true)
   {

    listQuery.setParameter ( 1, startID );
    
    @SuppressWarnings("unchecked")
    List<BioSampleGroup> result = listQuery.getResultList();
    
    int i=0;
    
    for(BioSampleGroup g : result)
    {
     count++;
     i++;
     
//     g.getId();
//     System.out.println(g.getAcc() + " : " + g.getSamples().size());

     STM2XMLconverter.exportGroup( g, out, false, samples, "true".equals( request.getParameter(AttributesParameter) ) );

     startID=g.getId()+1;
     
     if( count >= limit )
      break blockLoop;
    }
    
    if( i < blockSize )
     break;
   }
  }
  finally
  {
   ts.commit();
   
   em.close();
  }
  
  out.append("</BioSamples>\n");

  
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

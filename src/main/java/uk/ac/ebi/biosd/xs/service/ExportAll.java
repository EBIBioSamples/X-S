package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.export.STM2XMLconverter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

/**
 * Servlet implementation class ExportAll
 */
@WebServlet("/ExportAll")
public class ExportAll extends HttpServlet
{
 static final String ProfileParameter = "server";
 static final String LimitParameter = "limit";

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
   }
  }
  
  if( limit <=0 )
   limit=Long.MAX_VALUE;
  
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
  
  try
  {
   String queryStr = "SELECT a FROM " + BioSampleGroup.class.getCanonicalName() + " a ";//WHERE a.Id>=:id ORDER BY a.Id";
   Query query = em.createQuery(queryStr);

   blockLoop: while(true)
   {

    query.setParameter("id", startID);

    query.setMaxResults(blockSize);

    @SuppressWarnings("unchecked")
    List<BioSampleGroup> result = query.getResultList();

    int i=0;
    
    for(BioSampleGroup g : result)
    {
     count++;
     i++;
     
     g.getId();
     System.out.println(g.getAcc() + " : " + g.getSamples().size());

     STM2XMLconverter.exportGroup(g, System.out);

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

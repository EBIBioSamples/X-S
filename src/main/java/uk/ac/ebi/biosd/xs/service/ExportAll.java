package uk.ac.ebi.biosd.xs.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import uk.ac.ebi.biosd.xs.export.AbstractXMLFormatter.SamplesFormat;
import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.init.EMFManager;
import uk.ac.ebi.biosd.xs.service.RequestConfig.ParamPool;

public class ExportAll extends HttpServlet
{
 static final String       DefaultSchema                = SchemaManager.STXML;
 static final boolean      DefaultShowNS                = false;
 static final boolean      DefaultShowAttributesSummary = true;
 static final boolean      DefaultShowAccessControl     = false;
 static final boolean      DefaultShowSources           = true;
 static final boolean      DefaultSourcesByName         = false;
 static final boolean      DefaultPublicOnly            = false;

 private static final long serialVersionUID = 1L;
 
 private static final int blockSize = 1000;

 /**
  * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
  *      response)
  */
 @Override
 protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
 {
  XMLFormatter formatter=null;
  
  RequestConfig reqCfg = new RequestConfig();
  
  reqCfg.loadParameters(new ParamPool()
  {
   
   @Override
   public String getParameter(String name)
   {
    return request.getParameter(name);
   }
  }, "");
  
  long limit=reqCfg.getLimit(Long.MAX_VALUE);
  
  if( limit <=0 )
   limit=Long.MAX_VALUE;
  
  int threadsNum = reqCfg.getThreads(1);
  
  if( threadsNum <=0 )
   threadsNum=Runtime.getRuntime().availableProcessors();
  
  long since=reqCfg.getSince(-1);
  
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
  
  formatter = SchemaManager.getFormatter(sch,
    reqCfg.getShowAttributesSummary(DefaultShowAttributesSummary),
    reqCfg.getShowAccessControl(DefaultShowAccessControl),
    samplesFormat,
    reqCfg.getPublicOnly(DefaultPublicOnly)
);
  
  if( formatter == null )
  {
   response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
   response.getWriter().append("<html><body><span color='red'>Invalid schema: '"+sch+"'</span></body></html>");
   return;
  }
  
  boolean exportSources = reqCfg.getShowSources(DefaultShowSources);
  
  boolean sourcesByName = reqCfg.getSourcesByName(DefaultSourcesByName);

  
  Exporter expt = null;
  
  if( threadsNum == 1 )
   expt = new ExporterST(emf, formatter, exportSources, sourcesByName, blockSize, reqCfg.getShowNamespace(DefaultShowNS) );
  else
   expt = new ExporterMT(emf, formatter, exportSources, sourcesByName, reqCfg.getShowNamespace(DefaultShowNS), threadsNum);
 
  System.out.println("Start exporting. Request from: "+request.getRemoteAddr()+" Limit: "+limit+" Time: "+new Date()+" Thread: "+Thread.currentThread().getName());
  
  try
  {
   expt.export(since, out, limit);
  }
  catch( IOException e )
  {
   e.printStackTrace();

   System.out.println("Breaking exporting. Request from: "+request.getRemoteAddr()+" Time: "+new Date()+" Thread: "+Thread.currentThread().getName()+" Error: "+e.getMessage());
  }
  finally
  {
   formatter.shutdown();
  }
  
  System.out.println("Finished exporting. Request from: "+request.getRemoteAddr()+" Time: "+new Date()+" Thread: "+Thread.currentThread().getName());

 
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

package uk.ac.ebi.biosd.xs.init;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class Init implements ServletContextListener
{
 static String PersistParamPrefix = "persist";
 static String DefaultProfileParam = PersistParamPrefix+".defaultProfile";

 @Override
 public void contextInitialized(ServletContextEvent ctx)
 {
  Map<String, Map<String,Object>> profMap = new HashMap<>();
  Map<String,Object> defaultProfile=null;
  String defProfName = null;
  
  Matcher mtch = Pattern.compile("^"+PersistParamPrefix+"([\\s*(\\S+)\\s*])?\\.(\\S+)$").matcher("");
  
  ServletContext servletContext = ctx.getServletContext();
  
  Enumeration<?> pNames = servletContext.getInitParameterNames();
  
  while( pNames.hasMoreElements() )
  {
   String key = pNames.nextElement().toString();
   String val = servletContext.getInitParameter(key); 
  
   if( key.equals(DefaultProfileParam) )
   {
    defProfName = val;
    continue;
   }
   
   mtch.reset( key );
   
   if( ! mtch.matches() )
    continue;

   String profile = null;
   String param = null;

   if( mtch.groupCount() == 3 )
   {
    profile = mtch.group(2);
    param = mtch.group(3);
   }
   else
    param = mtch.group(mtch.groupCount());
   

   
   Map<String,Object> cm = profMap.get(profile);
   
   if( cm == null )
    profMap.put(profile, cm = new TreeMap<>() );
   
   cm.put(param, val);
  }
  
  defaultProfile = profMap.get(null);
  
  if( defProfName != null )
  {
   Map<String,Object> namedDP = profMap.get(defProfName);
   
   if( defaultProfile != null )
   {
    if( namedDP != null )
     defaultProfile.putAll(namedDP);
   }
   else
    defaultProfile = namedDP;
  }
  
  for( Map.Entry<String, Map<String,Object>> me : profMap.entrySet() )
  {
   if( me.getKey() == null )
    continue;
   
   EMFManager.addFactory( me.getKey(), Persistence.createEntityManagerFactory ( "defaultPersistenceUnit", me.getValue() )  );
  }
  
  if( defaultProfile != null )
   EMFManager.setDefaultFactory( Persistence.createEntityManagerFactory ( "defaultPersistenceUnit", defaultProfile ) );
 }

 @Override
 public void contextDestroyed(ServletContextEvent arg0)
 {
  // TODO Auto-generated method stub
  
 }



}

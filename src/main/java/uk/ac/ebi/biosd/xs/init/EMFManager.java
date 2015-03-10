package uk.ac.ebi.biosd.xs.init;

import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManagerFactory;

public class EMFManager
{
 private static Map<String, EntityManagerFactory> mngrMap = new TreeMap<>();
 private static Map<String, EntityManagerFactory> myeqMngrMap = new TreeMap<>();
 
 private static EntityManagerFactory defaultFactory;
 
 public static EntityManagerFactory getDefaultFactory()
 {
  return defaultFactory;
 }

 public static EntityManagerFactory getFactory( String mngId )
 {
  return mngrMap.get(mngId);
 }
 
 public static void setDefaultFactory( EntityManagerFactory cm )
 {
  defaultFactory = cm;
 }

 public static void addFactory( String cmId, EntityManagerFactory cm )
 {
  mngrMap.put(cmId, cm);
 }

 public static void addMyEqFactory( String cmId, EntityManagerFactory cm )
 {
  myeqMngrMap.put(cmId, cm);
 }

 public static EntityManagerFactory getMyEqFactory( String mngId )
 {
  return myeqMngrMap.get(mngId);
 }

 public static void destroy()
 {
  for( EntityManagerFactory emf : mngrMap.values() )
   if( emf.isOpen() )
    emf.close();
  
  for( EntityManagerFactory emf : myeqMngrMap.values() )
   if( emf.isOpen() )
    emf.close();
 }

}

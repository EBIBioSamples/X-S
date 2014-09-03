package uk.ac.ebi.biosd.xs.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import uk.ac.ebi.biosd.xs.export.AuxInfo;
import uk.ac.ebi.biosd.xs.export.EquivalenceRecord;
import uk.ac.ebi.fg.myequivalents.dao.EntityMappingDAO;
import uk.ac.ebi.fg.myequivalents.model.EntityMapping;

public class AuxInfoImpl implements AuxInfo
{
 private static final int MAX_REQUESTS = 50;
 
 private final EntityManagerFactory myeqFactory;
 private EntityManager myeqManager;
 
 private EntityMappingDAO entityMappingDAO;
 
 private int counter=0;

 public AuxInfoImpl( EntityManagerFactory myeqFact )
 {
  myeqFactory = myeqFact;
  myeqManager = myeqFactory.createEntityManager();
  
  entityMappingDAO = new EntityMappingDAO ( myeqManager );
 }
 
 @Override
 public Collection<EquivalenceRecord> getSampleEquivalences( String acc )
 {
  return getEquivalences("ebi.biosamples.samples", acc);
 }
 
 @Override
 public Collection<EquivalenceRecord> getGroupEquivalences( String acc )
 {
  return getEquivalences("ebi.biosamples.groups", acc);
 }

 @Override
 public void clear()
 {
  if( myeqManager == null )
   return;
  
  destroy();
  
  myeqManager = myeqFactory.createEntityManager();
  myeqManager.getTransaction().begin();
  
  entityMappingDAO = new EntityMappingDAO ( myeqManager );
  
  counter=0;

 }
 
 private Collection<EquivalenceRecord> getEquivalences( String srvId, String acc )
 {

//  EntityMappingSearchResult result = new EntityMappingSearchResult(true);
//  User user = userDao.getLoggedInUser();
//  boolean mustBePublic = user == null ? true : !user.hasPowerOf(EDITOR);

  List<EquivalenceRecord> res = null;
  
  for( EntityMapping mp : entityMappingDAO.findEntityMappings(srvId,acc, true) )
  {
   if( res == null )
    res = new ArrayList<>();

   res.add( new EquivalenceRecord(mp.getAccession(),mp.getService().getTitle(),mp.getService().getUriPattern()) );
  }

  if( ++counter > MAX_REQUESTS )
  {
   myeqManager.clear();
   counter=0;
  }
  
  return res;
 }
 
 @Override
 public void destroy()
 {
  if( myeqManager != null )
  {
   EntityTransaction trn = myeqManager.getTransaction();

   if( trn.isActive() && ! trn.getRollbackOnly() )
   {
    try
    {
     trn.commit();
    }
    catch(Exception e)
    {
     e.printStackTrace();
    }
   }
   
   myeqManager.clear();
   myeqManager.close();
   myeqManager = null;
  }
 }

}

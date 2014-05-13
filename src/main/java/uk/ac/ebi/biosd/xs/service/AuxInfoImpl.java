package uk.ac.ebi.biosd.xs.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import uk.ac.ebi.biosd.xs.export.AuxInfo;
import uk.ac.ebi.biosd.xs.export.EquivalenceRecord;
import uk.ac.ebi.fg.myequivalents.dao.EntityMappingDAO;
import uk.ac.ebi.fg.myequivalents.model.EntityMapping;

public class AuxInfoImpl implements AuxInfo
{
 private final EntityManagerFactory myeqFactory;
 private EntityManager myeqManager;
 
 private final EntityMappingDAO entityMappingDAO;

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

  myeqManager.clear();
  
  return res;
 }
 
 @Override
 public void destroy()
 {
  if( myeqManager != null )
  {
   myeqManager.close();
   myeqManager = null;
  }
 }

}

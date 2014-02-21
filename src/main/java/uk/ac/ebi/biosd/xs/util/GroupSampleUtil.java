package uk.ac.ebi.biosd.xs.util;

import uk.ac.ebi.fg.biosd.model.access_control.User;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;

public class GroupSampleUtil
{

 public static BioSampleGroup cloneGroup( BioSampleGroup g, String acc, Double smpMul )
 {
  BioSampleGroup ng = new BioSampleGroup(acc);
  
  ng.setAnnotations( g.getAnnotations() );
  ng.setDatabaseRecordRefs( g.getDatabaseRecordRefs() );
  ng.setInReferenceLayer( g.isInReferenceLayer() );
  ng.setPropertyValues( g.getPropertyValues() );
  ng.setPublicFlag( g.getPublicFlag() );
  ng.setReleaseDate( g.getReleaseDate() );
  ng.setUpdateDate( g.getUpdateDate() );
  
  if( g.getMSIs() != null )
  {
   for(MSI msi : g.getMSIs())
    ng.addMSI(msi);
  }
  
  if( g.getUsers() != null )
  {
   for( User u: g.getUsers() )
    ng.addUser(u);
  }
  
  int smpMulFloor = 1;
  double smpMulFrac = 0;

  if( smpMul != null )
  {
   smpMulFloor = (int) Math.floor(smpMul);
   smpMulFrac = smpMul - smpMulFloor;
  }

  int pos = acc.lastIndexOf("00");
 
  for( BioSample s : g.getSamples() )
  {
   int nSmpRep = smpMulFloor;

   if( smpMul != null && smpMulFrac > 0.005 )
    nSmpRep += Math.random() < smpMulFrac ? 1 : 0;
   
   for(int i=1; i <= nSmpRep; i++ )
   {
    
    String sacc = pos == -1?(s.getAcc()+"00"+i):(s.getAcc()+acc.substring(pos)+"00"+i);
    
    BioSample ns = cloneSample(s, sacc);
    
    ng.addSample(ns);
   }
  }
  
  return ng;
  
 }
 
 public static BioSample cloneSample(BioSample s, String acc)
 {
  BioSample ns = new BioSample(acc);

  ns.setAnnotations(s.getAnnotations());
  ns.setDatabaseRecordRefs(s.getDatabaseRecordRefs());
  ns.setInReferenceLayer(s.isInReferenceLayer());
  ns.setPropertyValues(s.getPropertyValues());
  ns.setPublicFlag(s.getPublicFlag());
  ns.setReleaseDate(s.getReleaseDate());
  ns.setUpdateDate(s.getUpdateDate());
  
  return ns;
 }

}

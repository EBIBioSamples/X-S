package uk.ac.ebi.biosd.xs.util;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class GroupSampleUtil
{

 public static BioSampleGroup cloneGroup( BioSampleGroup g, String acc, Double smpMul )
 {
  BioSampleGroup ng = new BioSampleGroup(acc);
  
  ng.setAnnotations( g.getAnnotations() );
  ng.setDatabases( g.getDatabases() );
  ng.setInReferenceLayer( g.isInReferenceLayer() );
  ng.setPropertyValues( g.getPropertyValues() );
  ng.setPublicFlag( g.getPublicFlag() );
  ng.setReleaseDate( g.getReleaseDate() );
  ng.setUpdateDate( g.getUpdateDate() );
  
  
  int smpMulFloor = 1;
  double smpMulFrac = 0;

  if( smpMul != null )
  {
   smpMulFloor = (int) Math.floor(smpMul);
   smpMulFrac = smpMul - smpMulFloor;
  }

  int pos = acc.lastIndexOf('-');
 
  for( BioSample s : g.getSamples() )
  {
   int nSmpRep = smpMulFloor;

   if( smpMul != null && smpMulFrac > 0.005 )
    nSmpRep += Math.random() < smpMulFrac ? 1 : 0;
   
   for(int i=1; i <= nSmpRep; i++ )
   {
    
    String sacc = pos == -1?(s.getAcc()+"-R"+i):(s.getAcc()+acc.substring(pos)+"R"+i);
    
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
  ns.setDatabases(s.getDatabases());
  ns.setInReferenceLayer(s.isInReferenceLayer());
  ns.setPropertyValues(s.getPropertyValues());
  ns.setPublicFlag(s.getPublicFlag());
  ns.setReleaseDate(s.getReleaseDate());
  ns.setUpdateDate(s.getUpdateDate());
  
  return ns;
 }

}

package uk.ac.ebi.biosd.xs.export;

public class EquivalenceRecord
{
 public static final String placeHolder = "${accession}";
 
 private String accession;
 private String title;
 private String url;

 public EquivalenceRecord(String accession, String title, String uriPattern )
 {
  this.accession = accession;
  this.title = title;
  
  if( uriPattern != null )
  {
   int pos = uriPattern.indexOf(placeHolder);
   
   if( pos < 0 )
    url = uriPattern;
   
   if( pos + placeHolder.length() == uriPattern.length() )
    url = uriPattern.substring(0,pos)+accession;
   else
    url = uriPattern.substring(0,pos)+accession+uriPattern.substring(pos+uriPattern.length());
  }

  
  
 }

 public String getAccession()
 {
  return accession;
 }

 public void setAccession(String accession)
 {
  this.accession = accession;
 }

 public String getTitle()
 {
  return title;
 }

 public void setTitle(String title)
 {
  this.title = title;
 }

 public String getUrl()
 {
  return url;
 }

 public void setUrl(String url)
 {
  this.url = url;
 }

}

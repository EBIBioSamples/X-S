package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.TimeZone;

import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public abstract class AbstractXMLFormatter
{
 public enum SamplesFormat
 {
  NONE,
  LIST,
  EMBED
 }
 
 private final boolean showNS;
 private final boolean showAttributes;
 private final boolean showAC;
 private final SamplesFormat smpfmt;
 
 private volatile int groupCount=0;
 private volatile int sampleCount=0;
 private volatile int uniqSampleCount=0;
 
 public AbstractXMLFormatter(boolean showNS, boolean showAttributes, boolean showAC, SamplesFormat smpfmt)
 {
  super();
  this.showNS = showNS;
  this.showAttributes = showAttributes;
  this.showAC = showAC;
  this.smpfmt = smpfmt;
 }

 protected static DateToXsdDatetimeFormatter dateTimeFmt = new DateToXsdDatetimeFormatter( TimeZone.getTimeZone("GMT") );

 public abstract void shutdown();
 
 public abstract boolean exportSample(BioSample smp,  Appendable out) throws IOException;

 public abstract boolean exportGroup( BioSampleGroup ao, Appendable out ) throws IOException;

// public abstract void exportGroup( BioSampleGroup ao, Appendable out, boolean showNS, Samples smpSts, boolean showAttributes ) throws IOException;
 
 public abstract void exportHeader(long since, Appendable out) throws IOException;
 public abstract void exportFooter(Appendable out) throws IOException;

 public abstract void exportSources(Map<String, Counter> srcMap, Appendable out) throws IOException;

 public void resetCounters()
 {
  groupCount=0;
  sampleCount=0;
  uniqSampleCount=0;
 }
 
 protected void incGroupCounter()
 {
  groupCount++;
 }
 
 protected void incSampleCounter()
 {
  sampleCount++;
 }

 protected void incUniqSampleCounter()
 {
  uniqSampleCount++;
 }

 public int getGroupCount()
 {
  return groupCount;
 }

 public int getSampleCount()
 {
  return sampleCount;
 }

 public int getUniqSampleCount()
 {
  return uniqSampleCount;
 }
 
 public static class ReplacePair implements Comparable<ReplacePair>
 {
  char subject;
  String replacement;
  
  public ReplacePair()
  {}
  
  public ReplacePair(char first, String second)
  {
   this.subject = first;
   this.replacement = second;
  }
  
  public char getSubject()
  {
   return subject;
  }
  
  public void setSubject(char first)
  {
   this.subject = first;
  }
  
  public String getReplacement()
  {
   return replacement;
  }

  public void setReplacement(String second)
  {
   this.replacement = second;
  }

  @Override
  public int compareTo(ReplacePair toCmp)
  {
   return subject-toCmp.getSubject();
  }
  
  @Override
  public boolean equals( Object o )
  {
   return subject==((ReplacePair)o).getSubject();
  }

  @Override
  public int hashCode()
  {
   return subject;
  }
  
 }

 static final ReplacePair[] htmlPairs = { 
  new ReplacePair('"',"&quot;"),
  new ReplacePair('\'',"&#39;"),
  new ReplacePair('<',"&lt;"),
  new ReplacePair('>',"&gt;"),
  new ReplacePair('&',"&amp;"),
  };
 
 
 public static void xmlEscaped( String s, Appendable out ) throws IOException
 {
  if( s == null )
   return;
  
  int len = s.length();
  
  boolean escaping = false;
  
  for( int i=0; i < len; i++ )
  {
   char ch = s.charAt(i);
   
   if( ch < 0x20 && ch != 0x0D && ch != 0x0A && ch != 0x09 )
   {
    
    if( ! escaping )
    {
     out.append( s.substring(0, i) );
     escaping=true;
    }
    
    int rem = ch%16;
    
    out.append("&#").append( (ch > 15)?'1':'0' ).append( (char)(rem > 9?(rem-10+'A'):(rem+'0')) ).append(';');
   }
   else
   {
    boolean replaced = false;
    
    for( ReplacePair p : htmlPairs )
    {
     if( ch == p.getSubject() )
     {
      if( ! escaping )
      {
       out.append( s.substring(0, i) );
       escaping=true;
      }
      
      out.append( p.getReplacement() );
      replaced = true;
      break;
     }
    }
    
    if( ! replaced )
    {
     if( escaping )
      out.append(ch);
    }
   }
  }
  
  if( ! escaping )
   out.append(s);
 }
 
 static class DateToXsdDatetimeFormatter
 {

  private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  public DateToXsdDatetimeFormatter()
  {
  }

  public DateToXsdDatetimeFormatter(TimeZone timeZone)
  {
   simpleDateFormat.setTimeZone(timeZone);
  }

  /**
   * Parse a xml date string in the format produced by this class only. This
   * method cannot parse all valid xml date string formats - so don't try to use
   * it as part of a general xml parser
   */
  public synchronized Date parse(String xmlDateTime) throws ParseException
  {
   if(xmlDateTime.length() != 25)
   {
    throw new ParseException("Date not in expected xml datetime format", 0);
   }

   StringBuilder sb = new StringBuilder(xmlDateTime);
   sb.deleteCharAt(22);
   return simpleDateFormat.parse(sb.toString());
  }

  public synchronized String format(Date xmlDateTime) throws IllegalFormatException
  {
   String s = simpleDateFormat.format(xmlDateTime);
   
   StringBuilder sb = new StringBuilder(s);
   sb.insert(22, ':');
   return sb.toString();
  }

  public synchronized void setTimeZone(String timezone)
  {
   simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timezone));
  }
}

 public boolean isShowNS()
 {
  return showNS;
 }


 public boolean isShowAttributes()
 {
  return showAttributes;
 }



 public boolean isShowAC()
 {
  return showAC;
 }

 public SamplesFormat getSamplesFormat()
 {
  return smpfmt;
 }

}

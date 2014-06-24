package uk.ac.ebi.biosd.xs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class FileUtils
{

 public static  void appendFile(Appendable out, File f) throws IOException
 {
  Reader rd = new InputStreamReader(new FileInputStream(f), Charset.forName("utf-8"));
  
  try
  {
   
   CharBuffer buf = CharBuffer.allocate(4096);
   
   while(rd.read(buf) != -1)
   {
    String str = new String(buf.array(), 0, buf.position());
    
    out.append(str);
    
    buf.clear();
   }
   
  }
  finally
  {
   rd.close();
  }
 }
}

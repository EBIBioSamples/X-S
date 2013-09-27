package uk.ac.ebi.biosd.xs.export;

import java.io.IOException;
import java.util.Map;

import uk.ac.ebi.biosd.xs.util.Counter;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public interface XMLFormatter
{
 void shutdown();
 
 boolean isSamplesExport();
 
 boolean exportSample(BioSample smp,  Appendable out, boolean showNS ) throws IOException;

 boolean exportGroup( BioSampleGroup ao, Appendable out, boolean showNS ) throws IOException;

 void exportHeader(long since, Appendable out, boolean showNS) throws IOException;
 void exportGroupHeader(Appendable out, boolean showNS) throws IOException;
 void exportSampleHeader(Appendable out, boolean showNS) throws IOException;

  
 void exportFooter(Appendable out) throws IOException;
 void exportGroupFooter(Appendable out) throws IOException;
 void exportSampleFooter(Appendable out) throws IOException;

 void exportSources(Map<String, Counter> srcMap, Appendable out) throws IOException;
 
 public int getGroupCount();

 public int getSampleCount();

 public int getUniqSampleCount();
}

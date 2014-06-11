package uk.ac.ebi.biosd.xs.output;

import java.io.IOException;

import uk.ac.ebi.biosd.xs.export.XMLFormatter;
import uk.ac.ebi.biosd.xs.mtexport.ExporterStat;

public interface OutputModule
{

 XMLFormatter getFormatter();

 Appendable getGroupOut();

 Appendable getSampleOut();
 
 boolean isGroupedSamplesOnly();
 boolean isSourcesByAcc();
 boolean isSourcesByName();
 
 void start() throws IOException;
 void finish(ExporterStat stat) throws IOException;

 void cancel() throws IOException;
 
}

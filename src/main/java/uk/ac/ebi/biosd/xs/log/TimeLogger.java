package uk.ac.ebi.biosd.xs.log;

public interface TimeLogger
{
 boolean init();
 boolean entry(String msg, String ... tags);
 boolean checkpoint(String msg, String ... tags);
 boolean exit(String msg, String ... tags);
 boolean summary();
}

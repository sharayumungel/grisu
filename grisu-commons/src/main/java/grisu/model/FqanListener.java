package grisu.model;

/**
 * An interface for classes that want to monitor fqan events.
 * 
 * @author markus
 */
public interface FqanListener {

	void fqansChanged(FqanEvent event);

}
